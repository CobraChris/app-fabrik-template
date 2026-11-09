import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../apps/fahrcockpit-map/'+p,import.meta.url),'utf8');

test('map host reserves a real viewport and never paints Spotify over it',()=>{
 const html=read('index.html'),css=read('styles.css');
 assert.match(html,/id="google-map"/);
 assert.match(css,/#google-map\{position:absolute;inset:0\}/);
 assert.match(html,/Kartenprototyp · keine Abbiegenavigation/);
 assert.doesNotMatch(html,/iframe\s+src="https:\/\/www.google.com\/maps/);
});
test('demo key is entered locally; only Google JS Maps loader is called',()=>{
 const js=read('app.js');
 assert.match(js,/https:\/\/maps\.googleapis\.com\/maps\/api\/js/);
 assert.match(js,/localStorage\.setItem\(KEY,key\)/);
 assert.match(js,/auth_referrer_policy:'origin'/);
 assert.doesNotMatch(js,/AIza[0-9A-Za-z_-]{25,90}/);
 assert.doesNotMatch(js,/DirectionsService|computeRoutes|Navigation SDK|PlacesService|fetch\(/);
});
test('standalone app has isolated scope and no billing or production promise',()=>{
 const m=JSON.parse(read('manifest.webmanifest'));
 assert.equal(m.id,'./');assert.equal(m.scope,'./');assert.equal(m.start_url,'./');
 assert.equal(m.display,'standalone');
 assert.match(read('index.html'),/href="\.\/manifest\.webmanifest"/);
});
