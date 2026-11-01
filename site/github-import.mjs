import { githubOwner, githubRepository } from './github-links.mjs';
import { PROJECT_LIMIT, normalizeProject, upsertProject } from './projects.mjs';

export const PUBLIC_REPO_PAGE_SIZE = 100;

export function publicRepositoriesUrl(rawOwner) {
  const owner = githubOwner(rawOwner);
  const params = new URLSearchParams({
    type: 'owner',
    sort: 'updated',
    per_page: String(PUBLIC_REPO_PAGE_SIZE),
    page: '1'
  });
  return 'https://api.github.com/users/' + encodeURIComponent(owner) + '/repos?' + params;
}

export function parsePublicRepositories(raw, rawOwner) {
  const owner = githubOwner(rawOwner);
  if (!Array.isArray(raw)) throw new Error('GitHub hat keine gültige Repository-Liste geliefert.');
  const results = [];
  for (const value of raw.slice(0, PUBLIC_REPO_PAGE_SIZE)) {
    if (!value || typeof value !== 'object' ||
        value.private !== false ||
        !value.owner || typeof value.owner.login !== 'string' ||
        value.owner.login.toLowerCase() !== owner.toLowerCase() ||
        value.archived === true) continue;
    try {
      const repository = githubRepository(value.full_name);
      if (repository.split('/')[0].toLowerCase() !== owner.toLowerCase()) continue;
      if (results.some(r => r.repository.toLowerCase() === repository.toLowerCase())) continue;
      results.push({
        repository,
        description: typeof value.description === 'string' ? value.description.slice(0, 160) : '',
        fork: value.fork === true
      });
    } catch { /* Nicht vertrauenswürdige GitHub-Antworten werden gefiltert. */ }
  }
  return results;
}

export function mergePublicRepositories(existing, available, selectedRepositories, createId) {
  if (!Array.isArray(existing) || !Array.isArray(available) ||
      !Array.isArray(selectedRepositories) || typeof createId !== 'function') {
    throw new Error('Ungültige Projektauswahl.');
  }
  if (new Set(selectedRepositories.map(x => String(x).toLowerCase())).size !== selectedRepositories.length) {
    throw new Error('Doppelte Repositories ausgewählt.');
  }
  const allowed = new Set(available.map(x => githubRepository(x.repository).toLowerCase()));
  let projects = existing.map(normalizeProject);
  let added = 0;
  for (const selected of selectedRepositories) {
    const repository = githubRepository(selected);
    if (!allowed.has(repository.toLowerCase())) throw new Error('Repository gehört nicht zu den Suchergebnissen.');
    if (projects.some(p => p.repository.toLowerCase() === repository.toLowerCase())) continue;
    if (projects.length >= PROJECT_LIMIT) throw new Error('Die Projektübersicht ist voll (maximal 100 Projekte).');
    const id = createId();
    if (typeof id !== 'string' || projects.some(p => p.id === id)) {
      throw new Error('Die neue Projekt-ID ist nicht eindeutig.');
    }
    projects = upsertProject(projects, {
      id, repository, status: 'Offen', nextStep: '', notes: '', todos: []
    });
    added++;
  }
  return { projects, added };
}
