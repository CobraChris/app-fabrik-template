/**
 * Sanftwecker release requests, never signing secrets.
 * A commit on the reviewed main branch controls a pinned source revision.
 */
import {readFileSync,appendFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

export const REQUEST_PATH='requests/sanftwecker-release.json';
export const PROJECT='experiments/watch-vibration-poc';
export const SOURCE_REF='refs/heads/feature/watch-vibration-poc';

export function validateReleaseRequest(value){
  if(!value||typeof value!=='object'||Array.isArray(value))
    throw new TypeError('Invalid release request');
  const keys=['expected_version_code','intent','request_id','schema','source_sha'];
  if(Object.keys(value).sort().join(',')!==keys.join(','))
    throw new TypeError('Release request schema mismatch');
  if(value.schema!==1||value.intent!=='sanftwecker-signed-beta'||
      typeof value.request_id!=='string'||
      !/^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]{3,60}$/.test(value.request_id))
    throw new TypeError('Invalid release request intent/identity');
  if(typeof value.source_sha!=='string'||!/^[a-f0-9]{40}$/.test(value.source_sha))
    throw new TypeError('Invalid pinned commit SHA');
  if(!Number.isSafeInteger(value.expected_version_code)||
      value.expected_version_code<1||value.expected_version_code>2100000000)
    throw new TypeError('Invalid Android versionCode');
  return {source_sha:value.source_sha,version_code:value.expected_version_code};
}
export function assertExactSourceHead(wanted,lsRemote){
  if(typeof lsRemote!=='string')throw new Error('Source head unavailable');
  const pieces=lsRemote.trim().split(/\s+/);
  if(pieces.length!==2||pieces[1]!==SOURCE_REF||
     pieces[0]!==wanted)
    throw new Error('Pinned source is no longer exact branch HEAD; new review required');
  return wanted;
}
export function assertRequestChanged(before,after,diff){
  if(!/^[a-f0-9]{40}$/.test(before)||!/^[a-f0-9]{40}$/.test(after)||
     diff.trim()!==REQUEST_PATH)
    throw new Error('Push did not change the expected signing request');
}
function run(){
  if(process.env.GITHUB_REPOSITORY!=='CobraChris/app-fabrik-template'||
     process.env.GITHUB_ACTOR!=='CobraChris'||
     process.env.GITHUB_REF!=='refs/heads/main'||
     process.env.GITHUB_EVENT_NAME!=='push')
    throw new Error('Untrusted release source/event');
  const request=validateReleaseRequest(JSON.parse(readFileSync(REQUEST_PATH,'utf8')));
  const before=process.env.REQUEST_BEFORE||'';
  const after=process.env.GITHUB_SHA||'';
  const diff=execFileSync('git',['diff','--name-only',before,after,'--',REQUEST_PATH],
    {encoding:'utf8',timeout:10000});
  assertRequestChanged(before,after,diff);
  const head=execFileSync('git',['ls-remote','origin',SOURCE_REF],
    {encoding:'utf8',timeout:20000});
  assertExactSourceHead(request.source_sha,head);
  if(typeof process.env.GITHUB_OUTPUT!=='string'||!process.env.GITHUB_OUTPUT)
    throw new Error('Missing GitHub Actions output');
  appendFileSync(process.env.GITHUB_OUTPUT,
    'source_sha='+request.source_sha+'\nversion_code='+request.version_code+'\n');
  console.log('Approved pinned Sanftwecker beta revision '+request.source_sha.slice(0,12)+
    ', versionCode '+request.version_code);
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
  try{run();}catch(error){
    console.error('Release request rejected: '+error.message);
    process.exitCode=1;
  }
