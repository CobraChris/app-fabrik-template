import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, stat } from 'node:fs/promises';
import { validAppSlug, validateAppManifest, renderAppCatalog } from '../scripts/build-apps.mjs';
import { parseCount, incrementCount, MAX_COUNT } from '../apps/abnahme/counter.mjs';

const dist = new URL('../dist/', import.meta.url);

test('Nur sichere App-Unterverzeichnisse erlaubt', () => {
  assert.equal(validAppSlug('abnahme'), 'abnahme');
  assert.equal(validAppSlug('fitness-tracker'), 'fitness-tracker');
  for (const name of ['../secret', '..', 'Abnahme', 'a--b', 'bad/path', 'a_b', '', 'a'.repeat(60)]) {
    assert.throws(() => validAppSlug(name));
  }
});

test('App-Manifest ist eigenständig und relativ zur App installiert', () => {
  const correct = {name:'Test',short_name:'T',description:'Beschreibung',id:'./',
    scope:'./',start_url:'./',display:'standalone',icons:[{src:'./icons/app.svg'}]};
  assert.equal(validateAppManifest(correct).name, 'Test');
  for (const bad of [
    {...correct,id:'/'}, {...correct,scope:'../../'}, {...correct,start_url:'../'},
    {...correct,display:'browser'}, {...correct,icons:[]},
    {...correct,icons:[{src:'../../secret'}]},
    {...correct,name:'<'.repeat(100)}
  ]) assert.throws(() => validateAppManifest(bad));
});

test('App-Katalog maskiert Inhalte und erzeugt sichere Links', () => {
  const html = renderAppCatalog([{slug:'abnahme',name:'<script>alert(1)</script>',
    description:'Test & Prüfen'}]);
  assert.match(html, /href="\.\/abnahme\/"/);
  assert.ok(html.includes('&lt;script&gt;'));
  assert.ok(html.includes('Test &amp; Prüfen'));
  assert.ok(!html.includes('<script>alert(1)</script>'));
});

test('Build liefert App-Fabrik und vollständige Demo-App aus', async () => {
  for (const name of ['index.html', 'manifest.webmanifest', 'sw.js', 'counter.mjs',
    'app.js', 'styles.css', 'icons/app.svg']) {
    const file = new URL('./apps/abnahme/' + name, dist);
    assert.ok((await stat(file)).isFile(), 'Fehlende App-Datei: ' + name);
  }
  const catalog = await readFile(new URL('./apps/index.html', dist), 'utf8');
  assert.match(catalog, /href="\.\/abnahme\/"/);
  assert.match(catalog, /App-Fabrik-Abnahme/);
  const root = await readFile(new URL('./index.html', dist), 'utf8');
  assert.match(root, /href="\.\/apps\/"/);
  const app = await readFile(new URL('./apps/abnahme/index.html', dist), 'utf8');
  assert.match(app, /href="\.\/manifest\.webmanifest"/);
  assert.match(app, /src="\.\/app\.js"/);
  const manifest = JSON.parse(await readFile(new URL('./apps/abnahme/manifest.webmanifest', dist), 'utf8'));
  assert.equal(manifest.id, './');
  assert.equal(manifest.scope, './');
  assert.equal(manifest.start_url, './');
});

test('Demo-Offline-Cache ist auf den App-Pfad beschränkt', async () => {
  const sw = await readFile(new URL('./apps/abnahme/sw.js', dist), 'utf8');
  assert.match(sw, /const prefix = 'app-shell:' \+ scope\.pathname/);
  assert.match(sw, /key\.startsWith\(prefix\)/);
  assert.match(sw, /url\.pathname\.startsWith\(scope\.pathname\)/);
  const names = ['index.html','app.js','counter.mjs','styles.css','manifest.webmanifest','icons/app.svg'];
  for (const name of names) assert.ok(sw.includes("'./" + name + "'"), 'Offline-Asset fehlt: ' + name);
});

test('Demo-Zähler arbeitet unabhängig vom Browser-Speicher', () => {
  for (const value of [null, undefined, '', '-1', 'NaN', '10000', '1.5', '01', 'Infinity']) {
    assert.equal(parseCount(value), 0);
  }
  assert.equal(parseCount('0'), 0);
  assert.equal(parseCount('5'), 5);
  assert.equal(parseCount('9999'), MAX_COUNT);
  assert.equal(incrementCount(0), 1);
  assert.equal(incrementCount(MAX_COUNT), MAX_COUNT);
  assert.throws(() => incrementCount(-1));
  assert.throws(() => incrementCount(10000));
});
