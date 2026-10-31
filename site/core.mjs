export const STORE_KEY = 'app-fabrik-ideas-v1';

export function normalizeTitle(value) {
  return typeof value === 'string' ? value.trim().replace(/\s+/g, ' ').slice(0, 100) : '';
}

export function addIdea(items, title) {
  const normalized = normalizeTitle(title);
  if (!normalized) throw new Error('Bitte eine App-Idee eingeben.');
  if (items.some(item => item.toLocaleLowerCase('de') === normalized.toLocaleLowerCase('de'))) {
    throw new Error('Diese Idee existiert bereits.');
  }
  return [...items, normalized];
}

export function removeIdea(items, title) {
  return items.filter(item => item !== title);
}

export function parseIdeas(json) {
  try {
    const value = JSON.parse(json);
    if (!Array.isArray(value)) return [];
    return [...new Set(value.filter(v => typeof v === 'string').map(normalizeTitle).filter(Boolean))].slice(0, 500);
  } catch {
    return [];
  }
}
