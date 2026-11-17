import { readFileSync, appendFileSync, lstatSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

export const REQUEST = 'requests/fahrcockpit-release.json';
export const SOURCE_REF = 'refs/heads/feature/fahrcockpit-blitzer-bridge';
export const GOOGLE_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v21-google-first-fallback';
export const GOOGLE_V22_SOURCE_REF =
    'refs/heads/fix/fahrcockpit-v22-keyboard-insets-layout';
export const GOOGLE_V23_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v23-destinations-places-ui';
// Independently reviewed and successfully tested Google-only v23 candidate.
// Any source change requires a new separate security review and release approval.
export const GOOGLE_V23_APPROVED_SHA = '151c382a6afad6890500c8be5d6b8bf2cb8f3f1a';
export const GOOGLE_V24_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v24-compact-navigation-ui';
// Explicit security review, pinned source: all changes require new review.
export const GOOGLE_V24_APPROVED_SHA = '61c73d12778f42d7481eef8a5eeabe7f2bf477a3';
export const GOOGLE_V25_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe';
// Exact reviewed source only; any post-CI edit needs a new review/approval.
export const GOOGLE_V25_APPROVED_SHA = 'e915f6dfdb49af1a2ac3d77219e723716a82bbed';
export const GOOGLE_V26_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe';
// Security-reviewed v26 source, exact commit after PR #88; no floating ref permitted.
export const GOOGLE_V26_APPROVED_SHA = '10d490addbdf1dbbb9933b58ead8faf3599eab2b';
export const GOOGLE_V27_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe';
// Exact reviewed compact UX source; further changes need separate approval.
export const GOOGLE_V27_APPROVED_SHA = 'c0b89ecc7ae7a0c9c68d1d9270ee79c97c23c913';
export const GOOGLE_V28_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe';
// Draft placeholder: must be updated to exact source-merge SHA after PR #97.
export const GOOGLE_V28_APPROVED_SHA = '950438faf31298dfabb94cb6b79fc50ca88825f3';
export const GOOGLE_V29_SOURCE_REF =
    'refs/heads/feature/fahrcockpit-v25-saphe-notification-probe';
// Exact reviewed v29 quick-fuel and GPS fallback source; no floating refs.
export const GOOGLE_V29_APPROVED_SHA = '88d7cd8b60dea038505c20ef4b61130dbaee3e56';

export function validateRequest(r) {
  if (!r || Array.isArray(r) || typeof r !== 'object') throw Error('Not an object');
  const legacy = r.schema === 1;
  const integrated = r.schema === 2;
  const keys = integrated
    ? ['expected_version_code','intent','request_id','schema','source_sha','variant']
    : ['expected_version_code','intent','request_id','schema','source_sha'];
  if ((!legacy && !integrated) ||
      Object.keys(r).sort().join(',') !== keys.join(','))
    throw Error('Unexpected release request keys');
  if (r.intent !== 'fahrcockpit-signed-beta' ||
      typeof r.request_id !== 'string' ||
      !/^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]{3,60}$/.test(r.request_id))
    throw Error('Unexpected request identity');
  if (typeof r.source_sha !== 'string' || !/^[a-f0-9]{40}$/.test(r.source_sha))
    throw Error('Invalid source SHA');
  if (!Number.isSafeInteger(r.expected_version_code) ||
      r.expected_version_code < 1 || r.expected_version_code > 2100000000)
    throw Error('Invalid version');
  if (legacy) {
    if (r.expected_version_code > 20)
      throw Error('Versions 21+ require explicit Google-only release schema');
    return { sha:r.source_sha, code:r.expected_version_code };
  }
  if (r.variant !== 'google-sdk-only' ||
      r.expected_version_code < 21 || r.expected_version_code > 29)
    throw Error('Google-only release must explicitly exclude OSM and preview; v23+ requires new review');
  if (r.expected_version_code === 22 &&
      r.request_id !== '2026-11-13-fahrcockpit-v22-google-only')
    throw Error('Unapproved v22 release request identity');
  if (r.expected_version_code === 23 &&
      (r.request_id !== '2026-11-14-fahrcockpit-v23-google-only' ||
       r.source_sha !== GOOGLE_V23_APPROVED_SHA))
    throw Error('Unapproved v23 release identity or reviewed source SHA');
  if (r.expected_version_code === 24 &&
      (r.request_id !== '2026-11-15-fahrcockpit-v24-google-only' ||
       r.source_sha !== GOOGLE_V24_APPROVED_SHA))
    throw Error('Unapproved v24 release identity or reviewed source SHA');
  if (r.expected_version_code === 25 &&
      (r.request_id !== '2026-11-16-fahrcockpit-v25-google-only' ||
       r.source_sha !== GOOGLE_V25_APPROVED_SHA))
    throw Error('Unapproved v25 release identity or reviewed source SHA');
  if (r.expected_version_code === 26 &&
      (r.request_id !== '2026-11-16-fahrcockpit-v26-unified-favorites' ||
       r.source_sha !== GOOGLE_V26_APPROVED_SHA))
    throw Error('Unapproved v26 release identity or reviewed source SHA');
  if (r.expected_version_code === 27 &&
      (r.request_id !== '2026-11-17-fahrcockpit-v27-compact-ux' ||
       r.source_sha !== GOOGLE_V27_APPROVED_SHA))
    throw Error('Unapproved v27 release identity or reviewed source SHA');
  if (r.expected_version_code === 28 &&
      (r.request_id !== '2026-11-17-fahrcockpit-v28-fuel-map-prices' ||
       r.source_sha !== GOOGLE_V28_APPROVED_SHA))
    throw Error('Unapproved v28 release identity or reviewed source SHA');
  if (r.expected_version_code === 29 &&
      (r.request_id !== '2026-11-17-fahrcockpit-v29-quick-fuel-controls' ||
       r.source_sha !== GOOGLE_V29_APPROVED_SHA))
    throw Error('Unapproved v29 release identity or reviewed source SHA');
  return { sha:r.source_sha, code:r.expected_version_code,
    variant:r.variant };
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
export function sourceRefFor(variant, code = 21) {
  if (variant !== 'google-sdk-only') return SOURCE_REF;
  if (code === 21) return GOOGLE_SOURCE_REF;
  if (code === 22) return GOOGLE_V22_SOURCE_REF;
  if (code === 23) return GOOGLE_V23_SOURCE_REF;
  if (code === 24) return GOOGLE_V24_SOURCE_REF;
  if (code === 25) return GOOGLE_V25_SOURCE_REF;
  if (code === 26) return GOOGLE_V26_SOURCE_REF;
  if (code === 27) return GOOGLE_V27_SOURCE_REF;
  if (code === 28) return GOOGLE_V28_SOURCE_REF;
  if (code === 29) return GOOGLE_V29_SOURCE_REF;
  throw Error('Unapproved Google-only release version');
}
export function validateSource(sha, remote, variant = 'legacy', code = 21) {
  const ref = sourceRefFor(variant, code);
  if (remote.trim() !== sha + '\t' + ref) throw Error('Source HEAD changed');
}

function run() {
  validateEvent(process.env);
  const stat=lstatSync(REQUEST);
  if (!stat.isFile() || stat.isSymbolicLink()) throw Error('Unsafe request file');
  const {sha,code,variant}=validateRequest(JSON.parse(readFileSync(REQUEST,'utf8')));
  validatePush(process.env.REQUEST_BEFORE||'',process.env.GITHUB_SHA||'',
      execFileSync('git',['diff','--name-only',process.env.REQUEST_BEFORE,
          process.env.GITHUB_SHA],{encoding:'utf8',timeout:10000}));
  const verifiedBranch = sourceRefFor(variant,code);
  validateSource(sha,execFileSync('git',['ls-remote','origin',verifiedBranch],
      {encoding:'utf8',timeout:20000}),variant,code);
  if (!process.env.GITHUB_OUTPUT) throw Error('Missing output');
  appendFileSync(process.env.GITHUB_OUTPUT,'sha='+sha+'\ncode='+code
    +'\nvariant='+(variant || 'legacy')+'\n');
  console.log('FahrCockpit source approved '+sha.slice(0,12));
}
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
  try { run(); } catch(e) {
    console.error('FahrCockpit release blocked: '+e.message);
    process.exitCode=1;
  }
