import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// npm test builds dist/ first. The existing CI workflow runs npm test for every PR.
const appsRoot = fileURLToPath(new URL('../dist/apps/', import.meta.url));

async function appDirs() {
  const entries = await readdir(appsRoot, { withFileTypes: true });
  return entries.filter(entry => entry.isDirectory()).map(entry => ({
    slug: entry.name,
    dir: path.join(appsRoot, entry.name)
  }));
}

async function checkAsset(app, from, reference) {
  const filename = reference.split(/[?#]/, 1)[0];
  assert.ok(filename.startsWith('./') && !filename.includes('\\'),
    app.slug + ': ungültiger lokaler Dateiverweis in ' + from + ': ' + reference);
  const base = path.join(app.dir, from);
  const destination = path.resolve(path.dirname(base), filename);
  assert.ok(destination.startsWith(app.dir + path.sep),
    app.slug + ': Dateiverweis verlässt die Unter-App in ' + from + ': ' + reference);
  const info = await stat(destination).catch(() => null);
  assert.ok(info?.isFile(), app.slug + ': fehlende Datei in ' + from + ': ' + reference);
}

async function walkFiles(dir, prefix = '') {
  const files = [];
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const relative = path.posix.join(prefix, entry.name);
    if (entry.isDirectory()) files.push(...await walkFiles(path.join(dir, entry.name), relative));
    else if (entry.isFile()) files.push(relative);
  }
  return files;
}

// Every new apps/<slug>/ app is included automatically; no hard-coded app names.
test('Unter-Apps: alle lokalen HTML-Assets und Manifest-Dateien existieren', async () => {
  const apps = await appDirs();
  assert.ok(apps.length > 0, 'Keine Unter-App im Build gefunden');
  const catalog = await readFile(path.join(appsRoot, 'index.html'), 'utf8');
  for (const app of apps) {
    const html = await readFile(path.join(app.dir, 'index.html'), 'utf8');
    const manifest = JSON.parse(await readFile(path.join(app.dir, 'manifest.webmanifest'), 'utf8'));
    assert.match(catalog, new RegExp('href="\\./' + app.slug + '/"'),
      app.slug + ': fehlt im App-Katalog');
    assert.ok(html.includes('href="./manifest.webmanifest"'),
      app.slug + ': eigenes Manifest nicht eingebunden');
    assert.equal(manifest.id, './', app.slug + ': id muss ./ sein');
    assert.equal(manifest.scope, './', app.slug + ': scope muss ./ sein');
    assert.equal(manifest.start_url, './', app.slug + ': start_url muss ./ sein');
    for (const icon of manifest.icons) await checkAsset(app, 'manifest.webmanifest', icon.src);

    // Check load-bearing links, not navigation such as an ordinary anchor.
    const refs = [...html.matchAll(/<(?:script|link|img|source|audio|video)\b[^>]*>/gi)]
      .flatMap(tag => [...tag[0].matchAll(/\b(?:src|href|poster)\s*=\s*["']([^"']+)["']/gi)]
        .map(match => match[1]))
      .filter(ref => ref.startsWith('.'));
    for (const ref of refs) await checkAsset(app, 'index.html', ref);
  }
});

test('Unter-Apps: JavaScript-Module und lokale CSS-Dateien sind vollständig', async () => {
  for (const app of await appDirs()) {
    for (const relative of await walkFiles(app.dir)) {
      if (/\.(?:js|mjs)$/i.test(relative)) {
        const source = await readFile(path.join(app.dir, relative), 'utf8');
        const refs = [
          ...source.matchAll(/\b(?:import|export)\s+(?:[^'"]+?\s+from\s*)?["'](\.[^"']+)["']/g),
          ...source.matchAll(/\bimport\s*\(\s*["'](\.[^"']+)["']\s*\)/g)
        ].map(match => match[1]);
        for (const ref of refs) await checkAsset(app, relative, ref);
      }
      if (/\.css$/i.test(relative)) {
        const source = await readFile(path.join(app.dir, relative), 'utf8');
        const refs = [...source.matchAll(/\burl\(\s*["']?(\.[^\s)'"]+)["']?\s*\)/g)]
          .map(match => match[1]);
        for (const ref of refs) await checkAsset(app, relative, ref);
      }
    }
  }
});
