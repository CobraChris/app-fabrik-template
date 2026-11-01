import test from 'node:test';
import assert from 'node:assert/strict';
import {
  publicRepositoriesUrl, parsePublicRepositories, mergePublicRepositories,
  PUBLIC_REPO_PAGE_SIZE
} from '../site/github-import.mjs';

const entries = [
  {full_name:'CobraChris/meine-livekarte',owner:{login:'CobraChris'},private:false,archived:false,
    description:'Meine Reisekarte',fork:false},
  {full_name:'CobraChris/app-fabrik-template',owner:{login:'CobraChris'},private:false,archived:false,
    description:'Vorlage',fork:false},
];

test('GitHub-API-Link enthält nur einen validierten Benutzernamen', () => {
  const url = new URL(publicRepositoriesUrl('CobraChris'));
  assert.equal(url.origin,'https://api.github.com');
  assert.equal(url.pathname,'/users/CobraChris/repos');
  assert.equal(url.searchParams.get('type'),'owner');
  assert.equal(url.searchParams.get('per_page'), String(PUBLIC_REPO_PAGE_SIZE));
  assert.throws(()=>publicRepositoriesUrl('evil/other'), /GitHub-Konto/);
});

test('Nur öffentliche, nicht archivierte Repositories des Kontos übernehmen', () => {
  const rows = parsePublicRepositories([...entries,
    {...entries[0],full_name:'CobraChris/private',private:true},
    {...entries[0],full_name:'Other/x',owner:{login:'Other'}},
    {...entries[0],full_name:'CobraChris/old',archived:true},
    {...entries[0],full_name:'CobraChris/.hidden'},
    {...entries[0],full_name:'cobrachris/MEINE-LIVEKARTE'}
  ],'CobraChris');
  assert.equal(rows.length,2);
  assert.equal(rows[0].repository,'CobraChris/meine-livekarte');
  assert.throws(()=>parsePublicRepositories({},'CobraChris'),/Repository-Liste/);
});

test('Auswahl importieren, aber bestehende Projekte und Todos erhalten', () => {
  const existing=[{id:'old',repository:'CobraChris/meine-livekarte',
    nextStep:'PR prüfen',status:'In Arbeit',notes:'Wichtig',
    todos:[{id:'t1',title:'Testen',done:false}]}];
  const available=parsePublicRepositories(entries,'CobraChris');
  const result=mergePublicRepositories(existing,available,
    ['CobraChris/meine-livekarte','CobraChris/app-fabrik-template'],
    ()=>'new-project');
  assert.equal(result.added,1);
  assert.equal(result.projects.length,2);
  assert.equal(result.projects[0].nextStep,'PR prüfen');
  assert.equal(result.projects[0].todos.length,1);
  assert.equal(result.projects[1].status,'Offen');
  assert.equal(result.projects[1].nextStep,'');
});

test('Auswahl darf keine erfundenen oder doppelt genannten Repositories enthalten', () => {
  const available=parsePublicRepositories(entries,'CobraChris');
  assert.throws(()=>mergePublicRepositories([],available,['Another/Repo'],()=>'id'),/Suchergebnissen/);
  assert.throws(()=>mergePublicRepositories([],available,
    ['CobraChris/meine-livekarte','cobrachris/MEINE-LIVEKARTE'],()=>'id'),
    /Doppelte/);
});

test('Projektlimit und doppelte IDs werden vor Speicherung geprüft', () => {
  const available=parsePublicRepositories(entries,'CobraChris');
  const existing=Array.from({length:100},(_,i)=>({
    id:'p'+i,repository:'example/repo'+i,status:'Offen',todos:[]
  }));
  assert.throws(()=>mergePublicRepositories(existing,available,
    ['CobraChris/meine-livekarte'],()=>'next'),/voll/);
  assert.throws(()=>mergePublicRepositories(
    [{id:'collision',repository:'example/other',status:'Offen'}],available,
    ['CobraChris/meine-livekarte'],()=>'collision'),/nicht eindeutig/);
});

test('Keine Auswahl verändert keine Projektdaten', () => {
  const result=mergePublicRepositories([],[],[],()=>'unused');
  assert.deepEqual(result,{projects:[],added:0});
});
