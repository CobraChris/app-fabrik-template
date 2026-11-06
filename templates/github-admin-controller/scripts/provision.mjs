/**
 * Private GitHub Actions controller: provision a durable signing identity per Android app.
 *
 * Modes:
 *   prepare: generate once and create AES-256-GCM encrypted backup
 *   apply:   install ONLY missing GitHub Actions Secrets using a short-lived GitHub App token
 *
 * Call only from the reviewed private controller workflow, never from a public PWA.
 */
import { createPrivateKey, randomBytes, sign } from 'node:crypto';
import { spawnSync, spawn } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync, mkdirSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { backupFileName, missingSecrets, protectBackup, recoverBackup,
         targetRepository, SECRET_NAMES } from './provision-core.mjs';

const BASE=path.resolve(fileURLToPath(new URL('../',import.meta.url)));
const API='https://api.github.com';
const headers=token=>({
  Accept:'application/vnd.github+json',
  Authorization:'Bearer '+token,
  'X-GitHub-Api-Version':'2022-11-28',
});

function requireEnv(name) {
  const val=process.env[name];
  if(typeof val!=='string'||!val)throw new Error('Controller configuration missing: '+name);
  return val;
}

function githubAppJwt(id,pem) {
  if(!/^\d{1,15}$/.test(id))throw new Error('Invalid App ID');
  const enc=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
  const now=Math.floor(Date.now()/1000);
  const msg=enc({typ:'JWT',alg:'RS256'})+'.'+enc({iat:now-60,exp:now+540,iss:id});
  return msg+'.'+sign('RSA-SHA256',Buffer.from(msg),createPrivateKey(pem)).toString('base64url');
}

async function tokenFor(repository) {
  const installationId=requireEnv('GH_APP_INSTALLATION_ID');
  if(!/^\d{1,15}$/.test(installationId))throw new Error('Invalid installation ID');
  const jwt=githubAppJwt(requireEnv('GH_APP_ID'),
    requireEnv('GH_APP_PRIVATE_KEY_PEM').replaceAll('\\n','\n'));
  const name=repository.split('/')[1];
  const r=await fetch(API+'/app/installations/'+installationId+'/access_tokens',{
    method:'POST',
    headers:{...headers(jwt),'Content-Type':'application/json'},
    body:JSON.stringify({repositories:[name],permissions:{secrets:'write'}})
  });
  if(!r.ok)throw new Error('GitHub App authorization failed: HTTP '+r.status);
  const data=await r.json();
  if(typeof data.token!=='string'||!data.token)throw new Error('Missing installation token');
  return data.token;
}

async function existingSecretNames(repository,token){
  const r=await fetch(API+'/repos/'+repository+'/actions/secrets?per_page=100',{
    headers:headers(token)
  });
  if(!r.ok)throw new Error('Cannot inspect target secret metadata: HTTP '+r.status);
  const data=await r.json();
  if(!Array.isArray(data.secrets))throw new Error('Unexpected GitHub Secrets response');
  return data.secrets.map(v=>v.name);
}

function createKeyMaterial(repository){
  const password=randomBytes(36).toString('base64url');
  const alias='appsign';
  const folder=mkdtempSync(path.join(tmpdir(),'app-fabrik-keystore-'));
  const filename=path.join(folder,'signing.p12');
  try {
    const args=['-genkeypair','-noprompt','-v',
      '-alias',alias,'-keyalg','RSA','-keysize','3072',
      '-validity','9125','-storetype','PKCS12',
      '-keystore',filename,
      '-storepass',password,'-keypass',password,
      '-dname','CN=App Factory Android Release, OU=Apps, O=Private, C=DE'];
    const result=spawnSync('keytool',args,{encoding:'utf8',timeout:60000});
    if(result.status!==0)throw new Error('Keystore generation failed');
    const store=readFileSync(filename).toString('base64');
    if(store.length<1000)throw new Error('Keystore too small');
    return {
      APP_SIGNING_KEYSTORE_BASE64:store,
      APP_SIGNING_STORE_PASSWORD:password,
      APP_SIGNING_KEY_ALIAS:alias,
      APP_SIGNING_KEY_PASSWORD:password,
    };
  } finally {
    rmSync(folder,{recursive:true,force:true});
  }
}

async function prepare(repository,key){
  const dest=path.join(BASE,backupFileName(repository));
  const token=await tokenFor(repository);
  const existing=await existingSecretNames(repository,token);
  if(existsSync(dest)){
    recoverBackup(repository,readFileSync(dest,'utf8'),key);
    console.log('Encrypted backup already present; existing signing identity retained.');
    return;
  }
  if(existing.some(name=>SECRET_NAMES.includes(name)))
    throw new Error('Target already has signing secrets but no controller backup; manual migration required, refusing key rotation');
  const material=createKeyMaterial(repository);
  mkdirSync(path.dirname(dest),{recursive:true});
  writeFileSync(dest,protectBackup(repository,material,key),{mode:0o600,flag:'wx'});
  console.log('Encrypted backup created for '+repository+'; ready for protected commit.');
}

function setSecret(name,value,repository,token) {
  return new Promise((resolve,reject)=>{
    const proc=spawn('gh',['secret','set',name,'--repo',repository],{
      stdio:['pipe','ignore','pipe'],
      env:{...process.env,GH_TOKEN:token},
    });
    // Suppress stderr to avoid leaking payload from third-party error messages.
    proc.stderr.resume();
    proc.on('error',()=>reject(new Error('GitHub CLI invocation failed')));
    proc.on('close',code=>code===0?resolve():reject(new Error('GitHub secret write failed')));
    proc.stdin.on('error',()=>{}); // Some error paths close stdin early.
    proc.stdin.end(value);
  });
}

async function apply(repository,key){
  const filename=path.join(BASE,backupFileName(repository));
  if(!existsSync(filename))
    throw new Error('Encrypted backup is missing. Run prepare and commit it first.');
  const values=recoverBackup(repository,readFileSync(filename,'utf8'),key);
  const token=await tokenFor(repository);
  const existing=await existingSecretNames(repository,token);
  const missing=missingSecrets(existing);
  for(const name of missing){
    await setSecret(name,values[name],repository,token);
    console.log('Stored: '+name);
  }
  const confirmed=await existingSecretNames(repository,token);
  if(missingSecrets(confirmed).length)
    throw new Error('Secret metadata verification incomplete – rerun with same encrypted backup');
  console.log('Signing secrets configured for '+repository+
              ' (keys never returned, existing signing identity preserved)');
}

async function main(){
  const mode=process.argv[2];
  if(mode!=='prepare'&&mode!=='apply')throw new Error('Expected mode prepare or apply');
  const repository=targetRepository(requireEnv('TARGET_REPOSITORY'));
  const key=requireEnv('BACKUP_KEY_BASE64');
  if(mode==='prepare')await prepare(repository,key);
  else await apply(repository,key);
}
if(process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  main().catch(e=>{
    console.error('Provisioning failed: '+e.message);
    process.exitCode=1;
  });
}
