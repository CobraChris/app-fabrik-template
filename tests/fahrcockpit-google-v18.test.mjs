import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const root = '../apps/fahrcockpit-google-v18/';
const read = p => readFileSync(new URL(root + p, import.meta.url), 'utf8');
const legacy = p => readFileSync(new URL('../apps/fahrcockpit-google-v16/' + p, import.meta.url), 'utf8');

test('v18 uses an isolated Google map host and never modifies installed v17 map', () => {
  const html = read('index.html'), js = read('app.js');
  assert.match(html, /id="google-map"/);
  assert.match(html, /id="map-target-open"/);
  assert.match(js, /fahrcockpit:\/\/destination\?lat=/);
  assert.doesNotMatch(js, /L\.tileLayer|OpenStreetMap|DirectionsService|NavigationSdk|fetch\(/);
  assert.doesNotMatch(legacy('index.html'), /id="map-target-open"/);
  assert.doesNotMatch(legacy('app.js'), /fahrcockpit:\/\/destination\?lat=/);
  assert.match(read('styles.css'), /\.map-target\{/);
  assert.equal(JSON.parse(read('manifest.webmanifest')).scope, './');
});

test('tapping map picks coordinates but never starts navigation before confirmation', () => {
  const listeners = {}, mapEvents = {}, outgoing = [], state = {textContent:''};
  const targetBox = {hidden:true}, targetLabel={textContent:''};
  const button=name=>({addEventListener(type,fn){listeners[name]=fn;},setAttribute(){}});
  const nodes={
    'google-map':{}, 'not-connected':{hidden:true}, 'status':state,
    'center':button('center'),'traffic':button('traffic'),'gps-toggle':button('gps'),
    'map-target':targetBox, 'map-target-label':targetLabel,
    'map-target-open':button('open'), 'map-target-close':button('close')
  };
  class GoogleMap {
    addListener(type,fn){mapEvents[type]=fn;}
  }
  const sandbox={
    URLSearchParams,
    document:{hidden:false,getElementById:id=>nodes[id],createElement:()=>({}),head:{append(){}},addEventListener(){}},
    localStorage:{getItem:key=>key==='fahrcockpit-map-demo-key-v1'?'AIza'+'x'.repeat(35):null},
    google:{maps:{Map:GoogleMap}},
    navigator:{geolocation:{}},
    location:{assign:uri=>outgoing.push(uri)},
    setTimeout:()=>1,clearTimeout:()=>{}
  };
  runInNewContext(read('app.js'),sandbox);
  sandbox.__fahrcockpitMapsReady();
  assert.equal(typeof mapEvents.click,'function');
  assert.equal(targetBox.hidden,true);
  mapEvents.click({latLng:{lat:()=>36.7484,lng:()=>139.6143}});
  assert.equal(targetBox.hidden,false);
  assert.match(targetLabel.textContent,/36\.748400, 139\.614300/);
  assert.equal(outgoing.length,0,'do not auto-start navigation on map tap');
  listeners.open();
  assert.deepEqual(outgoing,['fahrcockpit://destination?lat=36.748400&lng=139.614300']);
  listeners.close();
  assert.equal(targetBox.hidden,true);
  mapEvents.click({latLng:{lat:()=>91,lng:()=>139.6143}});
  assert.equal(targetBox.hidden,true);
  assert.equal(outgoing.length,1);
});

test('v18 retains Google key handoff, GPS and traffic without a paid routing API',()=>{
  const js=read('app.js');
  assert.match(js,/livekarte-google-demo-key-v1/);
  assert.match(js,/watchPosition/);
  assert.match(js,/TrafficLayer/);
  assert.match(read('transfer.js'),/AES-GCM/);
  assert.match(read('index.html'),/Google aus Live-Karte übernehmen/);
  assert.doesNotMatch(js,/new google\.maps\.DirectionsService|new google\.maps\.PlacesService/);
});
