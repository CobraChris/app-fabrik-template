import test from 'node:test';
import assert from 'node:assert/strict';
import {
  PROJECT_LIMIT, projectKey, normalizeProject, upsertProject,
  deleteProject, sortProjects, openProjectsCount, projectLinks,
  parseProjects, importProjects, exportProjects
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
  assert.deepEqual(importProjects(exportProjects([project])), [project]);
  assert.deepEqual(parseProjects(JSON.stringify([project, {...project, id:'bad!'}])), [project]);
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
