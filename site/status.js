import { projectKey, parseProjects, sortProjects } from './projects.mjs';
import {
  MAX_BATCH, githubApiUrls, parseRepository, parseWorkflowRuns,
  parseRelease, classifyRepoHttp
} from './status-data.mjs';

const $ = selector => document.querySelector(selector);
const scope = new URL('./', import.meta.url).pathname;
const list = $('#status-project-list');
const allButton = $('#status-refresh-all');
const feedback = $('#status-feedback');
const cards = new Map();
let busy = false;
let projects = [];

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
}

async function api(url) {
  const controller = new AbortController();
  const timeout = setTimeout(()=>controller.abort(),10000);
  try {
    const response = await fetch(url,{
      method:'GET',mode:'cors',credentials:'omit',redirect:'error',
      headers:{Accept:'application/vnd.github+json'},
      cache:'no-store',signal:controller.signal
    });
    if (!response.ok) {
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
  try {
    const metadata=parseRepository(await api(urls.repository),repo);
    branch.textContent='Branch: '+metadata.branch +
      (metadata.updatedAt ? ' · letzter Push '+dateLabel(metadata.updatedAt) : '');
    // Partial failures must be visible; never convert them to a green status.
    const outcomes=await Promise.allSettled([
      api(githubApiUrls(repo,metadata.branch).workflows),
      api(urls.releases)
    ]);
    if (outcomes[0].status==='fulfilled') {
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
    if (outcomes[1].status==='fulfilled') {
      try {
        const rel=parseRelease(outcomes[1].value,repo);
        release.replaceChildren(document.createTextNode('GitHub Release: '+rel.label+' '));
        if (rel.url) release.append(link('Release öffnen',rel.url));
      } catch(e) { release.textContent='GitHub Release: '+e.message; }
    } else release.textContent='GitHub Release: Abfrage fehlgeschlagen';
  } catch(e) {
    const privateOrMissing=e.status===404;
    info.textContent=privateOrMissing?
      'Privat oder nicht öffentlich abrufbar':(e.name==='AbortError'?'Zeitüberschreitung':e.message);
    info.dataset.kind='unknown';
    branch.textContent='Branch: nicht geprüft';
    workflow.textContent='GitHub Actions: nur nach GitHub-Anmeldung sichtbar';
    release.textContent='GitHub Release: nicht geprüft';
  } finally {
    checked.textContent='Abfrage am '+dateLabel(new Date().toISOString()) +
      ' · keine privaten Zugangsdaten verwendet';
    reload.disabled=busy;
  }
}

async function refreshOne(repo) {
  if (busy) return;
  busy=true;allButton.disabled=true;
  await refreshCard(repo);
  busy=false;allButton.disabled=false;
}

allButton.addEventListener('click',async()=>{
  if (busy || !projects.length) return;
  busy=true; allButton.disabled=true;
  const selected=projects.slice(0,MAX_BATCH);
  feedback.textContent='Prüfe '+selected.length+' Projekte über die öffentliche GitHub-API …';
  // Bounded concurrency limits unauthenticated API usage and mobile traffic.
  for (let i=0;i<selected.length;i+=2) {
    await Promise.all(selected.slice(i,i+2).map(p=>refreshCard(p.repository)));
  }
  feedback.textContent=selected.length+' Projekte geprüft. '+
    (projects.length>MAX_BATCH?'Weitere Projekte einzeln aktualisieren. ':'')+
    'Private Repositories werden ohne Token nicht ausgelesen.';
  busy=false;allButton.disabled=false;
  for(const card of cards.values())card.reload.disabled=false;
});

render();
