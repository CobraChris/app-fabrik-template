import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateRequest,validateEvent,validatePush,validateSource,SOURCE_REF}
  from '../scripts/fahrcockpit-release-request.mjs';
const sha='e6da2f64b027848d001b0f8f6158c5d5768171b0';
const request={schema:1,intent:'fahrcockpit-signed-beta',request_id:'2026-11-07-signed-fahr',
  source_sha:sha,expected_version_code:4};
test('strict request validation',()=>{
  assert.deepEqual(validateRequest(request),{sha,code:4});
  assert.throws(()=>validateRequest({...request,extra:'untrusted'}));
  assert.throws(()=>validateRequest({...request,expected_version_code:0}));
  assert.throws(()=>validateRequest({...request,source_sha:'main'}));
});
test('only exact reviewed source and main request change are accepted',()=>{
  assert.doesNotThrow(()=>validateSource(sha,sha+'\t'+SOURCE_REF+'\n'));
  assert.throws(()=>validateSource(sha,sha+'\trefs/heads/main\n'));
  assert.doesNotThrow(()=>validatePush(sha,sha,'requests/fahrcockpit-release.json\n'));
  assert.throws(()=>validatePush(sha,sha,'requests/fahrcockpit-release.json\nAGENTS.md\n'));
  const event={GITHUB_REPOSITORY:'CobraChris/app-fabrik-template',GITHUB_ACTOR:'CobraChris',
    GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
  assert.doesNotThrow(()=>validateEvent(event));
  assert.throws(()=>validateEvent({...event,GITHUB_ACTOR:'intruder'}));
});
test('release signing never uses shared Sanftwecker secrets',()=>{
  const y=readFileSync(new URL('../.github/workflows/fahrcockpit-release-candidate.yml',import.meta.url),'utf8');
  assert.match(y,/FAHRCOCKPIT_SIGNING_KEYSTORE_BASE64/);
  assert.match(y,/environment: android-releases/);
  assert.match(y,/fahrcockpit-release\.apk/);
  assert.doesNotMatch(y,/secrets\.APP_SIGNING_/);
  assert.doesNotMatch(y,/^\s+pull_request:/m);
  assert.doesNotMatch(y,/pull_request_target|workflow_run/);
});

test('GitHub release workflow expressions must be complete and Actions pinned',()=>{
  const y=readFileSync(new URL('../.github/workflows/fahrcockpit-release-candidate.yml',import.meta.url),'utf8');
  const complete=[...y.matchAll(/\$\{\{[^}\n]*\}\}/g)].length;
  assert.equal(complete,16,'Expected all sixteen GitHub expressions');
  assert.doesNotMatch(y,/\$\{\{[^}\n]*\}(?!\})/,'Broken GitHub expression');
  const refs=[...y.matchAll(/\buses:\s*([^\n\s]+)/g)].map(x=>x[1]);
  assert.equal(refs.length,7);
  assert.ok(refs.every(ref=>/@[a-f0-9]{40}$/.test(ref)),
    'Release pipeline must pin each action to an immutable commit SHA');
});
