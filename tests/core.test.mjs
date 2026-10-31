import test from 'node:test';
import assert from 'node:assert/strict';
import {addIdea, removeIdea, normalizeTitle, parseIdeas} from '../site/core.mjs';

test('Titel werden normalisiert', () => {
  assert.equal(normalizeTitle('  Meine   App  '), 'Meine App');
  assert.equal(normalizeTitle(null), '');
});

test('Idee hinzufügen und Duplikate verhindern', () => {
  const items = addIdea([], ' Fitness Tracker ');
  assert.deepEqual(items, ['Fitness Tracker']);
  assert.throws(() => addIdea(items, 'fitness tracker'), /bereits/);
  assert.throws(() => addIdea(items, '    '), /eingeben/);
});

test('Idee entfernen', () => {
  assert.deepEqual(removeIdea(['A','B'], 'A'), ['B']);
});

test('Ungültige gespeicherte Daten sind sicher', () => {
  assert.deepEqual(parseIdeas('{'), []);
  assert.deepEqual(parseIdeas('{}'), []);
  assert.deepEqual(parseIdeas('["App","App",null,"  Test  "]'), ['App', 'Test']);
});
