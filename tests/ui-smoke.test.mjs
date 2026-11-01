import test from 'node:test';
import assert from 'node:assert/strict';
import { access, readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dist = fileURLToPath(new URL('../dist/', import.meta.url));
async function content(relativePath) {
  return readFile(path.join(dist, relativePath), 'utf8');
}
function localFile(reference) {
  if (typeof reference !== 'string' || !reference.startsWith('./') ||
      reference.includes('?') || reference.includes('#')) {
    throw new Error('Unerwarteter lokaler Dateipfad: ' + reference);
  }
  const full = path.resolve(dist, reference);
  assert.ok(full === dist || full.startsWith(dist + path.sep), 'Datei außerhalb von dist/: ' + reference);
  return full;
}

test('Alle Oberflächenelemente aus app.js existieren im gebauten HTML', async () => {
  const html = await content('index.html');
  const js = await content('app.js');
  const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
  assert.equal(ids.length, new Set(ids).size, 'HTML enthält doppelte IDs');
  const selectors = [
    ...js.matchAll(/\$\(\s*['"]#([A-Za-z][\w-]*)['"]\s*\)/g),
    ...js.matchAll(/document\.getElementById\(\s*['"]([A-Za-z][\w-]*)['"]\s*\)/g)
  ].map(match => match[1]);
  assert.ok(selectors.length >= 35, 'Unerwartet wenige geprüfte UI-Elemente');
  assert.deepEqual([...new Set(selectors)].filter(id => !ids.includes(id)), [],
    'JavaScript verwendet nicht vorhandene HTML-Elemente');
});

test('Schnellnavigation und wichtige Aktionen verweisen auf vorhandene HTML-IDs', async () => {
  const html = await content('index.html');
  const ids = new Set([...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]));
  for (const id of ['quickstart-action', 'repo-import-panel', 'repo-import-search',
    'project-form', 'project-list', 'focus-list', 'idea-form', 'idea-list',
    'project-export', 'project-import']) {
    assert.ok(ids.has(id), 'Pflichtelement fehlt: ' + id);
  }
  const nav = [...html.matchAll(/href="#([A-Za-z][\w-]*)"/g)].map(match => match[1]);
  assert.ok(nav.length >= 3, 'Schnellnavigation fehlt');
  assert.deepEqual(nav.filter(id => !ids.has(id)), [], 'Sprungmarke existiert nicht');
});

test('Relative JavaScript-Importe sind im veröffentlichten Paket enthalten', async () => {
  const scripts = (await readdir(dist)).filter(file => /\.(?:js|mjs)$/.test(file));
  assert.ok(scripts.includes('app.js'));
  for (const script of scripts) {
    const source = await content(script);
    const refs = [...source.matchAll(/\b(?:import|export)\s+(?:[^'"]+?\s+from\s*)?['"](\.[^'"]+)['"]/g)]
      .map(match => match[1]);
    for (const ref of refs) {
      const destination = path.resolve(dist, path.dirname(script), ref);
      assert.ok(destination.startsWith(dist + path.sep), script + ': Import verlässt dist/');
      await access(destination);
    }
  }
});

test('Offline-Service-Worker referenziert nur vorhandene Dateien', async () => {
  const worker = await content('sw.js');
  const match = worker.match(/const ASSETS = \[([\s\S]*?)\];/);
  assert.ok(match, 'Offline-Dateiliste fehlt');
  const refs = [...match[1].matchAll(/['"]([^'"]+)['"]/g)].map(item => item[1]);
  assert.ok(refs.includes('./index.html'));
  assert.ok(refs.includes('./app.js'));
  assert.ok(refs.includes('./quickstart.mjs'));
  assert.equal(refs.length, new Set(refs).size, 'Doppelte Offline-Datei');
  for (const ref of refs) {
    const info = await stat(localFile(ref));
    if (ref === './') assert.ok(info.isDirectory());
    else assert.ok(info.isFile(), 'Offline-Datei fehlt: ' + ref);
  }
});

test('Stylesheet, Manifest, PWA-Icon und App-Einstieg sind verfügbar', async () => {
  const html = await content('index.html');
  for (const ref of ['./styles.css', './manifest.webmanifest', './icons/app.svg', './app.js']) {
    assert.ok(html.includes('"' + ref + '"'), 'HTML-Verweis fehlt: ' + ref);
    await access(localFile(ref));
  }
  const manifest = JSON.parse(await content('manifest.webmanifest'));
  assert.equal(manifest.display, 'standalone');
  assert.ok(manifest.name && manifest.short_name);
  assert.ok(manifest.icons.length >= 1);
  for (const icon of manifest.icons) assert.ok((await stat(localFile(icon.src))).isFile());
});
