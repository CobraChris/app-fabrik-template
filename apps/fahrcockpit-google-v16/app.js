/* Google-only FahrCockpit map: existing Live-Karte Demo Key is reused on the
 * same browser origin, or imported once through an explicit encrypted handoff.
 * The Android WebView cannot directly read Chrome/PWA localStorage.
 * Prototype only: no Google Navigation SDK, directions, billing, or OSM.
 */
(()=>{'use strict';
 const KEY='fahrcockpit-map-demo-key-v1';
 const LIVE_KEY='livekarte-google-demo-key-v1';
 const SCRIPT_BASE='https://maps.googleapis.com/maps/api/js';
 const CALLBACK='__fahrcockpitMapsReady';
 const GOOGLE_KEY=/^AIza[0-9A-Za-z_-]{25,90}$/;
 const $=id=>document.getElementById(id);
 const mapEl=$('google-map'),status=$('status'),empty=$('not-connected'),gpsToggle=$('gps-toggle');
 let googleMap=null,loading=false,script=null,loadTimer=null,position=null;
 let positionMarker=null,accuracyCircle=null,positionAccuracy=null;
 let positionOverlay=null,positionDot=null;
 let trackingRequested=false,gpsWatchId=null,followPosition=true;
 const setStatus=value=>{status.textContent=value;};
 function stored(){
  try{
   const own=localStorage.getItem(KEY)||'';
   if(GOOGLE_KEY.test(own))return own;
   const existing=localStorage.getItem(LIVE_KEY)||'';
   return GOOGLE_KEY.test(existing)?existing:'';
  }catch{return '';}
 }
 const scriptUrl=key=>SCRIPT_BASE+'?'+new URLSearchParams({
  key,loading:'async',callback:CALLBACK,auth_referrer_policy:'origin',v:'weekly',language:'de'
 }).toString();
 function showUnconfigured(msg){
  loading=false;clearTimeout(loadTimer);loadTimer=null;
  if(script){script.remove();script=null;}
  empty.hidden=false;
  setStatus(msg||'Google-Zugang noch nicht verbunden.');
 }
 // Google Maps loads different library bundles depending on client/version.
 // Location must not depend on SymbolPath.CIRCLE or the optional Marker library.
 function makeDot(){
  const dot=document.createElement('div');
  dot.className='gps-position-dot';
  dot.title='Mein Standort';
  dot.setAttribute('role','img');
  dot.setAttribute('aria-label','Mein Standort');
  return dot;
 }
 function showPosition(point,accuracy){
  if(!googleMap)return false;
  const maps=globalThis.google?.maps||{};
  let shown=false;

  // Preferred: a real coordinate-anchored native marker with an inline SVG
  // path. Do not require maps.SymbolPath, which was unavailable on this phone.
  if(positionMarker){
   try{positionMarker.setPosition(point);shown=true;}catch{positionMarker=null;}
  }
  if(!shown&&typeof maps.Marker==='function'){
   try{
    positionMarker=new maps.Marker({
     map:googleMap,position:point,title:'Mein Standort',
     clickable:false,zIndex:1000,
     icon:{
      path:'M 0 -8 C 4.4 -8 8 -4.4 8 0 C 8 4.4 4.4 8 0 8 C -4.4 8 -8 4.4 -8 0 C -8 -4.4 -4.4 -8 0 -8 Z',
      scale:1,fillColor:'#1a73e8',fillOpacity:1,
      strokeColor:'#ffffff',strokeOpacity:1,strokeWeight:3
     }
    });
    shown=true;
   }catch{positionMarker=null;}
  }

  // Second provider-independent option: a DOM dot in Google's map overlay
  // pane, positioned through Google's projection as the map is moved/zoomed.
  if(!shown&&typeof maps.OverlayView==='function'){
   try{
    if(!positionOverlay){
     const overlay=new maps.OverlayView();
     const dot=makeDot();
     overlay.onAdd=function(){
      const panes=overlay.getPanes();
      panes.overlayMouseTarget.appendChild(dot);
     };
     overlay.draw=function(){
      const pixel=overlay.getProjection()?.fromLatLngToDivPixel(position);
      if(!pixel)return;
      dot.style.left=pixel.x+'px';
      dot.style.top=pixel.y+'px';
     };
     overlay.onRemove=function(){dot.remove();};
     overlay.setMap(googleMap);
     positionOverlay=overlay;
    }else positionOverlay.draw();
    shown=true;
   }catch{positionOverlay=null;}
  }

  // Last-resort DOM indicator, tied to the center ONLY after an explicit
  // recenter. Hide it on manual pan so it never claims a false location.
  if(!shown){
   try{
    if(!positionDot){
     positionDot=makeDot();
     positionDot.classList.add('gps-center-fallback');
     mapEl.parentElement.appendChild(positionDot);
     if(typeof googleMap.addListener==='function'){
      googleMap.addListener('dragstart',()=>{if(positionDot)positionDot.hidden=true;});
     }
    }
    positionDot.hidden=!followPosition;
    shown=followPosition;
   }catch{shown=false;}
  }

  // An accuracy ring is optional. A missing Circle API must NEVER hide the dot.
  try{
   const precise=Number.isFinite(accuracy)&&accuracy>0&&accuracy<=2000;
   if(precise&&typeof maps.Circle==='function'){
    if(!accuracyCircle){
     accuracyCircle=new maps.Circle({
      map:googleMap,center:point,radius:accuracy,clickable:false,
      strokeColor:'#1a73e8',strokeOpacity:.4,strokeWeight:1,
      fillColor:'#1a73e8',fillOpacity:.12,zIndex:1
     });
    }else{
     accuracyCircle.setCenter(point);
     accuracyCircle.setRadius(accuracy);
     accuracyCircle.setMap(googleMap);
    }
   }else if(accuracyCircle)accuracyCircle.setMap(null);
  }catch{if(accuracyCircle)try{accuracyCircle.setMap(null);}catch{}}
  return shown;
 }
 function ready(){
  if(!loading)return;
  clearTimeout(loadTimer);loadTimer=null;
  try{
   if(!globalThis.google?.maps?.Map)throw Error('Google Maps nicht geladen');
   googleMap=new google.maps.Map(mapEl,{
    center:position||{lat:51.1657,lng:10.4515},zoom:position?16:6,
    mapTypeControl:false,fullscreenControl:false,streetViewControl:false,
    gestureHandling:'greedy',clickableIcons:true
   });
   empty.hidden=true;loading=false;
   if(position)showPosition(position,positionAccuracy);
   if(typeof googleMap.addListener==='function'){
    googleMap.addListener('dragstart',()=>{
     followPosition=false;
     if(positionDot)positionDot.hidden=true;
     if(trackingRequested)setStatus('GPS live · Karte frei verschoben · Fadenkreuz zum Folgen');
    });
   }
   setStatus('Google-Karte aktiv · noch keine Abbiegenavigation');
  }catch{showUnconfigured('Google-Karte konnte nicht gestartet werden.');}
 }
 globalThis[CALLBACK]=ready;
 globalThis.gm_authFailure=()=>showUnconfigured('Google hat den Demo-Zugang abgelehnt oder das Kontingent erreicht.');
 function loadGoogle(key){
  if(!GOOGLE_KEY.test(key))return showUnconfigured();
  if(googleMap||loading)return;
  loading=true;empty.hidden=true;setStatus('Google-Karte wird geladen …');
  script=document.createElement('script');script.async=true;script.referrerPolicy='origin';
  script.src=scriptUrl(key);
  script.onerror=()=>showUnconfigured('Google-Karte ist derzeit nicht erreichbar.');
  loadTimer=setTimeout(()=>showUnconfigured('Zeitüberschreitung beim Google-Kartenstart.'),18000);
  document.head.append(script);
 }
 // On-device location is tracked only while the map page is in the foreground.
 // No coordinates are stored, logged, or sent to a separate location service.
 function updateGpsButton(){
  if(!gpsToggle)return;
  gpsToggle.hidden=!trackingRequested&&!position;
  gpsToggle.textContent=trackingRequested?'GPS Pause':'GPS Start';
  gpsToggle.setAttribute('aria-pressed',String(trackingRequested));
  gpsToggle.setAttribute('aria-label',
   trackingRequested?'Automatische GPS-Aktualisierung pausieren':'Automatische GPS-Aktualisierung starten');
 }
 function stopWatching(){
  if(gpsWatchId!==null){
   try{navigator.geolocation?.clearWatch?.(gpsWatchId);}catch{}
   gpsWatchId=null;
  }
 }
 function updateFromFix(fix,first){
  if(!googleMap||!fix?.coords)return false;
  const lat=Number(fix.coords.latitude),lng=Number(fix.coords.longitude);
  if(!Number.isFinite(lat)||!Number.isFinite(lng)||lat>90||lat< -90||lng>180||lng< -180)return false;
  position={lat,lng};
  positionAccuracy=Number(fix.coords.accuracy);
  if(followPosition){
   googleMap.setCenter(position);
   if(first)googleMap.setZoom(16); // Do not fight the user's chosen zoom while tracking.
  }
  const visible=showPosition(position,positionAccuracy);
  updateGpsButton();
  if(!followPosition){
   setStatus(trackingRequested?'GPS live · freie Kartenansicht · Fadenkreuz zum Folgen':
    'Standort aktualisiert · Kartenansicht frei');
  }else{
   setStatus(visible
    ? (trackingRequested?'GPS live · blauer Punkt folgt deinem Standort · keine Abbiegenavigation':
       'Blauer Punkt: dein Standort · keine Abbiegenavigation')
    :'Standort zentriert, Positionssymbol nicht verfügbar.');
  }
  return visible;
 }
 function startWatching(){
  if(!trackingRequested||!googleMap||gpsWatchId!==null||document.hidden)return;
  if(typeof navigator.geolocation?.watchPosition!=='function'){
   trackingRequested=false;updateGpsButton();
   setStatus('Blauer Punkt: dein Standort · Live-GPS auf diesem Gerät nicht verfügbar.');
   return;
  }
  try{
   gpsWatchId=navigator.geolocation.watchPosition(fix=>{
    if(trackingRequested&&!document.hidden)updateFromFix(fix,false);
   },error=>{
    if(error?.code===1){
     stopWatching();trackingRequested=false;updateGpsButton();
     setStatus('Standortfreigabe verweigert · Live-GPS gestoppt.');
    }else{
     setStatus('GPS-Signal gestört · letzter bekannter Standort, nicht live.');
    }
   },{enableHighAccuracy:true,maximumAge:2000,timeout:15000});
  }catch{
   trackingRequested=false;updateGpsButton();
   setStatus('Live-GPS konnte nicht gestartet werden. Fadenkreuz für Einzelortung.');
  }
 }
 $('center').addEventListener('click',()=>{
  if(!googleMap){setStatus('Google-Karte zuerst mit der Live-Karte verbinden.');return;}
  if(!navigator.geolocation?.getCurrentPosition){
   setStatus('Standortbestimmung nicht verfügbar.');return;
  }
  // Explicitly request live tracking from this tap, never upon page load.
  followPosition=true;
  trackingRequested=true;
  if(positionDot)positionDot.hidden=false;
  setStatus('Standort wird bestimmt …');
  navigator.geolocation.getCurrentPosition(fix=>{
   updateFromFix(fix,true);
   startWatching();
  },error=>{
   if(error?.code===1){stopWatching();trackingRequested=false;updateGpsButton();}
   setStatus('Standort nicht freigegeben oder nicht verfügbar.');
  },{enableHighAccuracy:true,timeout:12000,maximumAge:3000});
 });
 gpsToggle?.addEventListener('click',()=>{
  if(trackingRequested){
   trackingRequested=false;
   stopWatching();
   setStatus('GPS pausiert · blauer Punkt zeigt den letzten Standort.');
  }else{
   trackingRequested=true;
   setStatus('Live-GPS startet · Fadenkreuz für automatische Zentrierung');
   startWatching();
  }
  updateGpsButton();
 });
 if(typeof document.addEventListener==='function'){
  document.addEventListener('visibilitychange',()=>{
   if(document.hidden){
    stopWatching();
    if(trackingRequested)setStatus('GPS im Hintergrund pausiert · kein aktives Tracking.');
   }else if(trackingRequested){
    setStatus('Live-GPS wird im Vordergrund fortgesetzt …');
    startWatching();
   }
  });
 }
 loadGoogle(stored());
})();