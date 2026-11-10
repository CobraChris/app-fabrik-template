import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../apps/fahrcockpit-map/'+p,import.meta.url),'utf8');

test('zero-configuration map starts directly with Leaflet/OSM and no key prompt',()=>{
 const html=read('index.html'),css=read('styles.css'),js=read('app.js');
 assert.match(html,/src="\.\/vendor\/leaflet\.js"/);
 assert.match(html,/id="osm-map"/);
 assert.match(css,/#osm-map,#google-map/);
 assert.match(js,/startOSM\(\);\s*enableGoogleButton\(\)/);
 assert.match(js,/L\.tileLayer\('https:\/\/tile\.openstreetmap\.org\/\{z\}\/\{x\}\/\{y\}\.png'/);
 assert.doesNotMatch(js,/const old=stored\(\);if\(GOOGLE_KEY\.test\(old\)\)loadMap/);
 assert.doesNotMatch(html, /Karte einrichten|Demo-Schlüssel eingeben/);
});
test('local demo key is optional and used only for explicit Google selection',()=>{
 const js=read('app.js'),html=read('index.html');
 assert.match(html,/id="google" hidden/);
 assert.match(js,/googleBtn\.addEventListener\('click'/);
 assert.match(js,/localStorage\.setItem\(KEY,key\)/);
 assert.match(js,/auth_referrer_policy:'origin'/);
 assert.doesNotMatch(js,/AIza[0-9A-Za-z_-]{25,90}/);
 assert.doesNotMatch(js,/DirectionsService|computeRoutes|PlacesService|fetch\(/);
});
test('OSM license and isolated manifest are supplied',()=>{
 const m=JSON.parse(read('manifest.webmanifest'));
 assert.equal(m.scope,'./');assert.equal(m.start_url,'./');assert.equal(m.id,'./');
 assert.match(read('vendor/LEAFLET-LICENSE'),/BSD 2-Clause License/);
 assert.match(read('app.js'),/openstreetmap\.org\/copyright/);
});
