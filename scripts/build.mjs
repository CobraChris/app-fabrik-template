import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { buildApps } from './build-apps.mjs';

const root = fileURLToPath(new URL('../', import.meta.url));
const sourceDir = path.join(root, 'site');
const outputDir = path.join(root, 'dist');

export function validateConfig(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) {
    throw new Error('app.config.json muss ein JSON-Objekt enthalten.');
  }
  const fields = {
    name: 70,
    shortName: 30,
    description: 180,
  };
  const result = {};
  for (const [key, limit] of Object.entries(fields)) {
    if (typeof input[key] !== 'string' || !input[key].trim() || input[key].trim().length > limit ||
        /[\u0000-\u001f\u007f]/.test(input[key])) {
      throw new Error('Ungültiges Konfigurationsfeld: ' + key);
    }
    result[key] = input[key].trim();
  }
  for (const key of ['themeColor', 'accentColor']) {
    if (typeof input[key] !== 'string' || !/^#[0-9a-fA-F]{6}$/.test(input[key])) {
      throw new Error('Ungültige Farbe in: ' + key + '. Benötigt wird #RRGGBB.');
    }
    result[key] = input[key];
  }
  if (typeof input.iconLetter !== 'string' || !/^[A-Z0-9]$/.test(input.iconLetter)) {
    throw new Error('iconLetter muss genau ein Großbuchstabe A-Z oder eine Ziffer sein.');
  }
  result.iconLetter = input.iconLetter;
  return result;
}

export function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[char]);
}

function replaceOnce(source, expression, replacement, label) {
  if (!expression.test(source)) {
    throw new Error('Erwartete HTML-Markierung fehlt: ' + label);
  }
  return source.replace(expression, () => replacement);
}

function renderHtml(source, config) {
  let html = source;
  html = replaceOnce(html, /<title>[^<]*<\/title>/,
    '<title>' + escapeHtml(config.name) + '</title>', 'title');
  html = replaceOnce(html, /<meta name="theme-color" content="[^"]*">/,
    '<meta name="theme-color" content="' + config.themeColor + '">', 'theme-color');
  html = replaceOnce(html, /<meta name="description" content="[^"]*">/,
    '<meta name="description" content="' + escapeHtml(config.description) + '">', 'description');
  html = replaceOnce(html, /<div class="brand-icon">[^<]*<\/div>/,
    '<div class="brand-icon">' + config.iconLetter + '</div>', 'brand-icon');
  html = replaceOnce(html, /APP-FABRIK <small>\/ STARTER<\/small>/,
    escapeHtml(config.name) + ' <small>/ APP</small>', 'app-brand');
  return html;
}

function renderIcon(config) {
  return '<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">' +
    '<rect width="512" height="512" rx="112" fill="' + config.themeColor + '"/>' +
    '<text x="256" y="326" text-anchor="middle" font-size="250" font-family="Arial,sans-serif" font-weight="800" fill="' +
    config.accentColor + '">' + config.iconLetter + '</text></svg>\n';
}

export async function build() {
  const config = validateConfig(JSON.parse(await readFile(path.join(root, 'app.config.json'), 'utf8')));
  const html = await readFile(path.join(sourceDir, 'index.html'), 'utf8');
  const css = await readFile(path.join(sourceDir, 'styles.css'), 'utf8');
  const manifest = JSON.parse(await readFile(path.join(sourceDir, 'manifest.webmanifest'), 'utf8'));

  manifest.name = config.name;
  manifest.short_name = config.shortName;
  manifest.description = config.description;
  manifest.theme_color = config.themeColor;

  await rm(outputDir, { recursive: true, force: true });
  await cp(sourceDir, outputDir, { recursive: true });
  await mkdir(path.join(outputDir, 'icons'), { recursive: true });
  await Promise.all([
    writeFile(path.join(outputDir, 'index.html'), renderHtml(html, config)),
    writeFile(path.join(outputDir, 'styles.css'), ':root{--app-accent:' + config.accentColor + '}\n' + css),
    writeFile(path.join(outputDir, 'manifest.webmanifest'), JSON.stringify(manifest, null, 2) + '\n'),
    writeFile(path.join(outputDir, 'icons', 'app.svg'), renderIcon(config)),
  ]);
  await buildApps(outputDir);
  return outputDir;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  build().then(() => console.log('PWA erfolgreich gebaut: dist/'))
    .catch(error => { console.error(error.message); process.exitCode = 1; });
}
