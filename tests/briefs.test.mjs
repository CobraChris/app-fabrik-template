import test from 'node:test';
import assert from 'node:assert/strict';
import {
  storageKey, normalizeBrief, upsertBrief, removeBrief, parseBriefs,
  migrateLegacy, issueDraft, exportBackup, importBackup
} from '../site/briefs.mjs';

const brief = { id: 'app1', title: 'Reise-App', goal: 'Reise planen',
  features: '- Karte\n- Tickets', acceptance: '- Offline nutzbar',
  constraints: 'Keine fremden APIs', status: 'Idee', priority: 'Hoch' };

test('Ideen besitzen einen eigenen lokalen Speicher je App-Pfad', () => {
  assert.notEqual(storageKey('/app-fabrik-template/'), storageKey('/fitness-app/'));
  assert.throws(() => storageKey('../unsafe'), /Ungültiger/);
});

test('Steckbrief anlegen, bearbeiten und entfernen', () => {
  const added = upsertBrief([], brief);
  assert.equal(added.length, 1);
  const edited = upsertBrief(added, { ...brief, goal: 'Reise einfacher planen' });
  assert.equal(edited.length, 1);
  assert.equal(edited[0].goal, 'Reise einfacher planen');
  assert.deepEqual(removeBrief(edited, 'app1'), []);
});

test('Doppelte Namen und fehlende IDs werden abgewiesen', () => {
  assert.throws(() => upsertBrief([brief], { ...brief, id: 'app2', title: 'reise-app' }), /existiert/);
  assert.throws(() => normalizeBrief({ ...brief, id: '../' }), /App-ID/);
  assert.throws(() => normalizeBrief({ ...brief, title: ' ' }), /App-Namen/);
});

test('Frühere Ideen werden als Steckbriefe übernommen', () => {
  const migrated = migrateLegacy(['Fitness', 'Reise']);
  assert.equal(migrated.length, 2);
  assert.equal(migrated[0].id, 'legacy-1');
  assert.equal(migrated[1].title, 'Reise');
  assert.equal(migrated[0].status, 'Idee');
});

test('Ungültige Einträge werden beim Laden ignoriert', () => {
  assert.deepEqual(parseBriefs('kaputt'), []);
  const parsed = parseBriefs(JSON.stringify([brief, { id: '*', title: 'Fehler' }, brief]));
  assert.equal(parsed.length, 1);
});

test('Export und Import erhalten alle Felder', () => {
  const imported = importBackup(exportBackup([brief]));
  assert.deepEqual(imported, [brief]);
  assert.throws(() => importBackup('{"schemaVersion":2,"briefs":[]}'), /Backup-Format/);
  assert.throws(() => importBackup('not json'), /gültiges JSON/);
  assert.throws(() => importBackup(JSON.stringify({schemaVersion:1,briefs:[brief,brief]})), /doppelte/);
});

test('Entwicklungsauftrag benennt Ziel, Funktionen und Abnahme', () => {
  const result = issueDraft(brief);
  assert.match(result, /GitHub Issue/);
  assert.match(result, /Reise planen/);
  assert.match(result, /Tickets/);
  assert.match(result, /Offline nutzbar/);
  assert.match(result, /Starte weder Codex/);
});

test('Leere Detailfelder werden als offen gekennzeichnet', () => {
  const text = issueDraft({id:'x1',title:'App'});
  assert.match(text, /Noch zu definieren/);
});

test('Feldlängen und Auswahlwerte sind begrenzt', () => {
  const result = normalizeBrief({ ...brief, goal: 'a'.repeat(1200), priority:'unsicher',status:'falsch'});
  assert.equal(result.goal.length,1000);
  assert.equal(result.priority,'Normal');
  assert.equal(result.status,'Idee');
});

test('Ein Import kann höchstens 200 Ideen enthalten', () => {
  const items = Array.from({length:201}, (_,i)=>({id:'a'+i,title:'App '+i}));
  assert.throws(()=>importBackup(JSON.stringify({schemaVersion:1,briefs:items})), /Backup-Format/);
});
