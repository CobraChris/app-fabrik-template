import test from 'node:test';
import assert from 'node:assert/strict';
import {
  AUTOMATION_REPO,
  validateRepositoryRequest,
  selectAddedRequestPaths,
  selectRequestFromGit,
  createRepository,
} from '../scripts/create-repository.mjs';

const validPublic = {
  schemaVersion: 1,
  owner: 'CobraChris',
  name: 'CobraChris.github.io',
  visibility: 'public',
  description: 'Android Verified App Links',
  confirmation: 'CREATE_PUBLIC',
};
const validPrivate = {
  ...validPublic,
  name: 'mein-projekt',
  visibility: 'private',
  confirmation: 'CREATE_PRIVATE',
};

test('gültige öffentliche und private Aufträge sind explizit bestätigt', () => {
  assert.equal(validateRepositoryRequest(validPublic).visibility, 'public');
  assert.equal(validateRepositoryRequest(validPrivate).visibility, 'private');
});

test('fehlende oder ungeeignete Sichtbarkeitsbestätigung wird abgelehnt', () => {
  assert.throws(() => validateRepositoryRequest({ ...validPublic, confirmation: 'CREATE_PRIVATE' }), /Bestätigung/);
  assert.throws(() => validateRepositoryRequest({ ...validPrivate, confirmation: 'CREATE_PUBLIC' }), /Bestätigung/);
  assert.throws(() => validateRepositoryRequest({ ...validPrivate, visibility: undefined }), /Sichtbarkeit/);
});

test('nur exakt erlaubte Felder, Nutzer und Repo-Namen sind zulässig', () => {
  assert.throws(() => validateRepositoryRequest({ ...validPublic, owner: 'fremd' }), /Eigentümer/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, name: '../evil' }), /Repository-Name/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, name: '-evil' }), /Repository-Name/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, name: 'a'.repeat(101) }), /Repository-Name/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, name: 'bad..name' }), /Repository-Name/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, token: 'SECRET' }), /Felder/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, description: 'falsch\nnewline' }), /Beschreibung/);
  assert.throws(() => validateRepositoryRequest({ ...validPublic, confirmation: null }), /Bestätigung/);
});

test('nur neu hinzugefügte Auftragsdateien auf main dürfen wirken', () => {
  const config = { before: 'a'.repeat(40), after: 'b'.repeat(40), repository: AUTOMATION_REPO, ref: 'refs/heads/main' };
  const file = selectRequestFromGit(config, (args) => {
    assert.deepEqual(args, ['diff', '--diff-filter=A', '--name-only', config.before, config.after, '--', 'repo-requests/']);
    return 'repo-requests/2026-11-05-pages.json\n';
  });
  assert.equal(file, 'repo-requests/2026-11-05-pages.json');
  assert.equal(selectRequestFromGit(config, () => ''), null);
  assert.throws(() => selectRequestFromGit({ ...config, ref: 'refs/heads/other' }, () => ''), /Hauptzweig/);
  assert.throws(() => selectRequestFromGit({ ...config, repository: 'other/repo' }, () => ''), /Hauptzweig/);
  assert.throws(() => selectRequestFromGit({ ...config, after: 'x' }, () => ''), /Versionskennung/);
  assert.throws(() => selectAddedRequestPaths('repo-requests/a.json\nrepo-requests/b.json\n'), /Nur ein/);
  assert.throws(() => selectAddedRequestPaths('repo-requests/../evil.json\n'), /Pfad/);
  assert.throws(() => selectAddedRequestPaths('.github/workflows/evil.yml\n'), /Pfad/);
});

test('API-Aufruf nutzt ausschließlich POST /user/repos und bestätigte Sichtbarkeit', async () => {
  let calls = 0;
  const fakeFetch = async (url, options) => {
    calls++;
    assert.equal(url, 'https://api.github.com/user/repos');
    assert.equal(options.method, 'POST');
    assert.equal(options.redirect, 'error');
    assert.equal(options.headers.Authorization, 'Bearer secret-test-value');
    const body = JSON.parse(options.body);
    assert.deepEqual(body, { name: validPublic.name, description: validPublic.description, private: false, auto_init: true });
    return { status: 201, json: async () => ({ full_name: 'CobraChris/CobraChris.github.io', private: false }) };
  };
  const r = await createRepository(validPublic, 'secret-test-value', fakeFetch);
  assert.equal(r.url, 'https://github.com/CobraChris/CobraChris.github.io');
  assert.equal(calls, 1);
});

test('ohne Secret oder bei falscher GitHub-Antwort wird keine Erstellung behauptet', async () => {
  await assert.rejects(createRepository(validPublic, '', () => { throw new Error('must not call'); }), /Secret/);
  await assert.rejects(createRepository(validPublic, 'secret', async () => ({ status: 403 })), /HTTP 403/);
  await assert.rejects(createRepository(validPublic, 'secret', async () => ({
    status: 201, json: async () => ({ full_name: 'other/other', private: false })
  })), /nicht mit/);
});

test('private Projekte bleiben bei der API explizit privat', async () => {
  await createRepository(validPrivate, 'secret', async (_, opts) => {
    assert.equal(JSON.parse(opts.body).private, true);
    return { status: 201, json: async () => ({ full_name: 'CobraChris/mein-projekt', private: true }) };
  });
});
