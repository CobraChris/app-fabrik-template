import { STORE_KEY, parseIdeas } from './core.mjs';
import {
  storageKey, normalizeBrief, upsertBrief, removeBrief, parseBriefs, migrateLegacy,
  issueDraft, exportBackup, importBackup
} from './briefs.mjs';

const scopePath = new URL('./', import.meta.url).pathname;
const key = storageKey(scopePath);
const $ = selector => document.querySelector(selector);
const form = $('#idea-form'), list = $('#idea-list');
const feedback = $('#form-feedback'), count = $('#count');
const fields = {
  id: $('#edit-id'),
  title: $('#idea-title'),
  goal: $('#idea-goal'),
  features: $('#idea-features'),
  acceptance: $('#idea-acceptance'),
  constraints: $('#idea-constraints'),
  priority: $('#idea-priority'),
  status: $('#idea-status'),
};
let briefs = [];
let storageAvailable = true;

function notify(message) { feedback.textContent = message; }

function save() {
  if (!storageAvailable) return;
  try { localStorage.setItem(key, JSON.stringify(briefs)); }
  catch { storageAvailable = false; notify('Speichern blockiert: Ideen gelten nur bis zum Neuladen. Bitte Backup exportieren.'); }
}

function load() {
  try {
    const existing = localStorage.getItem(key);
    if (existing !== null) {
      briefs = parseBriefs(existing);
      if (briefs.length === 0 && existing !== '[]') notify('Gespeicherte Daten konnten nicht vollständig gelesen werden. Bitte ein vorhandenes Backup prüfen.');
    } else if (scopePath === '/app-fabrik-template/') {
      // Nur für die ursprüngliche App: den früheren globalen Ideas-Key einmal übernehmen.
      briefs = migrateLegacy(parseIdeas(localStorage.getItem(STORE_KEY)));
      save();
      if (briefs.length) notify('Bisherige Ideen wurden in das neue Format übernommen.');
    }
  } catch {
    storageAvailable = false;
    notify('Gerätespeicher nicht verfügbar. Ideen bleiben nur bis zum Neuladen erhalten.');
  }
}

function resetForm() {
  form.reset();
  fields.id.value = '';
  $('#idea-details').open = false;
  $('#save-button').textContent = '+ Idee speichern';
  $('#cancel-button').hidden = true;
}

function getValues() {
  const id = fields.id.value || (globalThis.crypto?.randomUUID?.() ?? ('brief-' + Date.now() + '-' + Math.random().toString(36).slice(2)));
  return Object.fromEntries(Object.entries(fields).map(([name, input]) => [name, name === 'id' ? id : input.value]));
}

function edit(brief) {
  for (const [name, input] of Object.entries(fields)) input.value = brief[name] ?? '';
  $('#idea-details').open = true;
  $('#save-button').textContent = 'Änderungen speichern';
  $('#cancel-button').hidden = false;
  fields.title.focus();
  form.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function copyBrief(brief) {
  const value = issueDraft(brief);
  try {
    if (!navigator.clipboard?.writeText) throw new Error('Clipboard nicht verfügbar');
    await navigator.clipboard.writeText(value);
    $('#copy-fallback').hidden = true;
    notify('Entwicklungsauftrag kopiert. Füge ihn jetzt in den Chat ein.');
  } catch {
    $('#copy-text').value = value;
    $('#copy-fallback').hidden = false;
    $('#copy-text').focus();
    $('#copy-text').select();
    notify('Automatisches Kopieren nicht möglich. Der Text ist unten zur manuellen Auswahl markiert.');
  }
}

function actionButton(text, action, label) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'secondary-button';
  b.textContent = text;
  b.setAttribute('aria-label', label);
  b.addEventListener('click', action);
  return b;
}

function render() {
  list.replaceChildren();
  count.textContent = briefs.length + (briefs.length === 1 ? ' Idee' : ' Ideen');
  $('#empty-state').hidden = briefs.length !== 0;
  for (const brief of briefs) {
    const li = document.createElement('li');
    li.className = 'idea-card';
    const heading = document.createElement('div');
    heading.className = 'idea-card-heading';
    const title = document.createElement('strong');
    title.textContent = brief.title;
    const meta = document.createElement('span');
    meta.className = 'idea-meta';
    meta.textContent = brief.status + ' · ' + brief.priority;
    heading.append(title, meta);
    li.append(heading);
    if (brief.goal) {
      const description = document.createElement('p');
      description.textContent = brief.goal;
      li.append(description);
    }
    const actions = document.createElement('div');
    actions.className = 'idea-actions';
    actions.append(
      actionButton('Bearbeiten', () => edit(brief), brief.title + ' bearbeiten'),
      actionButton('Für ChatGPT kopieren', () => copyBrief(brief), brief.title + ' als Entwicklungsauftrag kopieren'),
      actionButton('Löschen', () => {
        if (!confirm('Idee "' + brief.title + '" wirklich löschen?')) return;
        briefs = removeBrief(briefs, brief.id);
        if (fields.id.value === brief.id) resetForm();
        save(); render(); notify('Idee gelöscht.');
      }, brief.title + ' löschen')
    );
    li.append(actions);
    list.append(li);
  }
}

form.addEventListener('submit', event => {
  event.preventDefault();
  try {
    briefs = upsertBrief(briefs, getValues());
    save(); resetForm(); render();
    if (storageAvailable) notify('Idee gespeichert.');
    fields.title.focus();
  } catch (error) { notify(error.message); }
});
$('#cancel-button').addEventListener('click', () => { resetForm(); notify('Bearbeitung abgebrochen.'); });
$('#export-button').addEventListener('click', () => {
  const data = exportBackup(briefs);
  const url = URL.createObjectURL(new Blob([data], { type: 'application/json;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url; link.download = 'app-fabrik-backup.json'; link.click();
  // Nicht sofort revoken: Mobile Browser benötigen Gelegenheit zum Download.
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  notify('Backup erstellt. Bewahre die JSON-Datei sicher auf.');
});
$('#import-button').addEventListener('click', () => $('#import-file').click());
$('#import-file').addEventListener('change', async event => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  if (file.size > 1024 * 1024) { notify('Backup zu groß (maximal 1 MB).'); return; }
  try {
    const imported = importBackup(await file.text());
    if (briefs.length && !confirm('Vorhandene Ideen durch das Backup ersetzen?')) return;
    briefs = imported; save(); resetForm(); render();
    if (storageAvailable) notify(imported.length + ' Ideen aus dem Backup wiederhergestellt.');
  } catch (error) { notify(error.message); }
});

load(); render();
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}
