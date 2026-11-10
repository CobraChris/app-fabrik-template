import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../apps/fahrcockpit-google-v16/'+p,import.meta.url),'utf8');
const legacy=p=>readFileSync(new URL('../apps/fahrcockpit-map/'+p,import.meta.url),'utf8');

test('Google is the only embedded map; existing Live-Karte key automatically reused in same browser',()=>{
 const html=read('index.html'),css=read('styles.css'),js=read('app.js');
 assert.match(html,/id="google-map"/);
 assert.match(css,/#google-map\{position:absolute;inset:0/);
 assert.doesNotMatch(html,/id="osm-map"|leaflet|OpenStreetMap/);
 assert.doesNotMatch(js,/L\.tileLayer|startOSM|openstreetmap\.org/);
 assert.match(js,/LIVE_KEY='livekarte-google-demo-key-v1'/);
 assert.match(js,/localStorage\.getItem\(LIVE_KEY\)/);
 assert.match(js,/loadGoogle\(stored\(\)\)/);
 assert.match(js,/https:\/\/maps\.googleapis\.com\/maps\/api\/js/);
 assert.match(js,/auth_referrer_policy:'origin'/);
 assert.doesNotMatch(js,/AIza[0-9A-Za-z_-]{25,90}/);
 assert.doesNotMatch(js,/DirectionsService|computeRoutes|PlacesService|fetch\(/);
});
test('no Google key: explicit pairing prompt, never silently fall back to a different provider',()=>{
 const html=read('index.html'),js=read('app.js');
 assert.match(html,/href="fahrcockpit:\/\/import"/);
 assert.match(js,/function showUnconfigured/);
 assert.match(js,/empty\.hidden=false/);
 assert.doesNotMatch(js,/selectOSM|startOSM|tile\.openstreetmap/);
});
test('existing browser key is transferred only as authenticated AES-GCM ciphertext',()=>{
 const html=read('transfer.html'),js=read('transfer.js');
 assert.match(html,/Content-Security-Policy/);
 assert.match(html,/script defer src="\.\/transfer\.js"/);
 assert.match(js,/livekarte-google-demo-key-v1/);
 assert.match(js,/history\.replaceState/);
 assert.match(js,/crypto\.subtle\.encrypt\(\{name:'AES-GCM',iv\}/);
 assert.match(js,/scheme=fahrcockpit;package=com\.cobrachris\.fahrcockpit/);
 assert.doesNotMatch(js,/fetch\(|XMLHttpRequest|sendBeacon/);
});
test('Google-only map manifest keeps a separate PWA scope',()=>{
 const m=JSON.parse(read('manifest.webmanifest'));
 assert.equal(m.id,'./');assert.equal(m.scope,'./');
 assert.equal(m.start_url,'./');assert.equal(m.display,'standalone');
});

test('the signed v15 map host is untouched while v16 is Google-only',()=>{
 const oldHtml=legacy('index.html'),oldJs=legacy('app.js');
 assert.match(oldHtml,/id="osm-map"/);
 assert.match(oldJs,/function startOSM\(\)/);
 const newHtml=read('index.html'),newJs=read('app.js');
 assert.doesNotMatch(newHtml,/id="osm-map"|leaflet\.js/);
 assert.doesNotMatch(newJs,/startOSM\(|tile\.openstreetmap\.org/);
});

test('Google pairing can import a key copied from a different installed browser profile',()=>{
 const html=read('transfer.html'),js=read('transfer.js');
 assert.match(html,/id="clipboard"/);
 assert.match(html,/Aus Zwischenablage übernehmen/);
 assert.match(html,/id="pasted-key"/);
 assert.match(html,/id="retry"/);
 assert.match(js,/navigator\.clipboard\.readText\(\)/);
 assert.match(js,/pasteForm\.addEventListener\('submit'/);
 assert.match(js,/document\.addEventListener\('visibilitychange'/);
 assert.match(js,/VALID_GOOGLE_KEY\.test\(key\)/);
 assert.match(js,/crypto\.subtle\.encrypt\(\{name:'AES-GCM',iv\}/);
 assert.match(js,/MAX_SESSION_AGE_MS=4\*60\*1000/);
 assert.doesNotMatch(js,/fetch\(|XMLHttpRequest|sendBeacon|postMessage\(/);
});
