import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  githubApiUrls,parseRepository,parseWorkflowRuns,parseRelease,classifyRepoHttp,MAX_BATCH
} from '../site/status-data.mjs';
import { projectKey, parseProjects } from '../site/projects.mjs';

const repo='CobraChris/sanftwecker';

test('only validated GitHub API URLs with bounded page size are generated',()=>{
  const urls=githubApiUrls(repo,'feature/qa-1');
  assert.equal(urls.repository,'https://api.github.com/repos/'+repo);
  assert.equal(new URL(urls.workflows).origin,'https://api.github.com');
  assert.equal(new URL(urls.workflows).searchParams.get('branch'),'feature/qa-1');
  assert.equal(new URL(urls.workflows).searchParams.get('per_page'),'30');
  assert.equal(urls.releases,'https://api.github.com/repos/'+repo+'/releases?per_page=5');
  for(const input of ['Other/../escape','Other/x?y=z','invalid','CobraChris/<script>'])
    assert.throws(()=>githubApiUrls(input));
  for(const branch of ['../../secrets','a//b','/branch','a/','a?token=abc',''])
    if(branch)assert.throws(()=>githubApiUrls(repo,branch));
  assert.ok(MAX_BATCH<=10,'Bounded unauthenticated API traffic');
});

test('repository metadata has to be public and from the exact expected project',()=>{
  const x={full_name:repo,private:false,archived:false,
    default_branch:'main',pushed_at:'2026-11-17T10:00:00Z'};
  assert.deepEqual(parseRepository(x,repo),
    {branch:'main',updatedAt:'2026-11-17T10:00:00Z'});
  for(const bad of [
    {...x,private:true},{...x,full_name:'Other/sanftwecker'},
    {...x,archived:true},{...x,default_branch:'../bad'},
    {...x,default_branch:42},null,[],
  ])assert.throws(()=>parseRepository(bad,repo));
});

test('workflow results never claim success without a matching completed verified run',()=>{
  const run={
    id:123,head_branch:'main',repository:{full_name:repo},
    status:'completed',conclusion:'success',name:'Android CI',
    updated_at:'2026-11-17T10:00:00Z'
  };
  const success=parseWorkflowRuns({workflow_runs:[run]},repo,'main');
  assert.equal(success.label,'Erfolgreich');
  assert.equal(success.kind,'success');
  assert.equal(success.url,'https://github.com/'+repo+'/actions/runs/123');
  const failed=parseWorkflowRuns({workflow_runs:[{...run,conclusion:'failure'}]},repo,'main');
  assert.equal(failed.kind,'failure');
  const inProgress=parseWorkflowRuns({workflow_runs:[{...run,status:'in_progress',conclusion:null}]},repo,'main');
  assert.equal(inProgress.kind,'running');
  const missing=parseWorkflowRuns({workflow_runs:[{...run,head_branch:'dev'}]},repo,'main');
  assert.equal(missing.kind,'unknown');
  assert.throws(()=>parseWorkflowRuns({},repo,'main'));
  assert.equal(parseWorkflowRuns({workflow_runs:[{...run,repository:{full_name:'Other/evil'}}]},repo,'main').kind,'unknown');
});

test('GitHub Releases are not confused with private Actions artifacts or draft versions',()=>{
  const releases=[
    {tag_name:'draft-only',draft:true},
    {tag_name:'v1.3-beta',draft:false,prerelease:true,published_at:'2026-11-17T11:20:00Z'}
  ];
  const one=parseRelease(releases,repo);
  assert.equal(one.label,'v1.3-beta (Vorabversion)');
  assert.equal(one.url,'https://github.com/'+repo+'/releases/tag/v1.3-beta');
  assert.equal(parseRelease([],repo).label,'Kein GitHub Release');
  assert.throws(()=>parseRelease({},repo));
  assert.equal(classifyRepoHttp(404),'Privat oder nicht vorhanden');
  assert.match(classifyRepoHttp(403),/API-Limit/);
});

test('dashboard reuses local project store without changing schema or existing tasks',()=>{
  assert.equal(projectKey('/app-fabrik-template/'),
    'app-fabrik-projects-v1:/app-fabrik-template/');
  const original=JSON.stringify([{id:'x',repository:repo,
    status:'Wartet',nextStep:'Auf Nutzer warten',notes:'Kein Release',
    todos:[{id:'task',title:'Später testen',done:false}]}]);
  const restored=parseProjects(original);
  assert.equal(restored[0].todos[0].title,'Später testen');
  assert.equal(restored[0].nextStep,'Auf Nutzer warten');
});

test('dashboard is read-only, has honest privacy notices and is cached as an app shell',()=>{
  const file=name=>readFileSync(new URL('../'+name,import.meta.url),'utf8');
  const html=file('site/status.html');
  const js=file('site/status.js');
  const index=file('site/index.html');
  const sw=file('site/sw.js');
  for(const id of ['status-refresh-all','status-project-list','status-project-count','status-feedback','status-empty']){
    assert.match(html,new RegExp('id="'+id+'"'));
  }
  assert.match(index,/href="\.\/status\.html"/);
  assert.match(html,/Privat|private Repositories/i);
  assert.match(html,/Keine Tokens|keine Tokens/i);
  assert.match(html,/GitHub Release/);
  for(const fileName of ['./status.html','./status.js','./status-data.mjs'])
    assert.ok(sw.includes("'"+fileName+"'"),fileName+' must be offline cached');
  assert.doesNotMatch(js,/localStorage\.setItem|fetch\([^)]*\b(method\s*:\s*'POST'|Authorization)/);
  assert.match(js,/credentials:'omit'/);
  assert.match(js,/cache:'no-store'/);
  assert.match(js,/refreshCard/);
  assert.match(js,/textContent=/);
});
