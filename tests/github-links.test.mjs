import test from 'node:test';
import assert from 'node:assert/strict';
import { githubOwner, githubRepository, ownerFromHostname, repositorySlug, newAppUrl, repositoryVisibility, appSetupLinks, issueText, newIssueLink, PREFILL_LIMIT } from '../site/github-links.mjs';

const idea = { id: 'app1', title: 'Fitness Übungen', goal: 'Training organisieren',
  features: 'Übungen speichern', acceptance: 'Offline nutzbar',
  constraints: 'Kein Login', priority: 'Hoch', status: 'Idee' };

test('GitHub Konten und Repository-Pfade prüfen', () => {
  assert.equal(githubOwner(' CobraChris '), 'CobraChris');
  assert.equal(githubRepository('CobraChris/fitness'), 'CobraChris/fitness');
  assert.equal(ownerFromHostname('cobrachris.github.io'), 'cobrachris');
  assert.equal(ownerFromHostname('example.org'), '');
  for (const invalid of ['https://bad.test/a','../a','a/b/c','CobraChris/.hidden']) {
    assert.throws(() => githubRepository(invalid));
  }
  assert.throws(() => githubOwner('-bad'));
});

test('Neue Repositorys werden privat aus der zentralen Vorlage vorgeschlagen', () => {
  const url = new URL(newAppUrl(idea,'CobraChris'));
  assert.equal(url.origin, 'https://github.com');
  assert.equal(url.pathname, '/new');
  assert.equal(url.searchParams.get('visibility'), 'private');
  assert.equal(url.searchParams.get('template_owner'), 'CobraChris');
  assert.equal(url.searchParams.get('template_name'), 'app-fabrik-template');
  assert.equal(url.searchParams.get('name'), 'fitness-uebungen');
  assert.equal(repositorySlug('Äpfel & Öl'), 'aepfel-oel');
});

test('Issues erhalten nur fachliche Angaben und bleiben Entwürfe', () => {
  const link = newIssueLink(idea, 'CobraChris/fitness');
  const url = new URL(link.url);
  assert.equal(url.pathname, '/CobraChris/fitness/issues/new');
  assert.match(url.searchParams.get('title'), /Fitness/);
  assert.match(url.searchParams.get('body'), /Offline nutzbar/);
  assert.ok(link.prefilled);
  assert.match(issueText(idea), /Rahmenbedingungen/);
});

test('Lange Issue-Texte nutzen die leere Eingabemaske', () => {
  const long = { ...idea, features: 'x'.repeat(2500), acceptance: 'y'.repeat(2000), constraints: 'z'.repeat(1000) };
  const result = newIssueLink(long, 'CobraChris/fitness');
  assert.equal(result.prefilled, false);
  assert.equal(result.url, 'https://github.com/CobraChris/fitness/issues/new');
  assert.ok(result.body.length > 5000);
  assert.ok(PREFILL_LIMIT < 7000);
});


test('Öffentliche Repositories für kostenloses GitHub Pages nur nach expliziter Wahl', () => {
  const url = new URL(newAppUrl(idea, 'CobraChris', 'public'));
  assert.equal(url.searchParams.get('visibility'), 'public');
  assert.equal(url.searchParams.get('template_owner'), 'CobraChris');
  assert.equal(url.searchParams.get('template_name'), 'app-fabrik-template');
  assert.equal(repositoryVisibility('public'), 'public');
  assert.equal(repositoryVisibility('private'), 'private');
});

test('Unbekannte Sichtbarkeiten werden abgelehnt, Default bleibt konservativ privat', () => {
  assert.equal(new URL(newAppUrl(idea, 'CobraChris')).searchParams.get('visibility'), 'private');
  assert.throws(() => repositoryVisibility(undefined), /Bitte vor dem Erstellen/);
  for (const invalid of ['', 'Public', 'internal', 'public&owner=attacker', null]) {
    assert.throws(() => repositoryVisibility(invalid), /Bitte vor dem Erstellen/);
    assert.throws(() => newAppUrl(idea, 'CobraChris', invalid), /Bitte vor dem Erstellen/);
  }
});


test('Einrichtungslinks führen ausschließlich zum validierten GitHub-Repository', () => {
  const data = appSetupLinks('CobraChris/meine-neue-app');
  assert.equal(data.pagesSettings, 'https://github.com/CobraChris/meine-neue-app/settings/pages');
  assert.equal(data.setupWorkflow, 'https://github.com/CobraChris/meine-neue-app/actions/workflows/setup-app.yml');
  assert.equal(data.actions, 'https://github.com/CobraChris/meine-neue-app/actions');
  assert.equal(data.possibleSite, 'https://cobrachris.github.io/meine-neue-app/');
  assert.equal(data.template, false);
  assert.equal(appSetupLinks('CobraChris/app-fabrik-template').template, true);
  assert.throws(() => appSetupLinks('https://evil.example/x'));
  assert.throws(() => appSetupLinks('CobraChris/../../repo'));
});
