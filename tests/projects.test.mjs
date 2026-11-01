import test from 'node:test';
import assert from 'node:assert/strict';
import {
  PROJECT_LIMIT, TASK_LIMIT, projectKey, normalizeProject, upsertProject,
  deleteProject, sortProjects, openProjectsCount, projectLinks,
  parseProjects, importProjects, exportProjects,
  normalizeTask, addProjectTask, toggleProjectTask, removeProjectTask, openTasksCount
} from '../site/projects.mjs';

const project = {
  id: 'p1', repository: 'CobraChris/meine-livekarte',
  status: 'In Arbeit', nextStep: 'Route im Pop-up behalten',
  notes: 'Keine unnötigen API-Abonnements.'
};

test('Jedes Projektboard hat einen eigenen lokalen Schlüssel', () => {
  assert.notEqual(projectKey('/app-fabrik-template/'), projectKey('/meine-livekarte/'));
  assert.throws(() => projectKey('../outside'), /App-Pfad/);
});

test('GitHub-Repositories werden streng geprüft', () => {
  assert.equal(normalizeProject(project).repository, 'CobraChris/meine-livekarte');
  assert.throws(() => normalizeProject({ ...project, repository: 'https://evil.test' }));
  assert.throws(() => normalizeProject({ ...project, id: '../bad' }), /Projekt-ID/);
  assert.equal(normalizeProject({ ...project, status: 'Unsicher' }).status, 'Offen');
});

test('Projekt ergänzen, bearbeiten und löschen', () => {
  let items = upsertProject([], project);
  assert.equal(items.length, 1);
  items = upsertProject(items, { ...project, nextStep: 'Neue Aufgabe' });
  assert.equal(items.length, 1);
  assert.equal(items[0].nextStep, 'Neue Aufgabe');
  assert.deepEqual(deleteProject(items, 'p1'), []);
});

test('Repository-Duplikate unterscheiden keine Groß-/Kleinschreibung', () => {
  assert.throws(() => upsertProject([project], { ...project, id: 'p2',
    repository: 'cobrachris/MEINE-LIVEKARTE' }), /bereits/);
});

test('Offene Projekte zuerst anzeigen und korrekt zählen', () => {
  const items = [
    { ...project, id:'done', status:'Fertig' },
    { ...project, id:'open', repository:'CobraChris/other', status:'Offen' },
    { ...project, id:'doing', repository:'CobraChris/test', status:'In Arbeit' },
  ];
  assert.equal(openProjectsCount(items), 2);
  assert.deepEqual(sortProjects(items).map(x => x.id), ['doing', 'open', 'done']);
});

test('GitHub-Aktionen öffnen nur das validierte Repository', () => {
  const links = projectLinks(project);
  assert.equal(links.repository, 'https://github.com/CobraChris/meine-livekarte');
  assert.equal(links.pulls, links.repository + '/pulls');
  assert.equal(links.issues, links.repository + '/issues');
  assert.equal(links.actions, links.repository + '/actions');
  assert.equal(links.pages, links.repository + '/settings/pages');
});

test('Backup importieren und exportieren einschließlich nächsten Schritts', () => {
  assert.deepEqual(importProjects(exportProjects([project])), [normalizeProject(project)]);
  assert.deepEqual(parseProjects(JSON.stringify([project, {...project, id:'bad!'}])), [normalizeProject(project)]);
  assert.throws(() => importProjects('{}'), /Projekt-Backup/);
  assert.throws(() => importProjects('not json'), /JSON-Datei/);
  assert.throws(() => importProjects(JSON.stringify({schemaVersion:1, projects:[project,project]})), /doppelte/);
});

test('Projektanzahl und Notizlängen bleiben begrenzt', () => {
  const items = Array.from({length:PROJECT_LIMIT},(_,i)=>({...project,id:'id'+i,repository:'u'+i+'/repo'}));
  assert.throws(() => upsertProject(items, {...project,id:'other',repository:'other/repo'}), /Maximal/);
  assert.equal(normalizeProject({...project,notes:'a'.repeat(1300)}).notes.length,1000);
  assert.equal(normalizeProject({...project,nextStep:'a'.repeat(500)}).nextStep.length,350);
});

test('Bestehende Projekt-Backups im Format v1 bleiben importierbar', () => {
  const previous = JSON.stringify({schemaVersion:1, projects:[project]});
  const restored = importProjects(previous);
  assert.equal(restored.length, 1);
  assert.equal(restored[0].nextStep, project.nextStep);
  assert.deepEqual(restored[0].todos, []);
});

test('Mehrere Aufgaben pro Projekt anlegen und einzeln abhaken', () => {
  let updated = addProjectTask(project, 'PR #6 überprüfen', 'todo1');
  updated = addProjectTask(updated, 'Tests ausführen', 'todo2');
  assert.equal(openTasksCount(updated.todos), 2);
  updated = toggleProjectTask(updated, 'todo1', true);
  assert.equal(openTasksCount(updated.todos), 1);
  assert.equal(updated.todos[0].done, true);
  updated = toggleProjectTask(updated, 'todo1', false);
  assert.equal(openTasksCount(updated.todos), 2);
  updated = removeProjectTask(updated, 'todo2');
  assert.equal(updated.todos.length, 1);
  assert.equal(updated.nextStep, project.nextStep);
});

test('Doppelte und ungültige Aufgaben werden abgewiesen', () => {
  assert.throws(() => normalizeTask({id:'x',title:' ',done:false}), /ungültig/);
  assert.throws(() => normalizeTask({id:'../evil',title:'X',done:false}), /ungültig/);
  assert.throws(() => normalizeTask({id:'ok',title:'X',done:'false'}), /Aufgabenstatus/);
  const updated = addProjectTask(project, '  Testen  ', 'a');
  assert.throws(() => addProjectTask(updated, 'testen', 'b'), /existiert/);
  assert.throws(() => toggleProjectTask(updated, 'unbekannt', true), /nicht gefunden/);
  assert.throws(() => normalizeProject({...project, todos:[{id:'x',title:'A',done:false},{id:'x',title:'B',done:false}]}), /Doppelte/);
});

test('Maximal 50 Aufgaben pro Projekt und vollständiger Backup-Roundtrip', () => {
  let updated = normalizeProject(project);
  updated.todos = Array.from({length:TASK_LIMIT}, (_,i) => ({
    id:'todo'+i,title:'Aufgabe '+i,done:i%2===0
  }));
  assert.throws(() => addProjectTask(updated, 'Weitere Aufgabe', 'extra'), /Maximal/);
  assert.deepEqual(importProjects(exportProjects([updated])), [updated]);
  assert.equal(openTasksCount(updated.todos), 25);
  const invalid = JSON.stringify({schemaVersion:2,projects:[{...updated,todos:[{id:'bad',title:'',done:false}]}]});
  assert.throws(() => importProjects(invalid), /ungültig/);
});

test('Alte lokale Daten ohne Todos werden ergänzt, ohne Status zu verändern', () => {
  const data = parseProjects(JSON.stringify([{...project, status:'Wartet'}]));
  assert.equal(data[0].status, 'Wartet');
  assert.equal(data[0].nextStep, project.nextStep);
  assert.deepEqual(data[0].todos, []);
});

test('Alte Projekt-Bearbeitungsmaske darf Checklisten nicht löschen', () => {
  const original = addProjectTask(project, 'Wichtiges To-do', 'keep-me');
  const editedFromOldForm = {...project, nextStep:'Anderer Fokus'};
  const saved = upsertProject([original], editedFromOldForm);
  assert.equal(saved[0].nextStep, 'Anderer Fokus');
  assert.equal(saved[0].todos.length, 1);
  assert.equal(saved[0].todos[0].id, 'keep-me');
});
