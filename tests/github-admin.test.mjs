import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, verify } from 'node:crypto';
import { GitHubSecretsAdmin } from '../integrations/github-admin/github-secrets.mjs';
import { makeAppJwt, createInstallationTokenProvider } from '../integrations/github-admin/github-app-auth.mjs';

const REPO='CobraChris/app-fabrik-template';
const NAME='SANFTWECKER_SIGNING_PASSWORD';
const VAULT='vault://android/sanftwecker/password';
const calls=[];
const createClient=({fetchImpl,authorizeWrite=async()=>true}={})=>new GitHubSecretsAdmin({
  allowedRepositories:[REPO],
  allowedSecretNames:[NAME],
  allowedVaultRefs:[VAULT],
  getInstallationToken:async()=>'short-lived-test-token',
  readVaultSecret:async()=>'server-only-plaintext',
  sealForGitHub:async(secret,key)=>{
    assert.equal(secret,'server-only-plaintext');
    assert.equal(key,'QUJD');
    return 'encrypted-ciphertext';
  },
  authorizeWrite,
  fetchImpl:fetchImpl||async(url,init)=>{
    calls.push({url,init});
    if(url.endsWith('/public-key'))
      return {ok:true,json:async()=>({key:'QUJD',key_id:'key_123'})};
    return {ok:true,status:201};
  },
});

test('rejects repositories, secret names and vault references outside allowlists',async()=>{
  const client=createClient();
  await assert.rejects(client.installFromVault({repository:'Attacker/repo',secretName:NAME,vaultRef:VAULT}),/allowlist/);
  await assert.rejects(client.installFromVault({repository:REPO,secretName:'GITHUB_TOKEN',vaultRef:VAULT}),/not approved/);
  await assert.rejects(client.installFromVault({repository:REPO,secretName:NAME,vaultRef:'vault://unknown'}),/not enrolled/);
});
test('requires trusted backend approval before any HTTP calls',async()=>{
  let count=0;
  const client=createClient({authorizeWrite:async()=>false,fetchImpl:async()=>{count++;throw Error('wrong');}});
  await assert.rejects(client.installFromVault({repository:REPO,secretName:NAME,vaultRef:VAULT}),/approval/);
  assert.equal(count,0);
});
test('only sealed ciphertext reaches GitHub, never plaintext',async()=>{
  calls.length=0;
  const result=await createClient().installFromVault({repository:REPO,secretName:NAME,vaultRef:VAULT});
  assert.equal(result.plaintext_returned,false);
  assert.equal(calls.length,2);
  assert.ok(calls[0].url.endsWith('/actions/secrets/public-key'));
  assert.equal(calls[1].init.method,'PUT');
  assert.deepEqual(JSON.parse(calls[1].init.body),{encrypted_value:'encrypted-ciphertext',key_id:'key_123'});
  assert.ok(!JSON.stringify(calls).includes('server-only-plaintext'));
});
test('403 errors never expose secrets',async()=>{
  const client=createClient({fetchImpl:async()=>({ok:false,status:403})});
  await assert.rejects(client.installFromVault({repository:REPO,secretName:NAME,vaultRef:VAULT}),/HTTP 403/);
});
test('lists allowlisted metadata only',async()=>{
  const client=createClient({fetchImpl:async()=>({ok:true,json:async()=>({secrets:[
    {name:NAME,updated_at:'2026-01-01'},{name:'OTHER_TOKEN',updated_at:'2026-01-01'},
  ]})})});
  assert.deepEqual(await client.listSecretNames(REPO),[{name:NAME,updated_at:'2026-01-01'}]);
});
test('GitHub App JWT is verifiable RS256 and expires within 10 minutes',()=>{
  const pair=generateKeyPairSync('rsa',{modulusLength:2048});
  const privateKeyPem=pair.privateKey.export({type:'pkcs8',format:'pem'});
  const jwt=makeAppJwt({appId:1234,privateKeyPem,nowSeconds:10000});
  const [header,payload,sig]=jwt.split('.');
  assert.equal(JSON.parse(Buffer.from(header,'base64url')).alg,'RS256');
  assert.deepEqual(JSON.parse(Buffer.from(payload,'base64url')),{iat:9940,exp:10540,iss:'1234'});
  assert.equal(verify('RSA-SHA256',Buffer.from(header+'.'+payload),pair.publicKey,Buffer.from(sig,'base64url')),true);
});
test('installation token is short-lived and restricted to secrets:write',async()=>{
  const pair=generateKeyPairSync('rsa',{modulusLength:2048});
  let request,count=0;
  const get=createInstallationTokenProvider({
    appId:123,
    privateKeyPem:pair.privateKey.export({type:'pkcs8',format:'pem'}),
    installationId:987,
    repositoryNames:['app-fabrik-template'],
    fetchImpl:async(url,init)=>{
      request={url,init};count++;
      return {ok:true,json:async()=>({token:'short-lived',expires_at:new Date(Date.now()+3600000).toISOString()})};
    },
  });
  assert.equal(await get(),'short-lived');
  assert.equal(await get(),'short-lived');
  assert.equal(count,1);
  assert.deepEqual(JSON.parse(request.init.body),{repositories:['app-fabrik-template'],permissions:{secrets:'write'}});
});
