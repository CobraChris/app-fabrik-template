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
 const mapEl=$('google-map'),status=$('status'),empty=$('not-connected');
 let googleMap=null,loading=false,script=null,loadTimer=null,position=null;
 let positionMarker=null,accuracyCircle=null,positionAccuracy=null;
 let positionOverlay=null,positionDot=null;
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
    positionDot.hidden=false;
    shown=true;
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
 $('center').addEventListener('click',()=>{
  if(!googleMap){setStatus('Google-Karte zuerst mit der Live-Karte verbinden.');return;}
  if(!navigator.geolocation){setStatus('Standortbestimmung nicht verfügbar.');return;}
  setStatus('Standort wird bestimmt …');
  navigator.geolocation.getCurrentPosition(p=>{
   const lat=p.coords.latitude,lng=p.coords.longitude;
   if(!Number.isFinite(lat)||!Number.isFinite(lng))return setStatus('GPS-Daten ungültig.');
   position={lat,lng};
   positionAccuracy=Number(p.coords.accuracy);
   googleMap.setCenter(position);googleMap.setZoom(16);
   const visible=showPosition(position,positionAccuracy);
   setStatus(visible
    ?'Blauer Punkt: dein Standort · keine Abbiegenavigation'
    :'Standort zentriert, Positionssymbol nicht verfügbar.');
  },()=>setStatus('Standort nicht freigegeben oder nicht verfügbar.'),
  {enableHighAccuracy:true,timeout:12000,maximumAge:3000});
 });
 loadGoogle(stored());
})();