import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateRequest,validateEvent,validatePush,validateSource,sourceRefFor,SOURCE_REF,GOOGLE_SOURCE_REF,GOOGLE_V22_SOURCE_REF,GOOGLE_V23_SOURCE_REF,GOOGLE_V23_APPROVED_SHA}
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
  assert.equal(complete,18,'Expected all eighteen GitHub expressions');
  assert.doesNotMatch(y,/\$\{\{[^}\n]*\}(?!\})/,'Broken GitHub expression');
  const refs=[...y.matchAll(/\buses:\s*([^\n\s]+)/g)].map(x=>x[1]);
  assert.equal(refs.length,8);
  assert.ok(refs.every(ref=>/@[a-f0-9]{40}$/.test(ref)),
    'Release pipeline must pin each action to an immutable commit SHA');
});

test('v21+ requires explicit, pinned Google SDK only release request',()=>{
  const v21={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-13-fahrcockpit-v21-google-only',source_sha:sha,
    expected_version_code:21,variant:'google-sdk-only'};
  assert.deepEqual(validateRequest(v21),
    {sha,code:21,variant:'google-sdk-only'});
  assert.throws(()=>validateRequest({...request,expected_version_code:21}),
    /schema/);
  assert.throws(()=>validateRequest({...v21,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...v21,variant:'legacy'}));
  assert.throws(()=>validateRequest({...v21,expected_version_code:20}));
  assert.throws(()=>validateRequest({...v21,skip_tests:true}));
});

test('v21 protected release contains Google SDK exclusively, no OSM and no API key',()=>{
  const y=readFileSync(new URL('../.github/workflows/fahrcockpit-release-candidate.yml',
    import.meta.url),'utf8');
  assert.match(y,/VARIANT: \$\{\{ needs\.validate\.outputs\.variant \}\}/);
  assert.match(y,/FAHRCOCKPIT_NAV_SDK_KEY: ''/);
  assert.match(y,/-PwithGoogleNavSdk=true -PgoogleOnly=true/);
  assert.doesNotMatch(y,/-PwithOsmFallback=true/);
  assert.match(y,/if \[ "\$VARIANT" = 'legacy' \]/);
  assert.match(y,/elif \[ "\$VARIANT" = 'google-sdk-only' \]/);
  assert.doesNotMatch(y,/gh release create [^\n]*-PwithGoogleNavSdk/);
});

test('v21 source pinning rejects any unapproved source branch or commit',()=>{
  assert.equal(GOOGLE_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v21-google-first-fallback');
  assert.doesNotThrow(()=>validateSource(
    sha,sha+'\t'+GOOGLE_SOURCE_REF+'\n','google-sdk-only'));
  assert.throws(()=>validateSource(
    sha,sha+'\t'+SOURCE_REF+'\n','google-sdk-only'));
  assert.throws(()=>validateSource(
    sha,sha+'\t'+GOOGLE_SOURCE_REF+'\n','legacy'));
  assert.throws(()=>validateSource(
    sha,'0'.repeat(40)+'\t'+GOOGLE_SOURCE_REF+'\n','google-sdk-only'));
});

test('Google-only signing workflow installs its required Android API 36',()=>{
 const workflow=readFileSync(new URL('../.github/workflows/fahrcockpit-release-candidate.yml',import.meta.url),'utf8');
 assert.match(workflow,/cmdline-tools\/latest\/bin\/sdkmanager" "platforms;android-36"/);
 assert.match(workflow,/-PwithGoogleNavSdk=true -PgoogleOnly=true/);
 assert.doesNotMatch(workflow,/-PwithOsmFallback=true/);
});


test('v22 source gate pins the exact reviewed feature branch, not v21 or main',()=>{
  const request22={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-13-fahrcockpit-v22-google-only',
    source_sha:sha,expected_version_code:22,variant:'google-sdk-only'};
  assert.deepEqual(validateRequest(request22),
    {sha,code:22,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',22),GOOGLE_V22_SOURCE_REF);
  assert.equal(sourceRefFor('google-sdk-only',21),GOOGLE_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(sha,
    sha+'\t'+GOOGLE_V22_SOURCE_REF+'\n','google-sdk-only',22));
  assert.throws(()=>validateSource(sha,
    sha+'\t'+GOOGLE_SOURCE_REF+'\n','google-sdk-only',22));
  assert.throws(()=>validateSource(sha,
    sha+'\t'+GOOGLE_V22_SOURCE_REF+'\n','google-sdk-only',21));
  assert.throws(()=>validateSource(sha,
    '0'.repeat(40)+'\t'+GOOGLE_V22_SOURCE_REF+'\n','google-sdk-only',22));
  assert.throws(()=>validateRequest({...request22,
    request_id:'2026-11-13-fahrcockpit-v22-fake'}));
  assert.throws(()=>validateRequest({...request22,expected_version_code:23}));
  assert.throws(()=>validateRequest({...request22,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request22,extra:true}));
});

test('v23 accepts only the one already-tested source SHA and only approved Google branch',()=>{
  const reviewed = '151c382a6afad6890500c8be5d6b8bf2cb8f3f1a';
  const request23={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-14-fahrcockpit-v23-google-only',
    source_sha:reviewed,expected_version_code:23,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V23_APPROVED_SHA,reviewed);
  assert.equal(GOOGLE_V23_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v23-destinations-places-ui');
  assert.deepEqual(validateRequest(request23),
    {sha:reviewed,code:23,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',23),GOOGLE_V23_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(reviewed,
    reviewed+'\\t'+GOOGLE_V23_SOURCE_REF+'\\n','google-sdk-only',23));
  assert.throws(()=>validateSource(reviewed,
    reviewed+'\\t'+GOOGLE_V22_SOURCE_REF+'\\n','google-sdk-only',23));
  assert.throws(()=>validateSource(reviewed,
    reviewed+'\\t'+GOOGLE_V23_SOURCE_REF+'\\n','google-sdk-only',22));
  assert.throws(()=>validateSource(reviewed,
    '0'.repeat(40)+'\\t'+GOOGLE_V23_SOURCE_REF+'\\n','google-sdk-only',23));
  assert.throws(()=>validateRequest({...request23,
    source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request23,
    request_id:'2026-11-14-fahrcockpit-v23-unreviewed'}));
  assert.throws(()=>validateRequest({...request23,
    expected_version_code:24}));
  assert.throws(()=>validateRequest({...request23,
    variant:'legacy'}));
  assert.throws(()=>validateRequest({...request23,
    variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request23,extra:true}));
});
