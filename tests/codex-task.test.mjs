import test from 'node:test';
import assert from 'node:assert/strict';
import { selectTask, issueToTask, buildPrompt, prepare } from '../scripts/prepare-codex-task.mjs';

test('Freitext oder Issue-Nummer, aber nie beides', () => {
  assert.deepEqual(selectTask({ task: 'Ein neues Widget ergänzen' }), {
    kind: 'text', text: 'Ein neues Widget ergänzen'
  });
  assert.deepEqual(selectTask({ issueNumber: ' 42 ' }), { kind: 'issue', number: '42' });
  assert.throws(() => selectTask({}), /entweder/);
  assert.throws(() => selectTask({ task: 'Ein Widget', issueNumber: '42' }), /entweder/);
  assert.throws(() => selectTask({ issueNumber: '../42' }), /positive ganze Zahl/);
  assert.throws(() => selectTask({ task: 'kurz' }), /10 bis 12000/);
});

test('Nur offene echte Issues mit passender Nummer werden angenommen', () => {
  const data = { number: 3, title: 'Neuer Filter', body: 'Ein Dropdown für Status', state: 'open' };
  assert.match(issueToTask(data, '3'), /GitHub Issue #3: Neuer Filter/);
  assert.throws(() => issueToTask({ ...data, pull_request: {} }, '3'), /nicht offen/);
  assert.throws(() => issueToTask({ ...data, state: 'closed' }, '3'), /nicht offen/);
  assert.throws(() => issueToTask(data, '4'), /nicht offen/);
});

test('GitHub-Inhalte werden Daten, nicht Shell-Befehle', async () => {
  const text = await prepare({
    issueNumber: '7',
    task: '',
    repository: 'example/my-app'
  }, async (repo, number) => {
    assert.equal(repo, 'example/my-app');
    assert.equal(number, '7');
    return { number: 7, state: 'open', title: 'Test', body: 'Bitte Filterfunktion ergänzen' };
  });
  assert.match(text, /fachliche Anforderung/);
  assert.match(text, /GitHub Issue #7: Test/);
  assert.match(text, /Bitte Filterfunktion ergänzen/);
});

test('Auftrag kann keine Workflow-Rechte delegieren', () => {
  const prompt = buildPrompt('Erstelle einen besseren Startbildschirm.');
  assert.match(prompt, /Verboten: \.github\//);
  assert.match(prompt, /Kein git commit/);
});

test('Zu große und ungültige Inputs werden zurückgewiesen', () => {
  assert.throws(() => selectTask({ task: 'x'.repeat(12001) }), /10 bis 12000/);
  assert.throws(() => issueToTask({ number: 1, state: 'open', title: 'X', body: 'a'.repeat(12000) }, '1'), /zu lang/);
});
