import test from 'node:test';
import assert from 'node:assert/strict';
import { randomBytes, generateKeyPairSync } from 'node:crypto';
import { readFileSync } from 'node:fs';
import {
  targetRepository,backupFileName,protectBackup,recoverBackup,
  missingSecrets,SECRET_NAMES,deriveBackupKey
} from '../templates/github-admin-controller/scripts/provision-core.mjs';
import { existingSecretNames, tokenFor } from '../templates/github-admin-controller/scripts/provision.mjs';
import { allowedRepositories, validateRequest, approveManualRequest } from '../templates/github-admin-controller/scripts/validate-request.mjs';

const REPO='CobraChris/meine-app';
const PASSWORD=randomBytes(32).toString('base64');
const VAL={
  APP_SIGNING_KEYSTORE_BASE64:'AAABAQIDAQAAAQ==',
  APP_SIGNING_STORE_PASSWORD:'private-store-pass',
  APP_SIGNING_KEY_ALIAS:'appsign',
  APP_SIGNING_KEY_PASSWORD:'private-key-pass',
};

test('controller only accepts CobraChris repositories with safe names',()=>{
  assert.equal(targetRepository(REPO),REPO);
  assert.equal(backupFileName(REPO),'backups/meine-app.json.enc');
  for(const value of [
    'OtherOwner/my-app','CobraChris/../evil',
    'CobraChris/dir/evil','CobraChris/.','CobraChris/repo..',
    'CobraChris/repo?x=1',null,'CobraChris/',
  ])assert.throws(()=>targetRepository(value));
});
test('controller encrypted backup round trips without exposing plaintext',()=>{
  const encrypted=protectBackup(REPO,VAL,PASSWORD);
  assert.ok(encrypted.includes('aes-256-gcm'));
  assert.ok(!encrypted.includes('private-store-pass'));
  assert.ok(!encrypted.includes('AAABAQIDAQAAAQ=='));
  assert.deepEqual(recoverBackup(REPO,encrypted,PASSWORD),VAL);
});
test('controller refuses secret recovery with wrong key or different repository',()=>{
  const encrypted=protectBackup(REPO,VAL,PASSWORD);
  const other=randomBytes(32).toString('base64');
  assert.throws(()=>recoverBackup(REPO,encrypted,other));
  assert.throws(()=>recoverBackup('CobraChris/andere-app',encrypted,PASSWORD));
  const doc=JSON.parse(encrypted);
  doc.ciphertext=doc.ciphertext.slice(0,-3)+'AAA';
  assert.throws(()=>recoverBackup(REPO,JSON.stringify(doc),PASSWORD));
});
test('controller verifies exact 256-bit master key input',()=>{
  assert.throws(()=>protectBackup(REPO,VAL,'my-short-password'));
  assert.throws(()=>protectBackup(REPO,VAL,randomBytes(16).toString('base64')));
  assert.throws(()=>protectBackup(REPO,{},PASSWORD));
});
test('controller only installs missing secrets; no automatic rotation',()=>{
  assert.deepEqual(missingSecrets([]),SECRET_NAMES);
  assert.deepEqual(missingSecrets([SECRET_NAMES[0]]),SECRET_NAMES.slice(1));
  assert.deepEqual(missingSecrets(SECRET_NAMES),[]);
  assert.throws(()=>missingSecrets('not-an-array'));
});
test('privileged workflow requires private repo and manual owner dispatch',()=>{
  const file=new URL('../templates/github-admin-controller/.github/workflows/provision-android-signing.yml',import.meta.url);
  const workflow=readFileSync(file,'utf8');
  for(const required of [
    'workflow_dispatch:',"github.actor == 'CobraChris'",
    "github.repository == 'CobraChris/github-app-fabrik-control'",
    'github.event.repository.private == true',
    'environment: signing-administration',
    'node scripts/provision.mjs prepare',
    'git push origin HEAD:main',
    'node scripts/provision.mjs apply',
  ])assert.ok(workflow.includes(required),required);
  assert.doesNotMatch(workflow,/pull_request_target:|workflow_run:/);
  assert.ok(workflow.includes("paths: ['requests/provision.json']"));
  assert.ok(workflow.includes('node scripts/validate-request.mjs'));
  assert.ok(workflow.indexOf('node scripts/provision.mjs prepare') <
            workflow.indexOf('node scripts/provision.mjs apply'));
});
test('provisioner refuses key replacement if target has keys without backup',()=>{
  const src=readFileSync(
    new URL('../templates/github-admin-controller/scripts/provision.mjs',import.meta.url),'utf8');
  assert.ok(src.includes('manual migration required, refusing key rotation'));
  assert.ok(src.includes('for(const name of missing)'));
  assert.ok(src.includes("GH_TOKEN:token"));
  assert.doesNotMatch(src,/console\.log\(material|console\.log\(values/);
});

test('secret inventory pagination prevents accidental key rotation',async()=>{
  const repo='CobraChris/long-list';
  const page1=Array.from({length:100},(_,n)=>({name:'OTHER_'+n}));
  const page2=[{name:'APP_SIGNING_KEYSTORE_BASE64'},{name:'APP_SIGNING_KEY_ALIAS'}];
  const pages=[];
  const fake=async(url)=>{
    pages.push(url);
    return {ok:true,json:async()=>({secrets:pages.length===1?page1:page2})};
  };
  const names=await existingSecretNames(repo,'ephemeral-test-token',fake);
  assert.equal(pages.length,2);
  assert.equal(names.length,102);
  assert.deepEqual(missingSecrets(names),[
    'APP_SIGNING_STORE_PASSWORD','APP_SIGNING_KEY_PASSWORD'
  ]);
});
test('controller fails closed if inventory metadata cannot be read',async()=>{
  await assert.rejects(existingSecretNames('CobraChris/app','token',
    async()=>({ok:false,status:403})),/HTTP 403/);
});

test('controller discovers its own installation directly from the approved repository',async()=>{
  const keys=generateKeyPairSync('rsa',{modulusLength:2048});
  const pem=keys.privateKey.export({type:'pkcs8',format:'pem'});
  const paths=[];
  const fake=async(url,init)=>{
    paths.push({url,init});
    if(url.endsWith('/installation'))
      return {ok:true,json:async()=>({
        id:998811,app_id:5640822,account:{login:'CobraChris'},
        permissions:{secrets:'write'}
      })};
    return {ok:true,json:async()=>({token:'mock-short-lived-installation-token'})};
  };
  const actual=await tokenFor('CobraChris/app-fabrik-template',fake,{
    GH_APP_ID:'5640822',GH_APP_PRIVATE_KEY_PEM:pem
  });
  assert.equal(actual,'mock-short-lived-installation-token');
  assert.equal(paths.length,2);
  assert.equal(paths[0].url,'https://api.github.com/repos/CobraChris/app-fabrik-template/installation');
  assert.equal(paths[1].url,'https://api.github.com/app/installations/998811/access_tokens');
  assert.deepEqual(JSON.parse(paths[1].init.body),{
    repositories:['app-fabrik-template'],permissions:{secrets:'write'}
  });
  assert.ok(paths[0].init.headers.Authorization.startsWith('Bearer ey'));
});
test('controller refuses an app installation with wrong permissions or app id',async()=>{
  const keys=generateKeyPairSync('rsa',{modulusLength:2048});
  const pem=keys.privateKey.export({type:'pkcs8',format:'pem'});
  for(const altered of [{app_id:1},{account:{login:'SomebodyElse'}},{permissions:{secrets:'read'}}]){
    const correct={id:998811,app_id:5640822,account:{login:'CobraChris'},
                   permissions:{secrets:'write'},...altered};
    let calls=0;
    const fake=async()=>{calls++;return {ok:true,json:async()=>correct};};
    await assert.rejects(tokenFor('CobraChris/app-fabrik-template',fake,{
      GH_APP_ID:'5640822',GH_APP_PRIVATE_KEY_PEM:pem
    }),/Unexpected GitHub App installation/);
    assert.equal(calls,1);
  }
});
test('controller gives actionable error if target has not been granted to the GitHub App',async()=>{
  const keys=generateKeyPairSync('rsa',{modulusLength:2048});
  const pem=keys.privateKey.export({type:'pkcs8',format:'pem'});
  await assert.rejects(tokenFor('CobraChris/new-app',
    async()=>({ok:false,status:404}),{
      GH_APP_ID:'5640822',GH_APP_PRIVATE_KEY_PEM:pem
    }),/no access to selected repository/);
});

test('chat-visible recovery salt never decrypts a signing backup on its own',()=>{
  const keys=generateKeyPairSync('rsa',{modulusLength:2048});
  const pem=keys.privateKey.export({format:'pem',type:'pkcs8'});
  const salt=randomBytes(32).toString('base64');
  const derived=deriveBackupKey(pem,salt,REPO);
  assert.notEqual(derived,salt);
  const encrypted=protectBackup(REPO,VAL,derived);
  assert.throws(()=>recoverBackup(REPO,encrypted,salt));
  assert.deepEqual(recoverBackup(REPO,encrypted,deriveBackupKey(pem,salt,REPO)),VAL);
});
test('signing backups use a different key for each repository',()=>{
  const pair=generateKeyPairSync('rsa',{modulusLength:2048});
  const pem=pair.privateKey.export({format:'pem',type:'pkcs8'});
  const salt=randomBytes(32).toString('base64');
  const first=deriveBackupKey(pem,salt,'CobraChris/app-one');
  const second=deriveBackupKey(pem,salt,'CobraChris/app-two');
  assert.equal(Buffer.from(first,'base64').length,32);
  assert.notEqual(first,second);
  assert.equal(first,deriveBackupKey(pem,salt,'CobraChris/app-one'));
});
test('backup derivation depends on private key and rejects malformed PEM',()=>{
  const a=generateKeyPairSync('rsa',{modulusLength:2048});
  const b=generateKeyPairSync('rsa',{modulusLength:2048});
  const salt=randomBytes(32).toString('base64');
  const pem=x=>x.privateKey.export({format:'pem',type:'pkcs8'});
  assert.notEqual(deriveBackupKey(pem(a),salt,REPO),deriveBackupKey(pem(b),salt,REPO));
  assert.throws(()=>deriveBackupKey('invalid pem',salt,REPO));
});

test('GitOps signing requests require explicit schema and pre-approved repositories',()=>{
  const allowed=allowedRepositories(['CobraChris/app-fabrik-template']);
  const valid={schema:1,intent:'create-app-signing-secrets',
    target_repository:'CobraChris/app-fabrik-template',
    request_id:'2026-11-06-initial'};
  assert.equal(validateRequest(valid,allowed),'CobraChris/app-fabrik-template');
  assert.equal(approveManualRequest(valid.target_repository,allowed),valid.target_repository);
  assert.throws(()=>validateRequest({...valid,target_repository:'CobraChris/other'},allowed),/not pre-approved/);
  assert.throws(()=>validateRequest({...valid,secret:'trick'},allowed),/schema mismatch/);
  assert.throws(()=>validateRequest({...valid,intent:'delete-secrets'},allowed),/Invalid/);
  assert.throws(()=>validateRequest({...valid,request_id:'../../evil'},allowed),/Invalid/);
  assert.throws(()=>approveManualRequest('CobraChris/other',allowed),/not pre-approved/);
});
test('GitOps signing allowlist never includes the controller itself',()=>{
  assert.throws(()=>allowedRepositories(['CobraChris/github-app-fabrik-control']));
  assert.throws(()=>allowedRepositories(['OtherOrganization/target']));
  assert.throws(()=>allowedRepositories(['CobraChris/app','CobraChris/app']));
});
