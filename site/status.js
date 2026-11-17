import { projectKey, parseProjects, sortProjects } from './projects.mjs';
import {
  MAX_API_REQUESTS_PER_PASS, githubApiUrls, parseRepository, parseWorkflowRuns,
  parseRelease, classifyRepoHttp, parseGithubRateLimit, canQueryGithub
} from './status-data.mjs';

const $ = selector => document.querySelector(selector);
const scope = new URL('./', import.meta.url).pathname;
const list = $('#status-project-list');
const allButton = $('#status-refresh-all');
const feedback = $('#status-feedback');
const cards = new Map();
let busy = false;
let projects = [];
let requestsThisPass = 0;
let remainingAtGithub = null;
let resetAtGithub = null;
let blockedUntil = 0;
const checkedRepos = new Set();

function resetBudget() {
  requestsThisPass = 0;
  if (resetAtGithub && Date.now() >= resetAtGithub) {
    remainingAtGithub = null;
    resetAtGithub = null;
    blockedUntil = 0;
  }
}

function updateBatchButton() {
  const remaining = projects.filter(p => !checkedRepos.has(p.repository.toLowerCase())).length;
  allButton.textContent = remaining
    ? 'Alle Projekte prüfen (' + remaining + ' noch offen)'
    : 'Alle Projekte erneut prüfen';
}

function make(tag, className, value) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (value !== undefined) el.textContent = value;
  return el;
}
function link(label, url) {
  const a = make('a', '', label);
  a.href = url;
  a.target = '_blank';
  a.rel = 'noopener noreferrer';
  return a;
}
function dateLabel(input) {
  if (!input) return '';
  const date = new Date(input);
  return Number.isNaN(date.valueOf()) ? '' :
    date.toLocaleString('de-DE', {dateStyle:'short',timeStyle:'short'});
}
function readProjects() {
  try {
    return sortProjects(parseProjects(localStorage.getItem(projectKey(scope)) || '[]'));
  } catch {
    feedback.textContent = 'Der lokale Projektspeicher ist nicht erreichbar.';
    return [];
  }
}

function render() {
  projects = readProjects();
  list.replaceChildren();
  cards.clear();
  $('#status-project-count').textContent = projects.length + ' lokal gespeicherte Projekte';
  $('#status-empty').hidden = projects.length > 0;
  for (const project of projects) {
    const urls = githubApiUrls(project.repository);
    const li = make('li','status-project-card');
    const head = make('div','status-project-head');
    head.append(make('strong','',project.repository),make('span','status-local-state',project.status+' · manuell'));
    const info = make('p','status-row','Noch nicht abgefragt');
    info.setAttribute('aria-live','polite');
    const branch = make('p','status-detail','Branch: noch nicht abgefragt');
    const workflow = make('p','status-detail','Letzter GitHub-Actions-Lauf: noch nicht abgefragt');
    const release = make('p','status-detail','GitHub Release: noch nicht abgefragt');
    const checked = make('p','status-checked','');
    const actions = make('div','status-card-actions');
    const reload = make('button','secondary-button','Aktualisieren');
    reload.type = 'button';
    reload.addEventListener('click',()=>refreshOne(project.repository));
    actions.append(reload,link('Repository',urls.web),link('Actions',urls.web+'/actions'),link('Releases',urls.web+'/releases'));
    li.append(head,info,branch,workflow,release,checked,actions);
    list.append(li);
    cards.set(project.repository,{info,branch,workflow,release,checked,reload});
  }
  updateBatchButton();
}

async function api(url) {
  if (!canQueryGithub({
    used:requestsThisPass,remaining:remainingAtGithub,blockedUntil
  })) {
    const err = Error('GitHub-API-Kontingent vorübergehend erschöpft');
    err.paused = true;
    throw err;
  }
  requestsThisPass++;
  const controller = new AbortController();
  const timeout = setTimeout(()=>controller.abort(),10000);
  try {
    const response = await fetch(url,{
      method:'GET',mode:'cors',credentials:'omit',redirect:'error',
      headers:{Accept:'application/vnd.github+json'},
      cache:'no-store',signal:controller.signal
    });
    const quota=parseGithubRateLimit(
      response.headers.get('x-ratelimit-remaining'),
      response.headers.get('x-ratelimit-reset')
    );
    if (quota.remaining !== null) remainingAtGithub = quota.remaining;
    if (quota.resetAt !== null) resetAtGithub = quota.resetAt;
    if (!response.ok) {
      const limited = response.status === 429 ||
        (response.status === 403 && (remainingAtGithub === 0 ||
          response.headers.get('retry-after') !== null));
      if (limited) {
        const retry = Number(response.headers.get('retry-after'));
        blockedUntil = Number.isFinite(retry) && retry > 0 && retry < 86400
          ? Date.now() + retry*1000
          : (resetAtGithub && resetAtGithub > Date.now()
            ? resetAtGithub : Date.now()+60000);
        const error = Error('GitHub-API-Limit erreicht');
        error.paused = true;
        throw error;
      }
      const err = Error(classifyRepoHttp(response.status));
      err.status = response.status;
      throw err;
    }
    return await response.json();
  } finally { clearTimeout(timeout); }
}

async function refreshCard(repo) {
  const card=cards.get(repo);
  if (!card) return;
  const {info,branch,workflow,release,checked,reload}=card;
  reload.disabled = true;
  info.textContent = 'GitHub wird abgefragt …';
  branch.textContent = 'Branch: wird geprüft';
  workflow.textContent = 'GitHub Actions: wird geprüft';
  release.textContent = 'GitHub Release: wird geprüft';
  const urls=githubApiUrls(repo);
  let paused = false;
  try {
    const metadata=parseRepository(await api(urls.repository),repo);
    branch.textContent='Branch: '+metadata.branch +
      (metadata.updatedAt ? ' · letzter Push '+dateLabel(metadata.updatedAt) : '');
    // Sequential requests respect the live rate-limit headers; each partial error remains visible.
    const outcomes=[];
    for (const url of [githubApiUrls(repo,metadata.branch).workflows,urls.releases]) {
      try {
        outcomes.push({status:'fulfilled',value:await api(url)});
      } catch (error) {
        outcomes.push({status:'rejected',reason:error});
        if (error.paused) break;
      }
    }
    if (outcomes[0]?.status==='fulfilled') {
      try {
        const run=parseWorkflowRuns(outcomes[0].value,repo,metadata.branch);
        workflow.replaceChildren(document.createTextNode('Letzter Actions-Lauf: '+run.label +
          (run.workflow?' · '+run.workflow:'') + (run.timestamp?' · '+dateLabel(run.timestamp):'')+' '));
        if (run.url) workflow.append(link('Lauf öffnen',run.url));
        info.textContent=run.label;
        info.dataset.kind=run.kind;
      } catch(e) {
        workflow.textContent='GitHub Actions: '+e.message;
        info.textContent='Nicht vollständig prüfbar';
        info.dataset.kind='unknown';
      }
    } else {
      workflow.textContent='GitHub Actions: Abfrage fehlgeschlagen';
      info.textContent='Nicht vollständig prüfbar';
      info.dataset.kind='unknown';
    }
    if (outcomes[1]?.status==='fulfilled') {
      try {
        const rel=parseRelease(outcomes[1].value,repo);
        release.replaceChildren(document.createTextNode('GitHub Release: '+rel.label+' '));
        if (rel.url) release.append(link('Release öffnen',rel.url));
      } catch(e) { release.textContent='GitHub Release: '+e.message; }
    } else release.textContent=outcomes.length<2 || outcomes[1]?.reason?.paused
      ? 'GitHub Release: wegen API-Pause nicht geprüft'
      : 'GitHub Release: Abfrage fehlgeschlagen';
    if (outcomes.some(x=>x.status==='rejected' && x.reason?.paused)) {
      paused = true;
      info.textContent = 'API-Pause: später fortsetzen';
      info.dataset.kind = 'unknown';
    }
  } catch(e) {
    paused = Boolean(e.paused);
    const privateOrMissing=e.status===404;
    info.textContent=paused
      ? 'API-Pause: später fortsetzen'
      : (privateOrMissing ? 'Privat oder nicht öffentlich abrufbar'
        : (e.name==='AbortError'?'Zeitüberschreitung':e.message));
    info.dataset.kind='unknown';
    branch.textContent='Branch: nicht geprüft';
    workflow.textContent='GitHub Actions: nur nach GitHub-Anmeldung sichtbar';
    release.textContent='GitHub Release: nicht geprüft';
  } finally {
    checked.textContent='Abfrage am '+dateLabel(new Date().toISOString()) +
      ' · keine privaten Zugangsdaten verwendet';
    reload.disabled=busy;
  }
  return {paused};
}

async function refreshOne(repo) {
  if (busy) return;
  busy = true;
  allButton.disabled = true;
  resetBudget();
  try {
    const result = await refreshCard(repo);
    if (!result.paused) checkedRepos.add(repo.toLowerCase());
    feedback.textContent = result.paused
      ? 'GitHub-API-Pause. Später erneut versuchen; bisherige Ergebnisse bleiben sichtbar.'
      : 'Projekt abgefragt. Private Repository-Daten bleiben ohne Anmeldung geschützt.';
  } finally {
    busy = false;
    allButton.disabled = false;
    for (const card of cards.values()) card.reload.disabled = false;
    updateBatchButton();
  }
}

allButton.addEventListener('click',async()=>{
  if (busy || !projects.length) return;
  busy = true;
  allButton.disabled = true;
  resetBudget();
  let pending = projects.filter(p=>!checkedRepos.has(p.repository.toLowerCase()));
  if (!pending.length) {
    checkedRepos.clear();
    pending = [...projects];
  }
  let completed = 0;
  let paused = false;
  try {
    for (const project of pending) {
      const result = await refreshCard(project.repository);
      if (result.paused) { paused = true; break; }
      checkedRepos.add(project.repository.toLowerCase());
      completed++;
      feedback.textContent = completed + ' von ' + pending.length +
        ' Projekten in dieser Runde abgefragt …';
    }
    const left = projects.length - checkedRepos.size;
    feedback.textContent = paused
      ? 'GitHub-API-Pause nach ' + completed + ' Abfragen in dieser Runde. ' +
        left + ' Projekte verbleiben. Über „Projekte prüfen“ später fortsetzen.'
      : 'Diese Runde abgeschlossen: ' + completed + ' Projekte abgefragt. ' +
        (left ? left + ' Projekte später fortsetzen.' : 'Alle Projekte bearbeitet.') +
        ' Private Repositories sind ohne Anmeldung nicht live auslesbar.';
  } catch(e) {
    feedback.textContent = 'Abfrage unterbrochen: ' + e.message +
      '. Bisherige Ergebnisse bleiben erhalten.';
  } finally {
    busy = false;
    allButton.disabled = false;
    for (const card of cards.values()) card.reload.disabled = false;
    updateBatchButton();
  }
});

render();
