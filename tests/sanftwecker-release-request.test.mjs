import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  REQUEST_PATH,SOURCE_REF,validateReleaseRequest,assertExactSourceHead,
  assertRequestChanged
} from '../scripts/sanftwecker-release-request.mjs';

const good={schema:1,intent:'sanftwecker-signed-beta',
  request_id:'2026-11-06-first-beta',
  source_sha:'b8191b3626dd368e3a4b59da088b46b94b8ebf3b',
  expected_version_code:9};

test('signed beta request has a strict, non-secret schema',()=>{
  assert.deepEqual(validateReleaseRequest(good),
    {source_sha:good.source_sha,version_code:9});
  for(const x of [null,[],{}, {...good,secret:'oops'},
    {...good,intent:'delete-files'},{...good,source_sha:'feature/watch-vibration-poc'},
    {...good,request_id:'../bad'},{...good,expected_version_code:-1},
    {...good,expected_version_code:'9'}])
    assert.throws(()=>validateReleaseRequest(x));
});
test('only the exact reviewed source branch HEAD can be signed',()=>{
  assert.equal(assertExactSourceHead(good.source_sha,good.source_sha+'\t'+SOURCE_REF+'\n'),
    good.source_sha);
  assert.throws(()=>assertExactSourceHead(good.source_sha,'a'.repeat(40)+'\t'+SOURCE_REF));
  assert.throws(()=>assertExactSourceHead(good.source_sha,good.source_sha+'\trefs/heads/main'));
});
test('signing request must be changed in a real commit',()=>{
  const one='a'.repeat(40),two='b'.repeat(40);
  assert.doesNotThrow(()=>assertRequestChanged(one,two,REQUEST_PATH+'\n'));
  assert.throws(()=>assertRequestChanged(one,two,'README.md\n'));
  assert.throws(()=>assertRequestChanged('garbage',two,REQUEST_PATH));
});
test('release candidate never signs or publishes on pull requests',()=>{
  const yaml=readFileSync(new URL('../.github/workflows/sanftwecker-release-candidate.yml',
    import.meta.url),'utf8');
  const validate=yaml.split('\n  validate:')[1]?.split('\n  build:')[0]||'';
  const build=yaml.split('\n  build:')[1]?.split('\n  sign:')[0]||'';
  const sign=yaml.split('\n  sign:')[1]||'';
  for(const term of ['branches: [main]',REQUEST_PATH,
    "github.actor == 'CobraChris'","github.ref == 'refs/heads/main'",
    'node scripts/sanftwecker-release-request.mjs'])assert.ok(yaml.includes(term),term);
  assert.ok(validate&&build&&sign);
  assert.doesNotMatch(build,/secrets\.|STORE_PASSWORD|KEY_PASSWORD|KEY_ALIAS/);
  for(const term of ['contents: write','environment: android-releases',
    'APP_SIGNING_KEYSTORE_BASE64','APP_SIGNING_STORE_PASSWORD',
    'APP_SIGNING_KEY_PASSWORD','APP_SIGNING_KEY_ALIAS',
    'apksigner','gh release create','gh release download','sha256sum'])
    assert.ok(sign.includes(term),term);
  assert.doesNotMatch(yaml,/pull_request_target:|workflow_run:|secrets: inherit/);
});
