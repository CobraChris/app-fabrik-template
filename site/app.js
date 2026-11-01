import { STORE_KEY, parseIdeas } from './core.mjs';
import { newAppUrl, newIssueLink, ownerFromHostname } from './github-links.mjs';
import { projectKey, upsertProject, deleteProject, sortProjects, openProjectsCount, projectLinks, parseProjects, exportProjects, importProjects, addProjectTask, toggleProjectTask, removeProjectTask, openTasksCount, focusActions } from './projects.mjs';
import { publicRepositoriesUrl, parsePublicRepositories, mergePublicRepositories } from './github-import.mjs';
import { quickstartState } from './quickstart.mjs';
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
const ownerInput = $('#github-owner');
const repoInput = $('#github-repository');
const visibilityInput = $('#github-visibility');
const githubKey = 'app-fabrik-github-v1:' + scopePath;

function loadGitHubSettings() {
  ownerInput.value = ownerFromHostname(location.hostname);
  try {
    const saved = JSON.parse(localStorage.getItem(githubKey) || '{}');
    if (typeof saved.owner === 'string') ownerInput.value = saved.owner;
    if (typeof saved.repository === 'string') repoInput.value = saved.repository;
    if (saved.visibility === 'public' || saved.visibility === 'private') visibilityInput.value = saved.visibility;
  } catch { /* GitHub-Einstellungen sind optional. */ }
}
function saveGitHubSettings() {
  try { localStorage.setItem(githubKey, JSON.stringify({
    owner: ownerInput.value.trim(), repository: repoInput.value.trim(),
    visibility: visibilityInput.value
  })); } catch { /* Kein GitHub-Login erforderlich. */ }
}
ownerInput.addEventListener('change', saveGitHubSettings);
repoInput.addEventListener('change', saveGitHubSettings);
visibilityInput.addEventListener('change', saveGitHubSettings);

function showGitHubBackup(url, body) {
  $('#copy-fallback').hidden = false;
  $('#copy-text').value = body || '';
  const fallback = $('#github-fallback-link');
  fallback.href = url;
  fallback.hidden = false;
}

function startNewApp(brief) {
  try {
    if (!visibilityInput.value) {
      $('#github-settings').open = true;
      $('#github-settings').scrollIntoView({ behavior: 'smooth', block: 'center' });
      visibilityInput.focus({ preventScroll: true });
      notify('Bitte unter GitHub-Verknüpfung die Sichtbarkeit wählen. Öffentlich ist für GitHub Pages im kostenlosen Tarif erforderlich.');
      return;
    }
    const visibility = visibilityInput.value;
    const url = newAppUrl(brief, ownerInput.value, visibility);
    const warning = visibility === 'public'
      ? 'ÖFFENTLICH: Repository, Quellcode und eingetragene Daten sind für alle sichtbar. GitHub Pages kann mit GitHub Free genutzt werden. Keine vertraulichen Inhalte hochladen.'
      : 'PRIVAT: Repository ist nicht öffentlich. GitHub Pages lässt sich damit im kostenlosen GitHub-Tarif nicht verwenden; dafür ist ein unterstützter kostenpflichtiger GitHub-Tarif erforderlich.';
    if (!confirm(warning + '\n\nGitHub zur Vorbereitung öffnen? Du erstellst das Repository erst dort selbst.')) return;
    saveGitHubSettings();
    showGitHubBackup(url, '');
    window.open(url, '_blank', 'noopener,noreferrer');
    notify(visibility === 'public'
      ? 'GitHub geöffnet: Vor dem Erstellen prüfen, ob Öffentlich eingestellt ist. Quellcode ist dann für alle sichtbar.'
      : 'GitHub geöffnet: Vor dem Erstellen Privat prüfen. GitHub Pages ist bei GitHub Free für private Repositories nicht verfügbar.');
  } catch (error) { notify(error.message); }
}

function startGitHubIssue(brief) {
  try {
    const issue = newIssueLink(brief, repoInput.value);
    if (!confirm('Issue-Entwurf im Repository ' + issue.repo + ' öffnen? ' +
      'Der Steckbrief wird als URL-Parameter an GitHub übermittelt. ' +
      'Öffentliche Issues sind nach dem Absenden für alle sichtbar.')) return;
    saveGitHubSettings();
    showGitHubBackup(issue.url, issue.body);
    if (navigator.clipboard?.writeText) navigator.clipboard.writeText(issue.body).catch(() => {});
    window.open(issue.url, '_blank', 'noopener,noreferrer');
    notify(issue.prefilled ?
      'GitHub-Issue geöffnet. Bitte Inhalt und Sichtbarkeit prüfen. Falls die Handy-App Felder verliert, Text unten kopieren.' :
      'Steckbrief zu lang für die URL: GitHub wurde leer geöffnet. Bitte den vollständigen Text unten kopieren.');
  } catch (error) { notify(error.message); }
}

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
    $('#github-fallback-link').hidden = true;
    notify('Entwicklungsauftrag kopiert. Füge ihn jetzt in den Chat ein.');
  } catch {
    $('#copy-text').value = value;
    $('#copy-fallback').hidden = false;
    $('#github-fallback-link').hidden = true;
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
    const extra = document.createElement('details');
    extra.className = 'idea-github-actions';
    const summary = document.createElement('summary');
    summary.textContent = 'Mit GitHub weiterarbeiten';
    const options = document.createElement('div');
    options.className = 'idea-actions';
    options.append(
      actionButton('Neue App aus Vorlage', () => startNewApp(brief), brief.title + ' als neue App vorbereiten'),
      actionButton('GitHub-Issue öffnen', () => startGitHubIssue(brief), brief.title + ' als GitHub-Issue vorbereiten')
    );
    extra.append(summary, options);
    li.append(extra);
    list.append(li);
  }
  renderQuickstart();
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


// Eigenständige lokale Projektübersicht. Kein GitHub-Token und keine API-Abfragen.
const projectStorage = projectKey(scopePath);
const projectForm = $('#project-form');
const projectFields = {
  id: $('#project-id'),
  repository: $('#project-repository'),
  nextStep: $('#project-next-step'),
  notes: $('#project-notes'),
  status: $('#project-status'),
};
let projects = [];
let projectsStorageAvailable = true;
const projectNotify = message => { $('#project-feedback').textContent = message; };

function saveProjects() {
  if (!projectsStorageAvailable) return;
  try { localStorage.setItem(projectStorage, JSON.stringify(projects)); }
  catch {
    projectsStorageAvailable = false;
    projectNotify('Gerätespeicher blockiert. Projekt-Backup exportieren; Änderungen gehen beim Neuladen verloren.');
  }
}

function loadProjects() {
  try {
    const raw = localStorage.getItem(projectStorage);
    if (raw !== null) projects = parseProjects(raw);
    if (raw && projects.length === 0 && raw !== '[]') {
      projectNotify('Projekt-Daten konnten nicht gelesen werden. Vorhandenes Backup prüfen.');
    }
  } catch {
    projectsStorageAvailable = false;
    projectNotify('Projekte können nur bis zum Neuladen angezeigt werden. Bitte Browser-Speicher prüfen.');
  }
}

function resetProjectForm() {
  projectForm.reset();
  projectFields.id.value = '';
  $('#project-save').textContent = 'Projekt speichern';
  $('#project-form-heading').textContent = '+ GitHub-Projekt hinzufügen';
  $('#project-editor').open = false;
}

function editProject(project) {
  for (const [name, input] of Object.entries(projectFields)) input.value = project[name] || '';
  $('#project-save').textContent = 'Änderungen speichern';
  $('#project-form-heading').textContent = 'Projekt bearbeiten';
  $('#project-editor').open = true;
  projectFields.nextStep.focus();
  $('#project-editor').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function githubLink(title, url) {
  const link = document.createElement('a');
  link.textContent = title;
  link.href = url;
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  return link;
}

// Jede Fokuszeile ist ein echtes lokales Projekt-To-do oder ein verlinkter
// manuell hinterlegter nächster Schritt. Keine Daten aus GitHub werden geraten.
function renderFocus() {
  const list = $('#focus-list');
  list.replaceChildren();
  const actions = focusActions(projects, 8);
  $('#focus-count').textContent = actions.length + ' angezeigt';
  $('#focus-empty').hidden = actions.length !== 0;
  for (const action of actions) {
    const li = document.createElement('li');
    li.className = 'focus-item';
    const body = document.createElement('div');
    body.className = 'focus-body';
    const repo = document.createElement('span');
    repo.className = 'focus-repo';
    repo.textContent = action.repository + ' · ' + action.status;
    const task = document.createElement('strong');
    task.className = 'focus-task';
    task.textContent = action.title;
    body.append(repo, task);
    const controls = document.createElement('div');
    controls.className = 'focus-controls';
    const github = document.createElement('a');
    github.textContent = 'GitHub';
    github.href = projectLinks(projects.find(p => p.id === action.projectId)).repository;
    github.target = '_blank';
    github.rel = 'noopener noreferrer';
    controls.append(github);
    if (action.kind === 'todo') {
      const done = document.createElement('button');
      done.type = 'button';
      done.textContent = 'Erledigt';
      done.setAttribute('aria-label', action.title + ' für ' + action.repository + ' als erledigt markieren');
      done.addEventListener('click', () => {
        try {
          // Projekt beim Klick neu ermitteln, damit keine veraltete Kopie genutzt wird.
          const current = projects.find(p => p.id === action.projectId);
          if (!current) throw new Error('Projekt nicht mehr vorhanden.');
          projects = upsertProject(projects, toggleProjectTask(current, action.taskId, true));
          saveProjects();
          renderProjects();
          if (projectsStorageAvailable) projectNotify('Aufgabe lokal erledigt. GitHub wurde nicht geändert.');
        } catch (error) { projectNotify(error.message); }
      });
      controls.append(done);
    } else {
      const edit = document.createElement('button');
      edit.type = 'button';
      edit.textContent = 'Bearbeiten';
      edit.addEventListener('click', () => {
        const project = projects.find(p => p.id === action.projectId);
        if (project) editProject(project);
      });
      controls.append(edit);
    }
    li.append(body, controls);
    list.append(li);
  }
}

// Abruf öffentlicher Repository-Metadaten ausschließlich nach einem Klick.
let publicRepoCandidates = [];
const importOwner = $('#repo-import-owner');
const importStatus = $('#repo-import-status');
const importList = $('#repo-import-results');
const importAdd = $('#repo-import-add');

function renderRepoImport() {
  importList.replaceChildren();
  let selectable = 0;
  for (const entry of publicRepoCandidates) {
    const existing = projects.some(p => p.repository.toLowerCase() === entry.repository.toLowerCase());
    const item = document.createElement('li');
    item.className = 'repo-import-item';
    const label = document.createElement('label');
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.value = entry.repository;
    checkbox.disabled = existing;
    if (!existing) selectable++;
    const content = document.createElement('span');
    const name = document.createElement('strong');
    name.textContent = entry.repository;
    content.append(name);
    const details = document.createElement('span');
    details.className = 'repo-import-info';
    details.textContent = existing ? 'Bereits erfasst' :
      [entry.description, entry.fork ? 'Fork' : ''].filter(Boolean).join(' · ') || 'Öffentliches Repository';
    content.append(details);
    label.append(checkbox, content);
    item.append(label);
    importList.append(item);
  }
  importAdd.disabled = true;
  importAdd.hidden = selectable === 0;
  if (publicRepoCandidates.length) {
    importStatus.textContent = publicRepoCandidates.length + ' öffentliche Repositories gefunden, ' +
      selectable + ' noch nicht erfasst.' +
      (publicRepoCandidates.length === 100 ? ' Maximal die 100 zuletzt aktualisierten Repositories werden angezeigt.' : '');
  }
}

importOwner.value = ownerFromHostname(location.hostname);
$('#repo-import-form').addEventListener('submit', async event => {
  event.preventDefault();
  const button = $('#repo-import-search');
  let url;
  try { url = publicRepositoriesUrl(importOwner.value); }
  catch (error) { importStatus.textContent = error.message; return; }
  publicRepoCandidates = [];
  importList.replaceChildren();
  importAdd.disabled = true;
  importAdd.hidden = true;
  button.disabled = true;
  importStatus.textContent = 'Öffentliche GitHub-Repositories werden geladen …';
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      credentials: 'omit',
      cache: 'no-store',
      referrerPolicy: 'no-referrer',
      headers: { Accept: 'application/vnd.github+json' }
    });
    if (!response.ok) {
      if (response.status === 403 || response.status === 429) {
        throw new Error('GitHub-Limit erreicht. Später erneut versuchen oder Repository manuell ergänzen.');
      }
      if (response.status === 404) throw new Error('GitHub-Konto nicht gefunden.');
      throw new Error('GitHub konnte nicht geladen werden (HTTP ' + response.status + ').');
    }
    const data = await response.json();
    publicRepoCandidates = parsePublicRepositories(data, importOwner.value);
    renderRepoImport();
    if (!publicRepoCandidates.length) importStatus.textContent =
      'Keine öffentlichen, nicht archivierten Repositories auf der ersten Seite gefunden.';
  } catch (error) {
    importStatus.textContent = error.name === 'AbortError' ?
      'GitHub hat nicht rechtzeitig geantwortet. Bitte erneut versuchen.' :
      error.message || 'GitHub-Abfrage fehlgeschlagen. Bitte Internetverbindung prüfen.';
  } finally {
    clearTimeout(timeout);
    button.disabled = false;
  }
});

importList.addEventListener('change', () => {
  importAdd.disabled = !importList.querySelector('input[type="checkbox"]:checked');
});

importAdd.addEventListener('click', () => {
  const selected = [...importList.querySelectorAll('input[type="checkbox"]:checked')].map(x => x.value);
  if (!selected.length) return;
  try {
    const createId = () => globalThis.crypto?.randomUUID?.() ??
      ('project-' + Date.now() + '-' + Math.random().toString(36).slice(2));
    const result = mergePublicRepositories(projects, publicRepoCandidates, selected, createId);
    if (!result.added) { importStatus.textContent = 'Alle ausgewählten Repositories sind bereits erfasst.'; return; }
    projects = result.projects;
    saveProjects();
    renderProjects();
    if (projectsStorageAvailable) projectNotify(result.added + ' Projekte importiert. Nächste Schritte kannst du über Bearbeiten ergänzen.');
  } catch (error) { importStatus.textContent = error.message; }
});

// Kein zusätzlicher Haken / kein zweiter Aufgabenstand: der Schnellstart ist
// eine abgeleitete Ansicht der bereits lokal gespeicherten Projekte und Ideen.
function renderQuickstart() {
  const state = quickstartState(projects, briefs);
  $('#quickstart-title').textContent = state.title;
  $('#quickstart-description').textContent = state.description;
  const button = $('#quickstart-action');
  button.textContent = state.button;
  button.dataset.target = state.target;
  const s = state.summary;
  $('#quickstart-summary').textContent = s.projects + ' Projekte · ' +
    s.ideas + ' Ideen · ' + s.openTodos + ' offene To-dos';
}

$('#quickstart-action').addEventListener('click', () => {
  const target = $('#quickstart-action').dataset.target;
  const element = target && document.getElementById(target);
  if (!element) return;
  if (element.tagName === 'DETAILS') element.open = true;
  element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  if (target === 'idea-title') element.focus({ preventScroll: true });
  if (target === 'repo-import-panel') $('#repo-import-owner').focus({ preventScroll: true });
});

function renderProjects() {
  const list = $('#project-list');
  list.replaceChildren();
  const openCount = openProjectsCount(projects);
  const openTodoCount = projects.reduce((total, project) => total + openTasksCount(project.todos), 0);
  $('#project-count').textContent = openCount + ' Projekte · ' + openTodoCount + ' To-dos';
  $('#project-empty').hidden = projects.length !== 0;
  for (const project of sortProjects(projects)) {
    const links = projectLinks(project);
    const li = document.createElement('li');
    li.className = 'project-card';
    li.id = 'project-card-' + project.id;
    const head = document.createElement('div');
    head.className = 'project-card-head';
    const name = document.createElement('strong');
    name.textContent = project.repository;
    const status = document.createElement('span');
    status.textContent = project.status;
    status.className = 'project-state';
    head.append(name, status);
    li.append(head);
    const taskLabel = document.createElement('span');
    taskLabel.className = 'project-next-label';
    taskLabel.textContent = 'NÄCHSTER SCHRITT';
    const task = document.createElement('p');
    task.className = 'project-next-text';
    task.textContent = project.nextStep || 'Noch keinen nächsten Schritt hinterlegt';
    li.append(taskLabel, task);

    const todoSection = document.createElement('section');
    todoSection.className = 'project-todo-section';
    todoSection.setAttribute('aria-label', 'Aufgaben für ' + project.repository);
    const todoHeader = document.createElement('div');
    todoHeader.className = 'project-todo-header';
    const todoTitle = document.createElement('strong');
    todoTitle.textContent = 'To-dos';
    const progress = document.createElement('span');
    progress.textContent = openTasksCount(project.todos) + ' offen von ' + project.todos.length;
    todoHeader.append(todoTitle, progress);
    todoSection.append(todoHeader);

    const todosList = document.createElement('ul');
    todosList.className = 'project-todo-list';
    for (const todo of [...project.todos].sort((a, b) => Number(a.done) - Number(b.done))) {
      const item = document.createElement('li');
      item.className = 'project-todo-item';
      const label = document.createElement('label');
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = todo.done;
      checkbox.setAttribute('aria-label', todo.title + ' als ' + (todo.done ? 'offen' : 'erledigt') + ' markieren');
      const title = document.createElement('span');
      title.textContent = todo.title;
      if (todo.done) title.className = 'project-todo-completed';
      checkbox.addEventListener('change', () => {
        try {
          projects = upsertProject(projects, toggleProjectTask(project, todo.id, checkbox.checked));
          saveProjects(); renderProjects();
        } catch (error) { projectNotify(error.message); }
      });
      label.append(checkbox, title);
      const remove = actionButton('Entfernen', () => {
        if (!confirm('Aufgabe "' + todo.title + '" aus der lokalen Liste entfernen?')) return;
        projects = upsertProject(projects, removeProjectTask(project, todo.id));
        saveProjects(); renderProjects();
        projectNotify('Aufgabe entfernt. GitHub bleibt unverändert.');
      }, todo.title + ' entfernen');
      item.append(label, remove);
      todosList.append(item);
    }
    todoSection.append(todosList);

    const todoForm = document.createElement('form');
    todoForm.className = 'project-todo-form';
    const input = document.createElement('input');
    input.type = 'text';
    input.maxLength = 240;
    input.placeholder = 'Neue Aufgabe, z. B. PR prüfen';
    input.required = true;
    input.setAttribute('aria-label', 'Neue Aufgabe für ' + project.repository);
    const add = document.createElement('button');
    add.type = 'submit';
    add.textContent = '+ Aufgabe';
    todoForm.append(input, add);
    todoForm.addEventListener('submit', event => {
      event.preventDefault();
      try {
        const id = globalThis.crypto?.randomUUID?.() ??
          ('todo-' + Date.now() + '-' + Math.random().toString(36).slice(2));
        projects = upsertProject(projects, addProjectTask(project, input.value, id));
        saveProjects(); renderProjects();
        if (projectsStorageAvailable) projectNotify('Aufgabe hinzugefügt.');
      } catch (error) { projectNotify(error.message); input.focus(); }
    });
    todoSection.append(todoForm);
    li.append(todoSection);
    if (project.notes) {
      const notes = document.createElement('p');
      notes.className = 'project-notes';
      notes.textContent = project.notes;
      li.append(notes);
    }
    const navigation = document.createElement('div');
    navigation.className = 'project-links';
    navigation.append(
      githubLink('Repository', links.repository),
      githubLink('Pull Requests', links.pulls),
      githubLink('Issues', links.issues),
      githubLink('Actions', links.actions)
    );
    li.append(navigation);
    const controls = document.createElement('div');
    controls.className = 'project-controls';
    controls.append(
      actionButton('Bearbeiten', () => editProject(project), project.repository + ' bearbeiten'),
      actionButton('Entfernen', () => {
        if (!confirm('Projekt "' + project.repository + '" aus der lokalen Übersicht entfernen?')) return;
        projects = deleteProject(projects, project.id);
        saveProjects(); renderProjects(); projectNotify('Projekt aus der Übersicht entfernt. GitHub bleibt unverändert.');
      }, project.repository + ' aus Übersicht entfernen')
    );
    li.append(controls);
    list.append(li);
  }
  renderFocus();
  if (publicRepoCandidates.length) renderRepoImport();
  renderQuickstart();
}

projectForm.addEventListener('submit', event => {
  event.preventDefault();
  const id = projectFields.id.value ||
    (globalThis.crypto?.randomUUID?.() ?? 'project-' + Date.now() + '-' + Math.random().toString(36).slice(2));
  const input = Object.fromEntries(Object.entries(projectFields).map(([name, field]) =>
    [name, name === 'id' ? id : field.value]));
  try {
    projects = upsertProject(projects, input);
    saveProjects();
    resetProjectForm();
    renderProjects();
    if (projectsStorageAvailable) projectNotify('Projekt gespeichert.');
  } catch (error) { projectNotify(error.message); }
});
$('#project-cancel').addEventListener('click', () => resetProjectForm());
$('#project-export').addEventListener('click', () => {
  const json = exportProjects(projects);
  const url = URL.createObjectURL(new Blob([json], { type: 'application/json;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = 'app-fabrik-projekte.json';
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  projectNotify('Projekt-Backup erstellt.');
});
$('#project-import').addEventListener('click', () => $('#project-file').click());
$('#project-file').addEventListener('change', async event => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  if (file.size > 1024 * 1024) { projectNotify('Backup zu groß (maximal 1 MB).'); return; }
  try {
    const imported = importProjects(await file.text());
    if (projects.length && !confirm('Alle lokalen Projekte mit den Einträgen aus diesem Backup ersetzen?')) return;
    projects = imported;
    saveProjects();
    resetProjectForm();
    renderProjects();
    if (projectsStorageAvailable) projectNotify('Projekt-Backup wiederhergestellt: ' + projects.length + ' Projekte.');
  } catch (error) { projectNotify(error.message); }
});

loadGitHubSettings(); load(); render();
loadProjects(); renderProjects();
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}
