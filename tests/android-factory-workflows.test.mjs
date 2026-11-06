import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const root=new URL('../',import.meta.url);
const load=name=>readFileSync(new URL('.github/workflows/'+name,root),'utf8');
test('Android CI is read-only and never receives signing secrets',()=>{
  const s=load('reusable-android-check.yml');
  for(const x of ['workflow_call:','testDebugUnitTest','lintDebug','assembleDebug','contents: read'])
    assert.ok(s.includes(x));
  assert.doesNotMatch(s,/secrets\.|contents: write|pull_request_target:|workflow_run:/);
});
test('Signing is separate from build, guarded by default branch and manual dispatch',()=>{
  const s=load('reusable-android-release.yml');
  const build=s.split('  build:')[1]?.split('  sign-and-publish:')[0];
  const signer=s.split('  sign-and-publish:')[1];
  assert.ok(build&&signer);
  assert.doesNotMatch(build,/secrets\.|STORE_PASSWORD|STORE_B64|KEY_PASSWORD|KEY_ALIAS/);
  for(const x of ['workflow_dispatch','github.event.repository.default_branch','applicationId',
                 'android-unsigned-for-signing'])assert.ok(s.includes(x));
  for(const x of ['needs: build','environment: android-releases','contents: write',
                 'apksigner','gh release create','gh release view'])assert.ok(signer.includes(x));
  assert.doesNotMatch(s,/pull_request_target:|workflow_run:|secrets: inherit/);
});
test('Release template maps app-specific keys explicitly',()=>{
  const s=readFileSync(new URL('templates/android-app/.github/workflows/android-release.yml',root),'utf8');
  for(const x of ['signing_keystore_base64','signing_store_password','signing_key_alias',
                 'signing_key_password']) assert.ok(s.includes(x+':'));
  assert.ok(s.includes('workflow_dispatch:'));
  assert.doesNotMatch(s,/secrets: inherit/);
});
