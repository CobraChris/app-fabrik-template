import { readFileSync, appendFileSync, lstatSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

export const REQUEST = 'requests/fahrcockpit-release.json';
export const SOURCE_REF = 'refs/heads/feature/fahrcockpit-blitzer-bridge';

export function validateRequest(r) {
  if (!r || Array.isArray(r) || typeof r !== 'object') throw Error('Not an object');
  const keys = ['expected_version_code','intent','request_id','schema','source_sha'];
  if (Object.keys(r).sort().join(',') !== keys.join(',')) throw Error('Unexpected keys');
  if (r.schema !== 1 || r.intent !== 'fahrcockpit-signed-beta' ||
      typeof r.request_id !== 'string' ||
      !/^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]{3,60}$/.test(r.request_id))
    throw Error('Unexpected request identity');
  if (typeof r.source_sha !== 'string' || !/^[a-f0-9]{40}$/.test(r.source_sha))
    throw Error('Invalid source SHA');
  if (!Number.isSafeInteger(r.expected_version_code) ||
      r.expected_version_code < 1 || r.expected_version_code > 2100000000)
    throw Error('Invalid version');
  return { sha:r.source_sha, code:r.expected_version_code };
}

export function validateEvent(env) {
  if (env.GITHUB_REPOSITORY !== 'CobraChris/app-fabrik-template' ||
      env.GITHUB_ACTOR !== 'CobraChris' || env.GITHUB_REF !== 'refs/heads/main' ||
      env.GITHUB_EVENT_NAME !== 'push') throw Error('Unauthorized release');
}
export function validatePush(before, after, changed) {
  if (!/^[a-f0-9]{40}$/.test(before) || !/^[a-f0-9]{40}$/.test(after) ||
      changed.trim() !== REQUEST) throw Error('Untrusted push content');
}
export function validateSource(sha, remote) {
  if (remote.trim() !== sha + '\t' + SOURCE_REF) throw Error('Source HEAD changed');
}

function run() {
  validateEvent(process.env);
  const stat=lstatSync(REQUEST);
  if (!stat.isFile() || stat.isSymbolicLink()) throw Error('Unsafe request file');
  const {sha,code}=validateRequest(JSON.parse(readFileSync(REQUEST,'utf8')));
  validatePush(process.env.REQUEST_BEFORE||'',process.env.GITHUB_SHA||'',
      execFileSync('git',['diff','--name-only',process.env.REQUEST_BEFORE,
          process.env.GITHUB_SHA],{encoding:'utf8',timeout:10000}));
  validateSource(sha,execFileSync('git',['ls-remote','origin',SOURCE_REF],
      {encoding:'utf8',timeout:20000}));
  if (!process.env.GITHUB_OUTPUT) throw Error('Missing output');
  appendFileSync(process.env.GITHUB_OUTPUT,'sha='+sha+'\ncode='+code+'\n');
  console.log('FahrCockpit source approved '+sha.slice(0,12));
}
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
  try { run(); } catch(e) {
    console.error('FahrCockpit release blocked: '+e.message);
    process.exitCode=1;
  }
