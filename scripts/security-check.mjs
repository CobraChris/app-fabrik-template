/**
 * Repository baseline security audit.
 * Read-only, no network, no external dependencies, no secret values in logs.
 * Run on every pull request through npm test.
 */
import {execFileSync} from 'node:child_process';
import {lstatSync,readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const ROOT=fileURLToPath(new URL('../',import.meta.url));
const SIZE_LIMIT=2*1024*1024;
const SENSITIVE_FILE=/(^|\/)(?:\.env(?:\.(?!example$|sample$|template$)[^/]+)?|[^/]+\.(?:pem|p12|pfx|jks|keystore|key))$/i;
const SECRETS=[
  ['GITHUB_TOKEN',/\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36,}\b/g],
  ['GITHUB_FINE_GRAINED_TOKEN',/\bgithub_pat_[A-Za-z0-9_]{60,}\b/g],
  ['AWS_ACCESS_KEY',/\b(?:AKIA|ASIA)[A-Z0-9]{16}\b/g],
  ['OPENAI_API_KEY',/\bsk-(?:proj-)?[A-Za-z0-9_-]{30,}\b/g],
  ['SLACK_TOKEN',/\bxox[baprs]-[A-Za-z0-9-]{20,}\b/g],
  ['PRIVATE_KEY_BLOCK',/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\s\S]{80,}?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/g]
];

function finding(file,code,level='error') {return {file,code,level};}

export function inspectFile(file,raw) {
  if(typeof file!=='string'||!file||file.startsWith('/')||file.includes('\\')||
     file.split('/').some(x=>x==='..'||x==='.'))
    throw new Error('Unsafe tracked filename');
  const issues=[];
  if(SENSITIVE_FILE.test(file)) issues.push(finding(file,'PRIVATE_MATERIAL_FILE'));
  if(!Buffer.isBuffer(raw))raw=Buffer.from(String(raw));
  if(raw.includes(0))return issues; // Binary assets are checked by name, not decoded.
  if(raw.length>SIZE_LIMIT) {
    issues.push(finding(file,'TEXT_FILE_TOO_LARGE'));
    return issues;
  }
  const text=raw.toString('utf8');
  for(const [code,pattern] of SECRETS) {
    pattern.lastIndex=0;
    if(pattern.test(text))issues.push(finding(file,code));
  }
  if(/^\.github\/workflows\/[^/]+\.ya?ml$/.test(file))
    issues.push(...inspectWorkflow(file,text));
  return issues;
}

export function inspectWorkflow(file,text) {
  if(!/^\.github\/workflows\/[^/]+\.ya?ml$/.test(file))
    throw new Error('Expected workflow path');
  const issues=[];
  // These events can execute attacker-controlled code with privileged context.
  if(/^\s*(?:pull_request_target|workflow_run):\s*(?:#.*)?$/m.test(text))
    issues.push(finding(file,'UNTRUSTED_PRIVILEGED_TRIGGER'));
  if(/^\s*permissions:\s*write-all\s*(?:#.*)?$/m.test(text))
    issues.push(finding(file,'WORKFLOW_WRITE_ALL'));
  if(/^\s*run:\s*(?:curl|wget)\b[^\n]*\|\s*(?:sudo\s+)?(?:ba)?sh\b/m.test(text))
    issues.push(finding(file,'UNVERIFIED_REMOTE_SHELL'));
  // Advisory until current existing workflows are migrated to immutable SHAs.
  const actions=[...text.matchAll(/^\s*-\s*uses:\s*([^\s#]+)/gm)];
  if(actions.some(([,value])=>
    !value.startsWith('./') && !value.startsWith('docker://') &&
    !/@[0-9a-f]{40}$/i.test(value)))
    issues.push(finding(file,'MUTABLE_ACTION_REFERENCE','warning'));
  return issues;
}

export function auditTrackedFiles(root=ROOT) {
  const output=execFileSync('git',['ls-files','-z'],{
    cwd:root,encoding:'buffer',timeout:15000,maxBuffer:5*1024*1024
  });
  const files=output.toString('utf8').split('\0').filter(Boolean);
  if(files.length===0)throw new Error('No tracked files found');
  const issues=[];
  for(const file of files) {
    const full=path.resolve(root,file);
    if(!full.startsWith(root+path.sep))
      throw new Error('Tracked path escapes repository');
    const stat=lstatSync(full);
    if(stat.isSymbolicLink()){
      issues.push(finding(file,'TRACKED_SYMLINK'));
      continue;
    }
    if(!stat.isFile())continue;
    // Only read source-sized files; large media never needs to be printed.
    if(stat.size>SIZE_LIMIT){
      if(SENSITIVE_FILE.test(file))issues.push(finding(file,'PRIVATE_MATERIAL_FILE'));
      continue;
    }
    issues.push(...inspectFile(file,readFileSync(full)));
  }
  return {files:files.length,errors:issues.filter(x=>x.level==='error'),
    warnings:issues.filter(x=>x.level==='warning')};
}

function main() {
  const result=auditTrackedFiles();
  for(const issue of result.errors)
    console.error('Sicherheitspruefung: '+issue.file+' ['+issue.code+']');
  for(const issue of result.warnings.slice(0,12))
    console.warn('Hinweis: '+issue.file+' ['+issue.code+']');
  if(result.warnings.length>12)
    console.warn('Weitere Hinweise: '+(result.warnings.length-12));
  console.log('Sicherheitsaudit: '+result.files+' Dateien, '+
    result.errors.length+' Sperren, '+result.warnings.length+' Hinweise.');
  if(result.errors.length)process.exitCode=1;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
  try{main()}catch(error){
    console.error('Sicherheitsaudit nicht durchfuehrbar: '+error.message);
    process.exitCode=1;
  }
