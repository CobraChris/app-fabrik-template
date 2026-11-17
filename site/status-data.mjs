// Read-only GitHub status for repositories already listed in the local project board.
// Deliberately no login, OAuth, GitHub token, or endpoint for private repositories.
import { githubRepository } from './github-links.mjs';

// A request budget, not a project-count limit. Unauthenticated GitHub has 60 calls/hour/IP.
export const MAX_API_REQUESTS_PER_PASS = 42;
export const GITHUB_RATE_RESERVE = 3;

export function parseGithubRateLimit(rawRemaining, rawReset) {
  const remaining = typeof rawRemaining === 'string' && /^\d+$/.test(rawRemaining)
    ? Number(rawRemaining) : null;
  const resetSeconds = typeof rawReset === 'string' && /^\d{9,12}$/.test(rawReset)
    ? Number(rawReset) : null;
  return {
    remaining: Number.isSafeInteger(remaining) ? remaining : null,
    resetAt: Number.isSafeInteger(resetSeconds) ? resetSeconds * 1000 : null
  };
}

export function canQueryGithub({ used, remaining, blockedUntil }, now = Date.now()) {
  return Number.isInteger(used) && used >= 0 && used < MAX_API_REQUESTS_PER_PASS &&
    (remaining === null || (Number.isInteger(remaining) && remaining >= GITHUB_RATE_RESERVE)) &&
    (!Number.isFinite(blockedUntil) || blockedUntil <= now);
}
export const MAX_WORKFLOW_RUNS = 30;

export function githubApiUrls(rawRepository, rawBranch = 'main') {
  const repo = githubRepository(rawRepository);
  const branch = String(rawBranch || '').trim();
  if (!/^[a-zA-Z0-9._/-]{1,120}$/.test(branch) ||
      branch.startsWith('/') || branch.endsWith('/') ||
      branch.includes('..') || branch.includes('//')) throw Error('Ungültiger GitHub-Branch.');
  const base = 'https://api.github.com/repos/' + repo;
  return {
    repository: base,
    workflows: base + '/actions/runs?branch=' + encodeURIComponent(branch) +
      '&per_page=' + MAX_WORKFLOW_RUNS,
    releases: base + '/releases?per_page=5',
    web: 'https://github.com/' + repo,
  };
}

export function parseRepository(raw, expected) {
  const repo = githubRepository(expected);
  if (!raw || typeof raw !== 'object' || Array.isArray(raw) ||
      typeof raw.full_name !== 'string' ||
      raw.full_name.toLowerCase() !== repo.toLowerCase() ||
      raw.private !== false || raw.archived === true ||
      typeof raw.default_branch !== 'string' || !raw.default_branch.trim()) {
    throw Error('Repository nicht öffentlich zugänglich.');
  }
  // Branch must be validated before it is inserted into an API URL.
  githubApiUrls(repo, raw.default_branch);
  return { branch: raw.default_branch,
    updatedAt: typeof raw.pushed_at === 'string' ? raw.pushed_at : null };
}

function safeTimestamp(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}T/.test(value)) return null;
  const n = Date.parse(value);
  return Number.isFinite(n) ? new Date(n).toISOString() : null;
}

export function parseWorkflowRuns(raw, rawRepo, branch) {
  const repo = githubRepository(rawRepo);
  if (!raw || !Array.isArray(raw.workflow_runs)) throw Error('Workflow-Status nicht verfügbar.');
  const run = raw.workflow_runs.find(x =>
    x && x.head_branch === branch && x.repository?.full_name?.toLowerCase() === repo.toLowerCase() &&
    Number.isSafeInteger(x.id) && x.id > 0 &&
    ['queued','in_progress','completed','waiting','requested','pending'].includes(x.status));
  if (!run) return { label: 'Keine Läufe gefunden', kind: 'unknown', url: null, timestamp: null };
  const kind = run.status !== 'completed' ? 'running' :
    run.conclusion === 'success' ? 'success' :
      run.conclusion === 'failure' || run.conclusion === 'timed_out' ? 'failure' : 'unknown';
  const label = run.status !== 'completed' ? 'Läuft / wartet' :
    run.conclusion === 'success' ? 'Erfolgreich' :
      run.conclusion === 'failure' || run.conclusion === 'timed_out' ? 'Fehlgeschlagen' :
        run.conclusion === 'cancelled' ? 'Abgebrochen' : 'Nicht erfolgreich abgeschlossen';
  return {
    label,
    kind,
    workflow: typeof run.name === 'string' ? run.name.slice(0, 100) : 'GitHub Actions',
    url: 'https://github.com/' + repo + '/actions/runs/' + run.id,
    timestamp: safeTimestamp(run.updated_at)
  };
}

export function parseRelease(raw, rawRepo) {
  const repo = githubRepository(rawRepo);
  if (!Array.isArray(raw)) throw Error('GitHub Releases nicht verfügbar.');
  const item = raw.find(r =>
    r && r.draft !== true && typeof r.tag_name === 'string' &&
    /^[A-Za-z0-9][A-Za-z0-9._+/-]{0,119}$/.test(r.tag_name) &&
    !r.tag_name.includes('..'));
  if (!item) return { label:'Kein GitHub Release', url: null, timestamp: null };
  return {
    label: item.tag_name.slice(0, 120) + (item.prerelease === true ? ' (Vorabversion)' : ''),
    url: 'https://github.com/' + repo + '/releases/tag/' + encodeURIComponent(item.tag_name),
    timestamp: safeTimestamp(item.published_at)
  };
}

// Explicitly represent a private or missing repository as unobservable.
// GitHub's unauthenticated 404 deliberately does not distinguish the two.
export function classifyRepoHttp(status) {
  if (status === 401 || status === 403) return 'Abfrage nicht möglich (API-Limit/Zugriff)';
  if (status === 404) return 'Privat oder nicht vorhanden';
  return 'GitHub antwortet nicht (' + status + ')';
}
