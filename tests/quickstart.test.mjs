import test from 'node:test';
import assert from 'node:assert/strict';
import { quickstartState } from '../site/quickstart.mjs';
import { addProjectTask, toggleProjectTask } from '../site/projects.mjs';

const base = { id:'project1', repository:'CobraChris/fit-app',
  status:'In Arbeit', nextStep:'Tests prüfen', notes:'', todos:[] };

test('Leerer Start führt zum manuellen GitHub-Import', () => {
  const state = quickstartState([], []);
  assert.equal(state.kind,'empty');
  assert.equal(state.target,'repo-import-panel');
  assert.deepEqual(state.summary,{projects:0,ideas:0,activeProjects:0,openTodos:0});
});

test('Bestehende App-Ideen bleiben ohne erfundene Projekte sichtbar', () => {
  const state=quickstartState([],[{title:'Eine App'}]);
  assert.equal(state.kind,'ideas');
  assert.equal(state.target,'ideas-title');
  assert.equal(state.summary.ideas,1);
});

test('Erste echte offene Aufgabe hat Vorrang und wird beschreibend angezeigt', () => {
  const project=addProjectTask(base,'Pull Request prüfen','todo1');
  const state=quickstartState([project],[]);
  assert.equal(state.kind,'focus');
  assert.equal(state.target,'focus-title');
  assert.match(state.description,/CobraChris\/fit-app: Pull Request prüfen/);
  assert.equal(state.summary.openTodos,1);
});

test('Bisheriger nächster Schritt wird ohne Checkliste berücksichtigt', () => {
  const state=quickstartState([base],[]);
  assert.equal(state.kind,'focus');
  assert.match(state.description,/Tests prüfen/);
  assert.equal(state.summary.openTodos,0);
});

test('Fertig abgehakte Checklisten erzeugen keinen alten Phantom-Auftrag', () => {
  const p=addProjectTask(base,'Tests prüfen','todo1');
  const done=toggleProjectTask(p,'todo1',true);
  const state=quickstartState([done],[]);
  assert.equal(state.kind,'projects');
  assert.equal(state.target,'project-list');
  assert.equal(state.summary.openTodos,0);
});

test('Fertige Projekte führen zurück zur App-Ideen-Eingabe', () => {
  const state=quickstartState([{...base,status:'Fertig'}],[]);
  assert.equal(state.kind,'complete');
  assert.equal(state.target,'idea-title');
  assert.equal(state.summary.activeProjects,0);
});

test('Unveränderte Daten und ungültige Eingaben', () => {
  const projects=[{...base}];
  const before=JSON.stringify(projects);
  quickstartState(projects,[]);
  assert.equal(JSON.stringify(projects),before);
  assert.throws(()=>quickstartState(null,[]),/Listen/);
  assert.throws(()=>quickstartState([],null),/Listen/);
});
