import { cp, lstat, mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../apps/', import.meta.url));
const slugPattern = /^[a-z0-9](?:[a-z0-9-]{0,48}[a-z0-9])?$/;

export function validAppSlug(slug) {
  if (typeof slug !== 'string' || !slugPattern.test(slug) || slug.includes('--')) {
    throw new Error('Ungültiger App-Ordnername: ' + String(slug));
  }
  return slug;
}

function htmlEscape(value) {
  return String(value).replace(/[&<>"']/g, x => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[x]);
}

function localAsset(value) {
  if (typeof value !== 'string' || !/^\.\/[a-zA-Z0-9_-][a-zA-Z0-9_./-]*$/.test(value) ||
      value.split('/').includes('..') || value.includes('//')) {
    throw new Error('Ungültiger relativer App-Dateipfad: ' + String(value));
  }
  return value;
}

async function rejectSymlinks(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    if (entry.isSymbolicLink()) throw new Error('Symbolische Links in Apps sind nicht erlaubt: ' + entry.name);
    if (entry.isDirectory()) await rejectSymlinks(path.join(dir, entry.name));
    else if (!entry.isFile()) throw new Error('Unerwarteter App-Dateityp: ' + entry.name);
  }
}

export function validateAppManifest(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    throw new Error('App-Manifest muss ein JSON-Objekt sein.');
  }
  for (const field of ['name', 'short_name', 'description']) {
    if (typeof raw[field] !== 'string' || !raw[field].trim() ||
        raw[field].length > (field === 'description' ? 180 : 80)) {
      throw new Error('Ungültiger Manifest-Wert: ' + field);
    }
  }
  if (raw.id !== './' || raw.scope !== './' || raw.start_url !== './' || raw.display !== 'standalone') {
    throw new Error('Jede App braucht eigenes id, scope und start_url "./" sowie display "standalone".');
  }
  if (!Array.isArray(raw.icons) || raw.icons.length === 0) {
    throw new Error('App-Icon fehlt.');
  }
  const sizes = new Set(raw.icons.flatMap(icon =>
    typeof icon?.sizes === 'string' ? icon.sizes.split(/\s+/) : []));
  if (!sizes.has('192x192') || !sizes.has('512x512')) {
    throw new Error('Android-Installation benötigt App-Icons in 192x192 und 512x512.');
  }
  return {
    name: raw.name.trim(),
    description: raw.description.trim(),
    icons: raw.icons.map(icon => localAsset(icon?.src))
  };
}

export function renderAppCatalog(apps) {
  const cards = apps.map(app => '<li><a href="./' + app.slug + '/">' +
    '<span class="name">' + htmlEscape(app.name) + '</span>' +
    '<span class="description">' + htmlEscape(app.description) + '</span></a></li>').join('\n');
  return '<!doctype html>\n<html lang="de"><head><meta charset="utf-8">' +
    '<meta name="viewport" content="width=device-width,initial-scale=1">' +
    '<meta name="color-scheme" content="dark"><title>Veröffentlichte Apps · App-Fabrik</title>' +
    '<style>body{background:#101622;color:#e7f4ff;font:16px system-ui,sans-serif;' +
    'margin:0}main{max-width:750px;margin:auto;padding:24px 18px 65px}' +
    'a{color:#b1e0ff}h1{font-size:clamp(28px,7vw,42px)}' +
    'ul{list-style:none;padding:0;display:grid;gap:14px}' +
    'li a{display:block;padding:19px;border:1px solid #3c5875;border-radius:15px;' +
    'text-decoration:none;background:#1a2c42}.name{display:block;font-size:19px;' +
    'font-weight:750;color:#edf8ff}.description{display:block;color:#bbcee5;' +
    'font-size:14px;margin-top:7px}a:focus-visible{outline:3px solid #b1e0ff}' +
    '</style></head><body><main><p><a href="../">Zur App-Fabrik</a></p>' +
    '<h1>Veröffentlichte Apps</h1><p>Eigenständige Web-Apps im selben kostenlosen ' +
    'GitHub-Pages-Projekt. Ihre Daten bleiben lokal im Browser, soweit in der App angegeben.</p>' +
    (apps.length ? '<ul>' + cards + '</ul>' : '<p>Noch keine Apps veröffentlicht.</p>') +
    '</main></body></html>\n';
}

export async function buildApps(outputDir) {
  const appsDestination = path.join(outputDir, 'apps');
  await mkdir(appsDestination, { recursive: true });
  const items = await readdir(root, { withFileTypes: true });
  const apps = [];
  for (const entry of items.sort((a, b) => a.name.localeCompare(b.name))) {
    if (!entry.isDirectory()) throw new Error('Unter apps/ sind nur App-Verzeichnisse erlaubt: ' + entry.name);
    const slug = validAppSlug(entry.name);
    const source = path.join(root, slug);
    await rejectSymlinks(source);
    const manifest = validateAppManifest(JSON.parse(await readFile(path.join(source, 'manifest.webmanifest'), 'utf8')));
    for (const asset of ['./index.html', ...manifest.icons]) {
      const file = path.join(source, localAsset(asset));
      if (!(await lstat(file)).isFile()) throw new Error('App-Datei fehlt: ' + slug + '/' + asset);
    }
    const html = await readFile(path.join(source, 'index.html'), 'utf8');
    if (!html.includes('href="./manifest.webmanifest"')) {
      throw new Error('App-HTML verweist nicht auf ihr eigenes Manifest: ' + slug);
    }
    await cp(source, path.join(appsDestination, slug), { recursive: true });
    apps.push({ slug, name: manifest.name, description: manifest.description });
  }
  await writeFile(path.join(appsDestination, 'index.html'), renderAppCatalog(apps));
  return apps;
}
