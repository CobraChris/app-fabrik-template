import { normalizeBrief } from './briefs.mjs';

export const PREFILL_LIMIT = 4800;
export function githubOwner(raw) {
  const value = typeof raw === 'string' ? raw.trim() : '';
  if (!/^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?$/.test(value) || value.includes('--')) {
    throw new Error('Bitte ein gültiges GitHub-Konto eingeben.');
  }
  return value;
}
export function githubRepository(raw) {
  const parts = String(raw || '').trim().split('/');
  if (parts.length !== 2) throw new Error('Repository bitte als Benutzer/Repository eingeben.');
  const owner = githubOwner(parts[0]);
  const name = parts[1];
  if (!/^[a-zA-Z0-9_.-]{1,100}$/.test(name) || name.startsWith('.') || name === '..') {
    throw new Error('Ungültiger GitHub-Repository-Name.');
  }
  return owner + '/' + name;
}
export function ownerFromHostname(hostname) {
  const match = /^([a-zA-Z0-9-]+)\.github\.io$/i.exec(hostname || '');
  if (!match) return '';
  try { return githubOwner(match[1]); } catch { return ''; }
}
export function repositorySlug(raw) {
  const name = String(raw || '').replace(/Ä/g,'Ae').replace(/Ö/g,'Oe').replace(/Ü/g,'Ue')
    .replace(/ä/g,'ae').replace(/ö/g,'oe').replace(/ü/g,'ue').replace(/ß/g,'ss')
    .normalize('NFKD').replace(/[\u0300-\u036f]/g,'').toLowerCase()
    .replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'').slice(0,70).replace(/-+$/g,'');
  if (!name) throw new Error('Der Name benötigt lateinische Buchstaben oder Ziffern.');
  return name;
}
export function repositoryVisibility(raw) {
  if (raw !== 'public' && raw !== 'private') {
    throw new Error('Bitte vor dem Erstellen Öffentlich oder Privat auswählen.');
  }
  return raw;
}

export function newAppUrl(rawBrief, ownerRaw, visibility = 'private') {
  const brief = normalizeBrief(rawBrief);
  const params = new URLSearchParams({
    owner: githubOwner(ownerRaw),
    name: repositorySlug(brief.title),
    description: (brief.goal || 'Neue App: ' + brief.title).slice(0,200),
    visibility: repositoryVisibility(visibility),
    template_owner: 'CobraChris',
    template_name: 'app-fabrik-template'
  });
  return 'https://github.com/new?' + params;
}
// Der geführte App-Start übernimmt nur den Namen und eine kurze Zielbeschreibung.
// Die ausführlichen Anforderungen bleiben im lokalen Ideenboard, bis der Nutzer
// sie bewusst als Entwicklungsauftrag weitergibt.
export function appDraftFromBrief(rawBrief) {
  const brief = normalizeBrief(rawBrief);
  return { name: brief.title, description: brief.goal.slice(0, 180) };
}

export function appSetupLinks(rawRepository) {
  const repository = githubRepository(rawRepository);
  const root = 'https://github.com/' + repository;
  const [owner, name] = repository.split('/');
  return {
    repository,
    template: repository.toLowerCase() === 'cobrachris/app-fabrik-template',
    pagesSettings: root + '/settings/pages',
    setupWorkflow: root + '/actions/workflows/setup-app.yml',
    actions: root + '/actions',
    possibleSite: 'https://' + owner.toLowerCase() + '.github.io/' + encodeURIComponent(name) + '/',
  };
}

export function issueText(rawBrief) {
  const b = normalizeBrief(rawBrief);
  const missing = '_Noch zu definieren_';
  return [
    '# App-Idee: ' + b.title,
    '## Ziel\n' + (b.goal || missing),
    '## MVP-Funktionen\n' + (b.features || missing),
    '## Abnahmekriterien\n' + (b.acceptance || missing),
    '## Rahmenbedingungen\n' + (b.constraints || 'Mobile PWA, keine kostenpflichtigen APIs ohne Freigabe.'),
    '## Priorität und Status\nPriorität: ' + b.priority + '\nStatus: ' + b.status
  ].join('\n\n') + '\n';
}
export function newIssueLink(rawBrief, rawRepo) {
  const brief = normalizeBrief(rawBrief);
  const repo = githubRepository(rawRepo);
  const base = 'https://github.com/' + repo + '/issues/new';
  const body = issueText(brief);
  const params = new URLSearchParams({ title: '[App-Idee] ' + brief.title, body });
  const filled = base + '?' + params;
  return { url: filled.length <= PREFILL_LIMIT ? filled : base,
    prefilled: filled.length <= PREFILL_LIMIT, body, repo };
}
