import test from 'node:test';
import assert from 'node:assert/strict';
import {inspectFile,inspectWorkflow,auditTrackedFiles}
  from '../scripts/security-check.mjs';

const sample=(text,file='src/example.js')=>inspectFile(file,Buffer.from(text));
const codes=items=>items.filter(x=>x.level==='error').map(x=>x.code);

test('secret file extensions fail closed even for binary payloads',()=>{
  for(const file of ['.env','.env.production','android/release.jks',
     'signing/release.p12','private/server.key','keys/app.pem']){
    assert.ok(codes(sample('\0binary',file)).includes('PRIVATE_MATERIAL_FILE'),file);
  }
  assert.equal(codes(sample('EXAMPLE=not-a-secret','.env.example')).length,0);
});
test('high confidence GitHub, AWS, OpenAI and Slack tokens are blocked',()=>{
  const faux=[
    'ghp_'+'a'.repeat(36),
    'github_pat_'+'b'.repeat(75),
    'AKIA'+'A'.repeat(16),
    'sk-proj-'+'c'.repeat(40),
    'xoxb-'+'d'.repeat(24)
  ];
  for(const token of faux)
    assert.ok(codes(sample('let credential="'+token+'";')).length>0);
  assert.deepEqual(codes(sample('const token=process.env.GITHUB_TOKEN;')),[]);
});
test('realistic private PEM key blocks are rejected, labels alone are not',()=>{
  const begin='-----BEGIN PRIVATE KEY-----';
  const end='-----END PRIVATE KEY-----';
  assert.ok(codes(sample([begin,'A'.repeat(160),end].join('\n')))
    .includes('PRIVATE_KEY_BLOCK'));
  assert.deepEqual(codes(sample(begin+' (example only)')),[]);
});
test('privileged untrusted workflow triggers and write-all are blocked',()=>{
  assert.ok(codes(sample('on:\n  pull_request_target:\n',' .github/workflows/a.yml'.trim()))
    .includes('UNTRUSTED_PRIVILEGED_TRIGGER'));
  assert.ok(codes(sample('on:\n  workflow_run:\n','.github/workflows/a.yml'))
    .includes('UNTRUSTED_PRIVILEGED_TRIGGER'));
  assert.ok(codes(sample('permissions: write-all','.github/workflows/a.yml'))
    .includes('WORKFLOW_WRITE_ALL'));
  assert.deepEqual(codes(sample('on:\n  pull_request:\npermissions:\n  contents: read','.github/workflows/a.yml')),[]);
});
test('unreviewed curl-to-shell workflow commands are blocked',()=>{
  assert.ok(codes(inspectWorkflow('.github/workflows/check.yml',
    '  - name: install\n    run: curl https://example.invalid/setup.sh | bash\n'))
    .includes('UNVERIFIED_REMOTE_SHELL'));
});
test('mutable action references are advisory and SHA pins are accepted',()=>{
  const mutable=inspectWorkflow('.github/workflows/check.yml',
    '- uses: actions/checkout@v4\n');
  assert.deepEqual(codes(mutable),[]);
  assert.ok(mutable.some(x=>x.level==='warning'&&x.code==='MUTABLE_ACTION_REFERENCE'));
  const pinned=inspectWorkflow('.github/workflows/check.yml',
    '- uses: actions/checkout@'+'a'.repeat(40)+'\n');
  assert.deepEqual(pinned,[]);
});
test('scanner gives only file names and finding codes, never token values',()=>{
  const token='ghp_'+'q'.repeat(36);
  const findings=sample('const password="'+token+'";');
  assert.ok(findings.some(x=>x.code==='GITHUB_TOKEN'));
  assert.ok(!JSON.stringify(findings).includes(token));
});
test('real tracked repository satisfies blocking security baseline',()=>{
  const result=auditTrackedFiles();
  assert.ok(result.files>20);
  assert.deepEqual(result.errors,[]);
});

test('named action steps are also detected (not only list-item uses)',()=>{
  const w='- name: Setup Node\n  uses: actions/setup-node@v4\n';
  const result=inspectWorkflow('.github/workflows/pipeline.yml',w);
  assert.ok(result.some(x=>x.code==='MUTABLE_ACTION_REFERENCE'));
});
test('multiline shell commands cannot conceal curl-to-bash',()=>{
  const w='- name: Install\n  run: |\n    set -e\n    curl https://example.invalid/setup.sh | bash\n';
  assert.ok(codes(inspectWorkflow('.github/workflows/pipeline.yml',w))
    .includes('UNVERIFIED_REMOTE_SHELL'));
});
