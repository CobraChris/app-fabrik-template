/**
 * Strict GitOps request validation. Never executes request contents as code.
 * The request file is a non-secret intent document in the PRIVATE repository.
 */
import { readFileSync, appendFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { targetRepository } from './provision-core.mjs';

export const REQUEST_PATH = 'requests/provision.json';

export function allowedRepositories(raw) {
  if (!Array.isArray(raw) || raw.length===0 || raw.length>30 ||
      raw.some(x=>typeof x!=='string' || targetRepository(x)!==x) ||
      new Set(raw).size!==raw.length)
    throw new TypeError('Invalid approved target-repository allowlist');
  if(raw.includes('CobraChris/github-app-fabrik-control'))
    throw new TypeError('Controller must never provision itself');
  return new Set(raw);
}

export function validateRequest(data, allowlist) {
  if(data===null || typeof data!=='object' || Array.isArray(data))
    throw new TypeError('Invalid signing request');
  const expected=['intent','request_id','schema','target_repository'];
  if(Object.keys(data).sort().join(',')!==expected.join(','))
    throw new TypeError('Signing request schema mismatch');
  if(data.schema!==1 || data.intent!=='create-app-signing-secrets' ||
      typeof data.request_id!=='string' ||
      !/^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]{3,60}$/.test(data.request_id))
    throw new TypeError('Invalid signing request identity');
  const repo=targetRepository(data.target_repository);
  if(!allowlist.has(repo))throw new Error('Target not pre-approved for signing');
  return repo;
}

export function approveManualRequest(repo, allowlist) {
  const target=targetRepository(repo);
  if(!allowlist.has(target))throw new Error('Target not pre-approved for signing');
  return target;
}

function run() {
  const allowlist=allowedRepositories(JSON.parse(
      readFileSync('config/approved-targets.json','utf8')));
  const event=process.env.EVENT_NAME;
  let target;
  if(event==='push'){
    const before=process.env.PUSH_BEFORE||'';
    const sha=process.env.GITHUB_SHA||'';
    if(!/^[a-f0-9]{40}$/.test(before) || !/^[a-f0-9]{40}$/.test(sha))
      throw new Error('Unverifiable push revision');
    const changed=execFileSync('git',[
      'diff','--name-only',before,sha,'--',REQUEST_PATH
    ],{encoding:'utf8',timeout:10000}).trim();
    if(changed!==REQUEST_PATH)
      throw new Error('Signing request was not changed in triggering push');
    const payload=JSON.parse(readFileSync(REQUEST_PATH,'utf8'));
    target=validateRequest(payload,allowlist);
  } else if(event==='workflow_dispatch') {
    target=approveManualRequest(process.env.MANUAL_TARGET,allowlist);
  } else {
    throw new Error('Untrusted triggering event');
  }
  const output=process.env.GITHUB_OUTPUT;
  if(typeof output!=='string'||!output)throw new Error('Missing GitHub step output');
  appendFileSync(output,'target_repository='+target+'\n');
  console.log('Approved signing request for '+target);
}
if(process.argv[1] && fileURLToPath(import.meta.url)===process.argv[1])
  try {run()} catch(err) {
    console.error('Signing request rejected: '+err.message);
    process.exitCode=1;
  }
