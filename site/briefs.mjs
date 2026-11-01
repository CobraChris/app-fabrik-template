import { normalizeTitle } from './core.mjs';

export const BRIEF_LIMIT = 200;
export const STATUS = ['Idee', 'Planung', 'In Arbeit', 'Erledigt'];
export const PRIORITY = ['Normal', 'Hoch', 'Später'];

export function storageKey(scopePath) {
  if (typeof scopePath !== 'string' || !/^\/(?:[a-zA-Z0-9._~-]+\/)*$/.test(scopePath)) {
    throw new Error('Ungültiger App-Pfad.');
  }
  return 'app-fabrik-briefs-v1:' + scopePath;
}

function bounded(value, max) {
  return typeof value === 'string' ? value.trim().slice(0, max) : '';
}

export function normalizeBrief(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    throw new Error('Ungültiger App-Steckbrief.');
  }
  const title = normalizeTitle(data.title);
  if (!title) throw new Error('Bitte einen App-Namen eingeben.');
  const id = typeof data.id === 'string' && /^[\w-]{1,100}$/.test(data.id) ? data.id : '';
  if (!id) throw new Error('App-ID fehlt oder ist ungültig.');
  return {
    id, title,
    goal: bounded(data.goal, 1000),
    features: bounded(data.features, 2500),
    acceptance: bounded(data.acceptance, 2000),
    constraints: bounded(data.constraints, 1000),
    priority: PRIORITY.includes(data.priority) ? data.priority : 'Normal',
    status: STATUS.includes(data.status) ? data.status : 'Idee',
  };
}

export function upsertBrief(items, raw) {
  const brief = normalizeBrief(raw);
  if (!Array.isArray(items)) throw new Error('Ideenliste ungültig.');
  if (items.some(item => item.id !== brief.id &&
    item.title.toLocaleLowerCase('de') === brief.title.toLocaleLowerCase('de'))) {
    throw new Error('Eine Idee mit diesem Namen existiert bereits.');
  }
  const idx = items.findIndex(item => item.id === brief.id);
  if (idx < 0 && items.length >= BRIEF_LIMIT) throw new Error('Maximal 200 Ideen pro App möglich.');
  if (idx < 0) return [brief, ...items];
  return items.map(item => item.id === brief.id ? brief : item);
}

export function removeBrief(items, id) {
  return items.filter(item => item.id !== id);
}

export function parseBriefs(raw) {
  try {
    const data = JSON.parse(raw);
    if (!Array.isArray(data)) return [];
    const items = [];
    for (const item of data.slice(0, BRIEF_LIMIT)) {
      try { const brief = normalizeBrief(item);
        if (!items.some(existing => existing.id === brief.id ||
          existing.title.toLocaleLowerCase('de') === brief.title.toLocaleLowerCase('de'))) items.push(brief);
      } catch { /* Ungültige Einträge ignorieren, gültige behalten. */ }
    }
    return items;
  } catch { return []; }
}

export function migrateLegacy(titles) {
  if (!Array.isArray(titles)) return [];
  return titles.slice(0, BRIEF_LIMIT).map((title, index) => normalizeBrief({
    id: 'legacy-' + String(index + 1), title,
  }));
}

export function issueDraft(raw) {
  const brief = normalizeBrief(raw);
  const fallback = '_Noch zu definieren_';
  const sections = [
    '# App-Idee: ' + brief.title,
    '## Ziel\n' + (brief.goal || fallback),
    '## MVP-Funktionen\n' + (brief.features || fallback),
    '## Abnahmekriterien\n' + (brief.acceptance || fallback),
    '## Rahmenbedingungen\n' + (brief.constraints || 'Kleine mobile Web-App/PWA. Keine Secrets oder kostenpflichtigen Dienste ohne Freigabe.'),
    '## Priorität und Status\nPriorität: ' + brief.priority + '\nStatus: ' + brief.status,
  ];
  return 'Bitte erstelle aus folgendem Steckbrief ein strukturiertes GitHub Issue im passenden Repository. ' +
    'Falls das Ziel-Repository nicht feststeht, frage nach. Starte weder Codex noch kostenpflichtige Aktionen. ' +
    'Keine vertraulichen Angaben in öffentliche Repositories übertragen.\n\n' + sections.join('\n\n') + '\n';
}

export function exportBackup(items) {
  if (!Array.isArray(items) || items.length > BRIEF_LIMIT) throw new Error('Ideenliste ungültig.');
  return JSON.stringify({ schemaVersion: 1, briefs: items.map(normalizeBrief) }, null, 2) + '\n';
}

export function importBackup(raw) {
  let data;
  try { data = JSON.parse(raw); } catch { throw new Error('Die Datei enthält kein gültiges JSON.'); }
  if (!data || data.schemaVersion !== 1 || !Array.isArray(data.briefs) ||
      data.briefs.length > BRIEF_LIMIT) {
    throw new Error('Unbekanntes oder ungültiges Backup-Format.');
  }
  const items = [];
  for (const rawBrief of data.briefs) {
    const brief = normalizeBrief(rawBrief);
    if (items.some(x => x.id === brief.id || x.title.toLocaleLowerCase('de') === brief.title.toLocaleLowerCase('de'))) {
      throw new Error('Das Backup enthält doppelte Ideen.');
    }
    items.push(brief);
  }
  return items;
}
