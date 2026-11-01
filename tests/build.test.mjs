import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { validateConfig, escapeHtml } from '../scripts/build.mjs';

const example = {
  name: 'Beispiel-App',
  shortName: 'Beispiel',
  description: 'Ein Test',
  themeColor: '#102030',
  accentColor: '#abcdef',
  iconLetter: 'B',
};

test('Name, Farben und Icon werden validiert', () => {
  assert.equal(validateConfig(example).name, 'Beispiel-App');
  assert.throws(() => validateConfig({ ...example, accentColor: 'red;background:url(x)' }), /Ungültige Farbe/);
  assert.throws(() => validateConfig({ ...example, iconLetter: '<' }), /iconLetter/);
  assert.throws(() => validateConfig({ ...example, name: '' }), /name/);
});

test('HTML-Sonderzeichen werden entschärft', () => {
  assert.equal(escapeHtml('<script>alert("x")</script>'), '&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;');
});

test('Generiertes Manifest und HTML verwenden die zentrale Konfiguration', async () => {
  const cfg = JSON.parse(await readFile(new URL('../app.config.json', import.meta.url), 'utf8'));
  const manifest = JSON.parse(await readFile(new URL('../dist/manifest.webmanifest', import.meta.url), 'utf8'));
  const html = await readFile(new URL('../dist/index.html', import.meta.url), 'utf8');
  const css = await readFile(new URL('../dist/styles.css', import.meta.url), 'utf8');
  const icon = await readFile(new URL('../dist/icons/app.svg', import.meta.url), 'utf8');
  assert.equal(manifest.name, cfg.name);
  assert.equal(manifest.short_name, cfg.shortName);
  assert.equal(manifest.theme_color, cfg.themeColor);
  assert.ok(html.includes('<title>' + cfg.name + '</title>'));
  assert.ok(css.includes('--app-accent:' + cfg.accentColor));
  assert.ok(icon.includes('>' + cfg.iconLetter + '</text>'));
});
