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
   position={lat,lng};googleMap.setCenter(position);googleMap.setZoom(16);
   setStatus('Auf Standort zentriert · keine Abbiegenavigation');
  },()=>setStatus('Standort nicht freigegeben oder nicht verfügbar.'),
  {enableHighAccuracy:true,timeout:12000,maximumAge:3000});
 });
 loadGoogle(stored());
})();