import test from 'node:test';
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
import {
  targetRepository,backupFileName,protectBackup,recoverBackup,
  missingSecrets,SECRET_NAMES
} from '../templates/github-admin-controller/scripts/provision-core.mjs';

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
  assert.doesNotMatch(workflow,/pull_request_target:|workflow_run:|push:\s*\n/);
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
