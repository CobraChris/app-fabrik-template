import { execFileSync } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

export const OWNER = 'CobraChris';
export const AUTOMATION_REPO = OWNER + '/app-fabrik-template';
const API = 'https://api.github.com/user/repos';

function exactKeys(value, expected, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(label + ' muss ein JSON-Objekt sein.');
  }
  const actual = Object.keys(value).sort();
  const allowed = [...expected].sort();
  if (actual.length !== allowed.length ||
      actual.some((key, index) => key !== allowed[index])) {
    throw new Error(label + ': unbekannte oder fehlende Felder.');
  }
}

export function validateRepositoryRequest(payload) {
  exactKeys(payload, ['schemaVersion', 'owner', 'name', 'visibility', 'description', 'confirmation'], 'Auftrag');
  if (payload.schemaVersion !== 1 || payload.owner !== OWNER) {
    throw new Error('Ungültiges Schema oder falscher Repository-Eigentümer.');
  }
  const name = payload.name;
  if (typeof name !== 'string' || name.length > 100 ||
      !/^[A-Za-z0-9](?:[A-Za-z0-9._-]{0,98}[A-Za-z0-9])?$/.test(name) ||
      name.includes('..') || name.toLowerCase().endsWith('.git')) {
    throw new Error('Ungültiger Repository-Name.');
  }
  if (payload.visibility !== 'private' && payload.visibility !== 'public') {
    throw new Error('Sichtbarkeit muss ausdrücklich public oder private sein.');
  }
  const expectedConfirmation = payload.visibility === 'public' ? 'CREATE_PUBLIC' : 'CREATE_PRIVATE';
  if (payload.confirmation !== expectedConfirmation) {
    throw new Error('Bestätigung passt nicht zur Repository-Sichtbarkeit.');
  }
  if (typeof payload.description !== 'string' || payload.description.length > 160 ||
      /[\u0000-\u001f\u007f]/.test(payload.description)) {
    throw new Error('Beschreibung ist ungültig.');
  }
  return Object.freeze({
    name,
    visibility: payload.visibility,
    description: payload.description.trim(),
  });
}

export function selectAddedRequestPaths(diffText) {
  if (typeof diffText !== 'string' || diffText.length > 20000) {
    throw new Error('Ungültige Git-Differenz.');
  }
  const paths = diffText.trim() ? diffText.trim().split('\n') : [];
  for (const filename of paths) {
    if (!/^repo-requests\/[A-Za-z0-9][A-Za-z0-9._-]{0,78}\.json$/.test(filename) ||
        filename.includes('..') || filename.includes('\\')) {
      throw new Error('Unerwarteter Pfad für Repository-Auftrag.');
    }
  }
  if (paths.length > 1) {
    throw new Error('Nur ein neuer Repository-Auftrag pro Commit/Push zulässig.');
  }
  return paths;
}

export function selectRequestFromGit({ before, after, repository, ref }, diffCommand) {
  if (repository !== AUTOMATION_REPO || ref !== 'refs/heads/main') {
    throw new Error('Die Automatisierung darf nur im Hauptzweig der App-Fabrik laufen.');
  }
  if (!/^[0-9a-f]{40}$/i.test(before ?? '') || !/^[0-9a-f]{40}$/i.test(after ?? '') ||
      /^0{40}$/.test(before) || /^0{40}$/.test(after)) {
    throw new Error('Ungültige oder fehlende Git-Versionskennung.');
  }
  const runDiff = diffCommand ?? ((args) => execFileSync('git', args, {
    encoding: 'utf8', maxBuffer: 20000, timeout: 20000
  }));
  const files = runDiff(['diff', '--diff-filter=A', '--name-only', before, after, '--', 'repo-requests/']);
  return selectAddedRequestPaths(files)[0] ?? null;
}

export async function createRepository(request, token, fetchImpl = fetch) {
  const verified = validateRepositoryRequest(request);
  if (typeof token !== 'string' || !token.trim()) {
    throw new Error('GitHub-Secret REPO_CREATOR_TOKEN ist nicht hinterlegt.');
  }

  const response = await fetchImpl(API, {
    method: 'POST',
    redirect: 'error',
    headers: {
      'Accept': 'application/vnd.github+json',
      'Authorization': 'Bearer ' + token,
      'X-GitHub-Api-Version': '2022-11-28',
      'Content-Type': 'application/json',
      'User-Agent': 'app-fabrik-repository-creator',
    },
    body: JSON.stringify({
      name: verified.name,
      description: verified.description,
      private: verified.visibility === 'private',
      auto_init: true,
    }),
    signal: AbortSignal.timeout(20000),
  });

  if (response.status !== 201) {
    throw new Error('GitHub hat die Repository-Erstellung abgelehnt (HTTP ' + response.status + ').');
  }
  const result = await response.json();
  if (!result || result.full_name?.toLowerCase() !== (OWNER + '/' + verified.name).toLowerCase() ||
      result.private !== (verified.visibility === 'private')) {
    throw new Error('GitHub-Antwort stimmt nicht mit dem freigegebenen Auftrag überein.');
  }
  return { name: result.full_name, url: 'https://github.com/' + OWNER + '/' + encodeURIComponent(verified.name) };
}

async function main() {
  const selected = selectRequestFromGit({
    before: process.env.GITHUB_BEFORE,
    after: process.env.GITHUB_SHA,
    repository: process.env.GITHUB_REPOSITORY,
    ref: process.env.GITHUB_REF,
  });
  if (!selected) {
    console.log('Keine neue JSON-Auftragsdatei: keine Aktion.');
    return;
  }
  const root = fileURLToPath(new URL('../', import.meta.url));
  const json = await readFile(path.join(root, selected), 'utf8');
  if (json.length > 4096) throw new Error('Auftragsdatei ist zu groß.');
  const request = JSON.parse(json);
  const result = await createRepository(request, process.env.REPO_CREATOR_TOKEN);
  console.log('GitHub-Repository angelegt: ' + result.url);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : 'Repository-Auftrag fehlgeschlagen.');
    process.exitCode = 1;
  });
}
