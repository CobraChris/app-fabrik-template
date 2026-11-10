import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
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

test('GPS recenter draws a visible blue point with a white border and accuracy ring',()=>{
 const listeners={};
 const status={textContent:''},placeholder={hidden:false};
 const els={
  'center':{addEventListener(name,fn){listeners[name]=fn;}},
  'google-map':{},'status':status,'not-connected':placeholder
 };
 const maps=[],markers=[],circles=[];
 let fix={coords:{latitude:36.7484,longitude:139.6143,accuracy:35}};
 class FakeMap{
  constructor(el,options){this.options=options;maps.push(this);}
  setCenter(p){this.center=p;}
  setZoom(z){this.zoom=z;}
 }
 class FakeMarker{
  constructor(options){this.options=options;this.position=options.position;markers.push(this);}
  setPosition(p){this.position=p;}
 }
 class FakeCircle{
  constructor(options){this.options=options;this.center=options.center;this.radius=options.radius;this.map=options.map;circles.push(this);}
  setCenter(p){this.center=p;}
  setRadius(r){this.radius=r;}
  setMap(m){this.map=m;}
 }
 const sandbox={
  URLSearchParams,
  localStorage:{getItem(key){return key==='fahrcockpit-map-demo-key-v1'?'AIza'+'x'.repeat(35):null;}},
  navigator:{geolocation:{getCurrentPosition(success){success(fix);}}},
  document:{
   getElementById:id=>els[id],
   createElement:()=>({}),
   head:{append(){}}
  },
  // Reproduce the actual Samsung WebView: SymbolPath is NOT available.
  google:{maps:{Map:FakeMap,Marker:FakeMarker,Circle:FakeCircle}},
  setTimeout:()=>1,clearTimeout:()=>{}
 };
 runInNewContext(read('app.js'),sandbox);
 sandbox.__fahrcockpitMapsReady();
 assert.equal(maps.length,1);
 assert.equal(markers.length,0,'do not invent a location before consent');
 listeners.click();
 assert.equal(maps[0].center.lat,36.7484);
 assert.equal(maps[0].center.lng,139.6143);
 assert.equal(maps[0].zoom,16);
 assert.equal(markers.length,1,'blue dot marker must be created');
 assert.equal(markers[0].position.lat,36.7484);
 assert.equal(markers[0].options.icon.fillColor,'#1a73e8');
 assert.equal(markers[0].options.icon.strokeColor,'#ffffff');
 assert.equal(markers[0].options.icon.fillOpacity,1);
 assert.equal(markers[0].options.map,maps[0]);
 assert.equal(circles.length,1,'accuracy radius must be drawn');
 assert.equal(circles[0].radius,35);
 assert.match(status.textContent,/Blauer Punkt/);
 fix={coords:{latitude:36.7500,longitude:139.6200,accuracy:12}};
 listeners.click();
 assert.equal(markers.length,1,'recenter should update not stack markers');
 assert.equal(circles.length,1,'recenter should update not stack accuracy circles');
 assert.equal(markers[0].position.lat,36.75);
 assert.equal(circles[0].radius,12);
 fix={coords:{latitude:36.7510,longitude:139.6210,accuracy:5000}};
 listeners.click();
 assert.equal(markers[0].position.lat,36.751);
 assert.equal(circles[0].map,null,'hide misleading oversized accuracy circle');
});

test('when Google Marker API is missing, a coordinate-anchored HTML overlay displays GPS',()=>{
 const listeners={},dots=[],status={textContent:''},placeholder={hidden:false};
 const center={addEventListener(event,fn){listeners[event]=fn;}};
 const els={'center':center,'google-map':{},'status':status,'not-connected':placeholder};
 let coords={latitude:36.7484,longitude:139.6143,accuracy:22};
 const document={
  getElementById:id=>els[id],
  createElement:()=>({style:{},setAttribute(){},classList:{add(){}},remove(){}}),
  head:{append(){}}
 };
 class FakeMap{setCenter(point){this.point=point;}setZoom(zoom){this.zoom=zoom;}}
 class FakeOverlay{
  getPanes(){return {overlayMouseTarget:{appendChild:el=>dots.push(el)}};}
  getProjection(){return {fromLatLngToDivPixel:p=>({x:p.lng*2,y:p.lat*2})};}
  setMap(map){this.map=map;this.onAdd();this.draw();}
 }
 const sandbox={
  URLSearchParams,document,google:{maps:{Map:FakeMap,OverlayView:FakeOverlay}},
  localStorage:{getItem:key=>key==='fahrcockpit-map-demo-key-v1'?'AIza'+'x'.repeat(35):null},
  navigator:{geolocation:{getCurrentPosition:success=>success({coords})}},
  setTimeout:()=>1,clearTimeout:()=>{}
 };
 runInNewContext(read('app.js'),sandbox);
 sandbox.__fahrcockpitMapsReady();
 listeners.click();
 assert.equal(dots.length,1,'HTML marker must be attached to the Google overlay pane');
 assert.equal(dots[0].style.left,(139.6143*2)+'px');
 assert.equal(dots[0].style.top,(36.7484*2)+'px');
 assert.equal(dots[0].getAttribute,undefined,'no SVG SymbolPath is required');
 assert.match(status.textContent,/Blauer Punkt/);
 coords={latitude:36.75,longitude:139.62,accuracy:12};
 listeners.click();
 assert.equal(dots.length,1,'recenter must update the same HTML marker');
 assert.equal(dots[0].style.left,(139.62*2)+'px');
});

test('even without Google Marker or OverlayView, a center-only dot appears and hides when panned',()=>{
 const listeners={},mapEvents={},container={children:[],appendChild(el){this.children.push(el);}};
 const status={textContent:''};
 const elements={
  'google-map':{parentElement:container},
  'center':{addEventListener(event,fn){listeners[event]=fn;}},
  'status':status,'not-connected':{hidden:false}
 };
 class FakeMap{
  setCenter(point){this.center=point;}setZoom(zoom){this.zoom=zoom;}
  addListener(event,fn){mapEvents[event]=fn;}
 }
 const sandbox={
  URLSearchParams,document:{
   getElementById:id=>elements[id],
   createElement:()=>({style:{},classList:{items:[],add(name){this.items.push(name);}},setAttribute(){},hidden:false}),
   head:{append(){}}
  },google:{maps:{Map:FakeMap}},
  localStorage:{getItem:key=>key==='fahrcockpit-map-demo-key-v1'?'AIza'+'x'.repeat(35):null},
  navigator:{geolocation:{getCurrentPosition:success=>success({
   coords:{latitude:36.7484,longitude:139.6143,accuracy:28}})}},
  setTimeout:()=>1,clearTimeout:()=>{}
 };
 runInNewContext(read('app.js'),sandbox);
 sandbox.__fahrcockpitMapsReady();
 assert.equal(container.children.length,0,'do not display a location before user permits GPS');
 listeners.click();
 assert.equal(container.children.length,1);
 const dot=container.children[0];
 assert.equal(dot.className,'gps-position-dot');
 assert.equal(dot.classList.items[0],'gps-center-fallback');
 assert.equal(dot.hidden,false);
 assert.match(status.textContent,/Blauer Punkt/);
 mapEvents.dragstart();
 assert.equal(dot.hidden,true,'manual pan must not leave a misleading fixed GPS dot');
 listeners.click();
 assert.equal(dot.hidden,false,'recenter restores the indicator');
 assert.equal(container.children.length,1,'do not stack dots');
 assert.match(read('styles.css'),/\.gps-position-dot\.gps-center-fallback/);
});

test('foreground live GPS follows position, pauses on drag/background and resumes without zoom resets',()=>{
 const events={},windowEvents={},watches=[],cleared=[],markers=[],circles=[];
 const status={textContent:''};
 const controls={
  'center':{addEventListener(type,fn){events.centerClick=fn;}},
  'gps-toggle':{hidden:true,textContent:'',attrs:{},addEventListener(type,fn){events.gpsClick=fn;},
   setAttribute(name,value){this.attrs[name]=value;}},
  'google-map':{},'status':status,'not-connected':{hidden:false}
 };
 const document={hidden:false,getElementById:key=>controls[key],
  createElement:()=>({style:{},classList:{add(){}},setAttribute(){},remove(){}}),
  head:{append(){}},
  addEventListener(type,fn){windowEvents[type]=fn;}
 };
 class Map{
  constructor(){this.centers=[];this.zooms=[];}
  setCenter(point){this.centers.push({...point});}
  setZoom(zoom){this.zooms.push(zoom);}
  addListener(type,fn){events[type]=fn;}
 }
 class Marker{
  constructor(opts){this.position=opts.position;markers.push(this);}
  setPosition(position){this.position=position;}
 }
 class Circle{
  constructor(opts){this.radius=opts.radius;circles.push(this);}
  setCenter(point){this.point=point;}
  setRadius(radius){this.radius=radius;}
  setMap(map){this.map=map;}
 }
 const coords=(lat,lng,accuracy=25)=>({coords:{latitude:lat,longitude:lng,accuracy}});
 let first=coords(36.7484,139.6143);
 const geolocation={
  getCurrentPosition(success){success(first);},
  watchPosition(success,error,options){
   const id=watches.length+11;
   watches.push({id,success,error,options});
   return id;
  },
  clearWatch(id){cleared.push(id);}
 };
 const sandbox={
  URLSearchParams,
  document,
  localStorage:{getItem:key=>key==='fahrcockpit-map-demo-key-v1'?'AIza'+'x'.repeat(35):null},
  navigator:{geolocation},
  google:{maps:{Map,Marker,Circle}},
  setTimeout:()=>1,clearTimeout:()=>{}
 };
 runInNewContext(read('app.js'),sandbox);
 sandbox.__fahrcockpitMapsReady();
 assert.equal(watches.length,0,'no background GPS before opt-in');
 assert.equal(controls['gps-toggle'].hidden,true);
 events.centerClick();
 const map=markers.length && watches.length ? sandbox : null;
 assert.ok(map,'start must create visible marker and GPS watch');
 assert.equal(watches.length,1);
 assert.equal(markers[0].position.lat,36.7484);
 assert.equal(controls['gps-toggle'].hidden,false);
 assert.equal(controls['gps-toggle'].textContent,'GPS Pause');
 const initialCenters=watches.length && markers[0].position.lat;
 assert.equal(initialCenters,36.7484);
 watches[0].success(coords(36.75,139.62,12));
 assert.equal(markers[0].position.lat,36.75);
 assert.equal(circles[0].radius,12);
 assert.match(status.textContent,/GPS live/);
 events.dragstart();
 watches[0].success(coords(36.76,139.63,17));
 assert.equal(markers[0].position.lat,36.76,
  'location dot updates while user is freely inspecting a different area');
 assert.match(status.textContent,/freie Kartenansicht/);
 document.hidden=true;
 windowEvents.visibilitychange();
 assert.deepEqual(cleared,[11]);
 assert.match(status.textContent,/Hintergrund pausiert/);
 // A stale callback from an old watch must not update location in background.
 watches[0].success(coords(0,0));
 assert.equal(markers[0].position.lat,36.76);
 document.hidden=false;
 windowEvents.visibilitychange();
 assert.equal(watches.length,2,'resume while foreground without additional clicks');
 watches[1].success(coords(36.77,139.64));
 assert.equal(markers[0].position.lat,36.77);
 events.gpsClick();
 assert.deepEqual(cleared,[11,12]);
 assert.equal(controls['gps-toggle'].textContent,'GPS Start');
 assert.match(status.textContent,/GPS pausiert/);
 watches[1].success(coords(0,0));
 assert.equal(markers[0].position.lat,36.77,'no processing after GPS pause');
 events.gpsClick();
 assert.equal(watches.length,3);
 watches[2].success(coords(36.78,139.65));
 assert.equal(markers[0].position.lat,36.78);
 assert.match(status.textContent,/GPS live/);
 first=coords(36.79,139.66);
 events.centerClick();
 assert.equal(markers[0].position.lat,36.79);
 assert.equal(watches.length,3,'recenter does not start a duplicate GPS watch');
 assert.match(status.textContent,/blauer Punkt/);
});

test('GPS denial stops live watches and never restarts location tracking silently',()=>{
 const js=read('app.js');
 assert.match(js,/error\?\.code===1/);
 assert.match(js,/stopWatching\(\);trackingRequested=false;updateGpsButton\(\)/);
 assert.match(js,/document\.addEventListener\('visibilitychange'/);
 assert.match(js,/!trackingRequested\|\|!googleMap\|\|gpsWatchId!==null\|\|document\.hidden/);
 assert.doesNotMatch(js,/localStorage\.setItem\('position|fetch\(.*coords/);
});
