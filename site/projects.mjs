import { githubRepository } from './github-links.mjs';

export const PROJECT_LIMIT = 100;
export const PROJECT_STATES = ['Offen', 'In Arbeit', 'Wartet', 'Fertig'];

export function projectKey(scope) {
  if (typeof scope !== 'string' || !/^\/(?:[a-zA-Z0-9._~-]+\/)*$/.test(scope)) {
    throw new Error('Ungültiger App-Pfad.');
  }
  return 'app-fabrik-projects-v1:' + scope;
}

function bounded(value, max) {
  return typeof value === 'string' ? value.trim().slice(0, max) : '';
}

export function normalizeProject(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    throw new Error('Ungültiges Projekt.');
  }
  const id = typeof raw.id === 'string' && /^[A-Za-z0-9_-]{1,100}$/.test(raw.id) ? raw.id : '';
  if (!id) throw new Error('Projekt-ID fehlt.');
  const repo = githubRepository(raw.repository);
  return {
    id,
    repository: repo,
    status: PROJECT_STATES.includes(raw.status) ? raw.status : 'Offen',
    nextStep: bounded(raw.nextStep, 350),
    notes: bounded(raw.notes, 1000),
  };
}

export function upsertProject(projects, raw) {
  if (!Array.isArray(projects)) throw new Error('Projektliste ungültig.');
  const project = normalizeProject(raw);
  const normalized = project.repository.toLocaleLowerCase('en-US');
  if (projects.some(item => item.id !== project.id &&
    item.repository.toLocaleLowerCase('en-US') === normalized)) {
    throw new Error('Dieses GitHub-Repository ist bereits eingetragen.');
  }
  const exists = projects.some(item => item.id === project.id);
  if (!exists && projects.length >= PROJECT_LIMIT) throw new Error('Maximal 100 Projekte möglich.');
  return exists ? projects.map(item => item.id === project.id ? project : item) : [project, ...projects];
}

export function deleteProject(projects, id) {
  return projects.filter(item => item.id !== id);
}

export function sortProjects(projects) {
  const rank = { 'In Arbeit': 0, 'Offen': 1, 'Wartet': 2, 'Fertig': 3 };
  return [...projects].sort((a, b) => rank[a.status] - rank[b.status] ||
    a.repository.localeCompare(b.repository, 'de'));
}

export function openProjectsCount(projects) {
  return projects.filter(item => item.status !== 'Fertig').length;
}

export function projectLinks(raw) {
  const project = normalizeProject(raw);
  const root = 'https://github.com/' + project.repository;
  return {
    repository: root,
    issues: root + '/issues',
    pulls: root + '/pulls',
    actions: root + '/actions',
    pages: root + '/settings/pages',
  };
}

export function parseProjects(json) {
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed)) return [];
    const valid = [];
    for (const candidate of parsed.slice(0, PROJECT_LIMIT)) {
      try {
        const project = normalizeProject(candidate);
        if (!valid.some(item => item.id === project.id ||
          item.repository.toLowerCase() === project.repository.toLowerCase())) valid.push(project);
      } catch { /* Fehlerhafte Einträge werden ausgelassen. */ }
    }
    return valid;
  } catch { return []; }
}

export function exportProjects(items) {
  if (!Array.isArray(items) || items.length > PROJECT_LIMIT) throw new Error('Projektliste ungültig.');
  return JSON.stringify({ schemaVersion: 1, projects: items.map(normalizeProject) }, null, 2) + '\n';
}

export function importProjects(json) {
  let data;
  try { data = JSON.parse(json); }
  catch { throw new Error('Keine gültige JSON-Datei.'); }
  if (!data || data.schemaVersion !== 1 || !Array.isArray(data.projects) ||
      data.projects.length > PROJECT_LIMIT) throw new Error('Unbekanntes Projekt-Backup.');
  let result = [];
  for (const candidate of data.projects) {
    const project = normalizeProject(candidate);
    if (result.some(item => item.id === project.id ||
      item.repository.toLowerCase() === project.repository.toLowerCase())) {
      throw new Error('Backup enthält doppelte Projekte.');
    }
    result = [...result, project];
  }
  return result;
}
