import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateRequest,validateEvent,validatePush,validateSource,sourceRefFor,SOURCE_REF,GOOGLE_SOURCE_REF,GOOGLE_V22_SOURCE_REF,GOOGLE_V23_SOURCE_REF,GOOGLE_V23_APPROVED_SHA,GOOGLE_V24_SOURCE_REF,GOOGLE_V24_APPROVED_SHA,GOOGLE_V25_SOURCE_REF,GOOGLE_V25_APPROVED_SHA,GOOGLE_V26_SOURCE_REF,GOOGLE_V26_APPROVED_SHA,GOOGLE_V27_SOURCE_REF,GOOGLE_V27_APPROVED_SHA,GOOGLE_V28_SOURCE_REF,GOOGLE_V28_APPROVED_SHA,GOOGLE_V29_SOURCE_REF,GOOGLE_V29_APPROVED_SHA,GOOGLE_V31_SOURCE_REF,GOOGLE_V31_APPROVED_SHA,GOOGLE_V32_SOURCE_REF,GOOGLE_V32_APPROVED_SHA,GOOGLE_V33_SOURCE_REF,GOOGLE_V33_APPROVED_SHA,GOOGLE_V34_SOURCE_REF,GOOGLE_V34_APPROVED_SHA,GOOGLE_V35_SOURCE_REF,GOOGLE_V35_APPROVED_SHA,GOOGLE_V36_SOURCE_REF,GOOGLE_V36_APPROVED_SHA,GOOGLE_V37_SOURCE_REF,GOOGLE_V37_APPROVED_SHA}
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
    reviewed+'\t'+GOOGLE_V23_SOURCE_REF+'\n','google-sdk-only',23));
  assert.throws(()=>validateSource(reviewed,
    reviewed+'\t'+GOOGLE_V22_SOURCE_REF+'\n','google-sdk-only',23));
  assert.throws(()=>validateSource(reviewed,
    reviewed+'\t'+GOOGLE_V23_SOURCE_REF+'\n','google-sdk-only',22));
  assert.throws(()=>validateSource(reviewed,
    '0'.repeat(40)+'\t'+GOOGLE_V23_SOURCE_REF+'\n','google-sdk-only',23));
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

test('v24 Google-only release requires exact reviewed commit, branch and request',()=>{
  const pinned='61c73d12778f42d7481eef8a5eeabe7f2bf477a3';
  const request24={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-15-fahrcockpit-v24-google-only',
    source_sha:pinned,expected_version_code:24,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V24_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V24_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v24-compact-navigation-ui');
  assert.deepEqual(validateRequest(request24),
    {sha:pinned,code:24,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',24),GOOGLE_V24_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',24));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V23_SOURCE_REF+'\n','google-sdk-only',24));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',23));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',24));
  assert.throws(()=>validateRequest({...request24,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request24,
    request_id:'2026-11-15-fahrcockpit-v24-unreviewed'}));
  assert.throws(()=>validateRequest({...request24,expected_version_code:25}));
  assert.throws(()=>validateRequest({...request24,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request24,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request24,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request24,extra:true}));
  assert.throws(()=>validateRequest({...request24,schema:1}));
});


test('v25 Saphe notification probe release requires fixed SHA, Google-only branch and exact request',()=>{
  const pinned='e915f6dfdb49af1a2ac3d77219e723716a82bbed';
  const request25={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-16-fahrcockpit-v25-google-only',
    source_sha:pinned,expected_version_code:25,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V25_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V25_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request25),
    {sha:pinned,code:25,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',25),GOOGLE_V25_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V25_SOURCE_REF+'\n','google-sdk-only',25));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',25));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V25_SOURCE_REF+'\n','google-sdk-only',24));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V25_SOURCE_REF+'\n','google-sdk-only',25));
  assert.throws(()=>validateRequest({...request25,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request25,request_id:'2026-11-16-fahrcockpit-v25-other'}));
  assert.throws(()=>validateRequest({...request25,expected_version_code:26}));
  assert.throws(()=>validateRequest({...request25,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request25,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request25,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request25,extra:true}));
});


test('v26 unified favorites release is pinned to reviewed source and exact request',()=>{
  const pinned='10d490addbdf1dbbb9933b58ead8faf3599eab2b';
  const request26={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-16-fahrcockpit-v26-unified-favorites',
    source_sha:pinned,expected_version_code:26,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V26_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V26_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request26),
    {sha:pinned,code:26,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',26),GOOGLE_V26_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V26_SOURCE_REF+'\n','google-sdk-only',26));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',26));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V26_SOURCE_REF+'\n','google-sdk-only',26));
  assert.throws(()=>validateRequest({...request26,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request26,request_id:'2026-11-16-other'}));
  assert.throws(()=>validateRequest({...request26,expected_version_code:27}));
  assert.throws(()=>validateRequest({...request26,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request26,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request26,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request26,extra:true}));
});

test('v27 compact UX release allows exactly one reviewed commit and request',()=>{
  const pinned='c0b89ecc7ae7a0c9c68d1d9270ee79c97c23c913';
  const request27={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-17-fahrcockpit-v27-compact-ux',
    source_sha:pinned,expected_version_code:27,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V27_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V27_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request27),
    {sha:pinned,code:27,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',27),GOOGLE_V27_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V27_SOURCE_REF+'\n','google-sdk-only',27));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',27));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V27_SOURCE_REF+'\n','google-sdk-only',27));
  assert.throws(()=>validateRequest({...request27,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request27,request_id:'2026-11-17-unapproved'}));
  assert.throws(()=>validateRequest({...request27,expected_version_code:28}));
  assert.throws(()=>validateRequest({...request27,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request27,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request27,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request27,extra:true}));
});


test('v28 map price release is pinned to reviewed source, branch and exact request',()=>{
  const pinned='950438faf31298dfabb94cb6b79fc50ca88825f3';
  const request28={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-17-fahrcockpit-v28-fuel-map-prices',
    source_sha:pinned,expected_version_code:28,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V28_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V28_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request28),
    {sha:pinned,code:28,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',28),GOOGLE_V28_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V28_SOURCE_REF+'\n','google-sdk-only',28));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',28));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V28_SOURCE_REF+'\n','google-sdk-only',28));
  assert.throws(()=>validateRequest({...request28,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request28,request_id:'2026-11-17-unapproved'}));
  assert.throws(()=>validateRequest({...request28,expected_version_code:29}));
  assert.throws(()=>validateRequest({...request28,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request28,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request28,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request28,extra:true}));
});

test('v29 one-tap fuel release requires exact reviewed source and request',()=>{
  const pinned='88d7cd8b60dea038505c20ef4b61130dbaee3e56';
  const request29={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-17-fahrcockpit-v29-quick-fuel-controls',
    source_sha:pinned,expected_version_code:29,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V29_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V29_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request29),
    {sha:pinned,code:29,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',29),GOOGLE_V29_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V29_SOURCE_REF+'\n','google-sdk-only',29));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V24_SOURCE_REF+'\n','google-sdk-only',29));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V29_SOURCE_REF+'\n','google-sdk-only',29));
  assert.throws(()=>validateRequest({...request29,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request29,request_id:'2026-11-17-unapproved'}));
  assert.throws(()=>validateRequest({...request29,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request29,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request29,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request29,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request29,extra:true}));
  assert.throws(()=>validateRequest({...request29,schema:1}));
});

test('v31 Saphe diagnostic release pins reviewed Google-only source and rejects v30',()=>{
  const pinned='cd3823114ca5c6a780c450bf2b8bbbcc13cf6e98';
  const request31={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-18-fahrcockpit-v31-saphe-diagnostics',
    source_sha:pinned,expected_version_code:31,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V31_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V31_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request31),
    {sha:pinned,code:31,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',31),GOOGLE_V31_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V31_SOURCE_REF+'\n','google-sdk-only',31));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V29_SOURCE_REF+'-other\n','google-sdk-only',31));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V31_SOURCE_REF+'\n','google-sdk-only',31));
  assert.throws(()=>validateRequest({...request31,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request31,request_id:'2026-11-18-fahrcockpit-v31-other'}));
  assert.throws(()=>validateRequest({...request31,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request31,expected_version_code:32}));
  assert.throws(()=>validateRequest({...request31,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request31,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request31,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request31,extra:true}));
  assert.throws(()=>validateRequest({...request31,schema:1}));
});

test('v32 neutral Safety prototype requires exact Google-only source and request',()=>{
  const pinned='9210adcf150d926dcd5505c3c5366c640ab4e9c9';
  const request32={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-18-fahrcockpit-v32-safety-banner-retry1',
    source_sha:pinned,expected_version_code:32,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V32_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V32_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request32),
    {sha:pinned,code:32,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',32),GOOGLE_V32_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V32_SOURCE_REF+'\n','google-sdk-only',32));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+'refs/heads/feature/fahrcockpit-v32-safety-event-banner'+'\n','google-sdk-only',32));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V32_SOURCE_REF+'\n','google-sdk-only',32));
  assert.throws(()=>validateRequest({...request32,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request32,request_id:'2026-11-18-fahrcockpit-v32-other'}));
  assert.throws(()=>validateRequest({...request32,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request32,expected_version_code:33}));
  assert.throws(()=>validateRequest({...request32,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request32,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request32,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request32,extra:true}));
  assert.throws(()=>validateRequest({...request32,schema:1}));
});

test('v33 safety and GPS speed release requires exact reviewed Google-only source',()=>{
  const pinned='ce02953faeeaf0cb308196794d306c94c1dd2537';
  const request33={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-21-fahrcockpit-v33-safety-speed',
    source_sha:pinned,expected_version_code:33,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V33_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V33_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request33),
    {sha:pinned,code:33,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',33),GOOGLE_V33_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V33_SOURCE_REF+'\n','google-sdk-only',33));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+'refs/heads/feature/fahrcockpit-v33-safety-speed'+'\n','google-sdk-only',33));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V33_SOURCE_REF+'\n','google-sdk-only',33));
  assert.throws(()=>validateRequest({...request33,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request33,request_id:'2026-11-21-fahrcockpit-v33-unreviewed'}));
  assert.throws(()=>validateRequest({...request33,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request33,expected_version_code:34}));
  assert.throws(()=>validateRequest({...request33,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request33,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request33,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request33,extra:true}));
  assert.throws(()=>validateRequest({...request33,schema:1}));
});

test('v34 one-speed/no-billing-dialog release requires exact reviewed Google-only source',()=>{
  const pinned='45d80902a08daa320890ddb36306debed64c71f4';
  const request34={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-21-fahrcockpit-v34-one-speed-no-billing-dialog',
    source_sha:pinned,expected_version_code:34,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V34_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V34_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request34),
    {sha:pinned,code:34,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',34),GOOGLE_V34_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V34_SOURCE_REF+'\n','google-sdk-only',34));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+'refs/heads/feature/fahrcockpit-v34-one-speedometer-no-billing-dialog'+'\n','google-sdk-only',34));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V34_SOURCE_REF+'\n','google-sdk-only',34));
  assert.throws(()=>validateRequest({...request34,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request34,request_id:'2026-11-21-fahrcockpit-v34-unreviewed'}));
  assert.throws(()=>validateRequest({...request34,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request34,expected_version_code:35}));
  assert.throws(()=>validateRequest({...request34,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request34,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request34,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request34,extra:true}));
  assert.throws(()=>validateRequest({...request34,schema:1}));
});

test('v35 complete UX, verified in-app updater and overflow menu require exact signed Google-only source',()=>{
  const pinned='2072adbfeecab262c5fd356299a507779a09f41b';
  const request35={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-22-fahrcockpit-v35-complete-ux-updater-options',
    source_sha:pinned,expected_version_code:35,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V35_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V35_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request35),
    {sha:pinned,code:35,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',35),GOOGLE_V35_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V35_SOURCE_REF+'\n','google-sdk-only',35));
  assert.throws(()=>validateSource(pinned,
    pinned+'\t'+'refs/heads/feature/fahrcockpit-v35-complete-cockpit-ux'+'\n','google-sdk-only',35));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V35_SOURCE_REF+'\n','google-sdk-only',35));
  assert.throws(()=>validateRequest({...request35,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request35,request_id:'2026-11-22-fahrcockpit-v35-unreviewed'}));
  assert.throws(()=>validateRequest({...request35,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request35,expected_version_code:36}));
  assert.throws(()=>validateRequest({...request35,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request35,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request35,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request35,extra:true}));
  assert.throws(()=>validateRequest({...request35,schema:1}));
});


test('v36 gated price cache release requires exact pinned Google-only source, auto API remains off',()=>{
  const pinned='0ed2862aa7379f8aaccc86d99ce288627ecaf877';
  const request36={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-22-fahrcockpit-v36-fuel-guard-offline-no-auto',
    source_sha:pinned,expected_version_code:36,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V36_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V36_SOURCE_REF,'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request36),{sha:pinned,code:36,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',36),GOOGLE_V36_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,pinned+'\t'+GOOGLE_V36_SOURCE_REF+'\n','google-sdk-only',36));
  assert.throws(()=>validateSource(pinned,'0'.repeat(40)+'\t'+GOOGLE_V36_SOURCE_REF+'\n','google-sdk-only',36));
  assert.throws(()=>validateRequest({...request36,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request36,request_id:'2026-11-22-fahrcockpit-v36-unreviewed'}));
  assert.throws(()=>validateRequest({...request36,expected_version_code:37}));
  assert.throws(()=>validateRequest({...request36,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request36,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request36,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request36,extra:true}));
  assert.throws(()=>validateRequest({...request36,schema:1}));
});


test('v37 parking and enlarged Saphe alert release uses exact Google-only approved source',()=>{
  const pinned='14ee53d42f8a3602287f6c327e3121f14fb606b9';
  const request37={schema:2,intent:'fahrcockpit-signed-beta',
    request_id:'2026-11-25-fahrcockpit-v37-parking-saphe-large-safety',
    source_sha:pinned,expected_version_code:37,variant:'google-sdk-only'};
  assert.equal(GOOGLE_V37_APPROVED_SHA,pinned);
  assert.equal(GOOGLE_V37_SOURCE_REF,
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe');
  assert.deepEqual(validateRequest(request37),
    {sha:pinned,code:37,variant:'google-sdk-only'});
  assert.equal(sourceRefFor('google-sdk-only',37),GOOGLE_V37_SOURCE_REF);
  assert.doesNotThrow(()=>validateSource(pinned,
    pinned+'\t'+GOOGLE_V37_SOURCE_REF+'\n','google-sdk-only',37));
  assert.throws(()=>validateSource(pinned,
    '0'.repeat(40)+'\t'+GOOGLE_V37_SOURCE_REF+'\n','google-sdk-only',37));
  assert.throws(()=>validateSource(pinned,
    pinned+'\trefs/heads/feature/fahrcockpit-v37-parking-large-saphe-alert\n',
    'google-sdk-only',37));
  assert.throws(()=>validateRequest({...request37,source_sha:'0'.repeat(40)}));
  assert.throws(()=>validateRequest({...request37,request_id:
    '2026-11-25-fahrcockpit-v37-unreviewed'}));
  assert.throws(()=>validateRequest({...request37,expected_version_code:38}));
  assert.throws(()=>validateRequest({...request37,expected_version_code:30}));
  assert.throws(()=>validateRequest({...request37,variant:'legacy'}));
  assert.throws(()=>validateRequest({...request37,variant:'google-osm-integrated'}));
  assert.throws(()=>validateRequest({...request37,variant:'google-places-activated'}));
  assert.throws(()=>validateRequest({...request37,extra:true}));
  assert.throws(()=>validateRequest({...request37,schema:1}));
});
