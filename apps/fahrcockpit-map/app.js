/* FahrCockpit Karte: ausschliesslich Google Maps JavaScript API Demo.
 * Eigener HTTPS-Ursprung; kein Directions, Places, Navigation SDK oder Billing.
 * Demo-Key bleibt im Browserspeicher dieses Ursprungs, niemals im Repository.
 */
(()=>{'use strict';
 const KEY='fahrcockpit-map-demo-key-v1';
 const SCRIPT_BASE='https://maps.googleapis.com/maps/api/js';
 const CALLBACK='__fahrcockpitMapsReady';
 const GOOGLE_KEY=/^AIza[0-9A-Za-z_-]{25,90}$/;
 const $=id=>document.getElementById(id);
 const mapEl=$('google-map'),placeholder=$('placeholder'),status=$('status');
 const panel=$('settings-panel'),keyInput=$('key');
 let map=null,loading=false,script=null,loadTimer=null;
 const setStatus=msg=>{status.textContent=msg;};
 const stored=()=>{try{return localStorage.getItem(KEY)||'';}catch{return '';}};
 const save=key=>{try{localStorage.setItem(KEY,key);return true;}catch{return false;}};
 const forget=()=>{try{localStorage.removeItem(KEY);}catch{}};
 function scriptUrl(key){
  const query=new URLSearchParams({key,loading:'async',callback:CALLBACK,auth_referrer_policy:'origin',v:'weekly',language:'de'});
  return SCRIPT_BASE+'?'+query.toString();
 }
 function showPanel(){panel.hidden=false;keyInput.value='';keyInput.focus();}
 function hidePanel(){panel.hidden=true;keyInput.value='';}
 function fail(message){
  loading=false;clearTimeout(loadTimer);loadTimer=null;
  if(script){script.remove();script=null;}
  setStatus(message);placeholder.hidden=false;
 }
 function mapsReady(){
  if(!loading)return;
  clearTimeout(loadTimer);loadTimer=null;
  try{
   if(!globalThis.google?.maps?.Map)throw Error('API nicht vorhanden');
   map=new google.maps.Map(mapEl,{
    center:{lat:53.5511,lng:9.9937},zoom:12,
    mapTypeControl:false,fullscreenControl:false,streetViewControl:false,
    gestureHandling:'greedy',clickableIcons:true
   });
   placeholder.hidden=true;hidePanel();loading=false;
   setStatus('Google-Karte aktiv · keine Abbiegenavigation');
  }catch{fail('Google-Karte konnte nicht erstellt werden. Demo-Schlüssel prüfen.');}
 }
 globalThis[CALLBACK]=mapsReady;
 globalThis.gm_authFailure=()=>fail('Google hat den Demo-Zugang abgelehnt. Schlüssel oder Kontingent prüfen.');
 function loadMap(key){
  if(!GOOGLE_KEY.test(key)){setStatus('Bitte einen gültigen Demo-Schlüssel eingeben.');return;}
  if(map){hidePanel();setStatus('Google-Karte aktiv · keine Abbiegenavigation');return;}
  if(loading)return;
  loading=true;hidePanel();setStatus('Google-Demokarte wird geladen …');
  script=document.createElement('script');script.async=true;script.referrerPolicy='origin';
  script.src=scriptUrl(key);script.onerror=()=>fail('Google nicht erreichbar. Verbindung und Schlüssel prüfen.');
  loadTimer=setTimeout(()=>fail('Zeitüberschreitung beim Laden der Google-Karte.'),18000);
  document.head.append(script);
 }
 $('settings').addEventListener('click',showPanel);
 $('setup').addEventListener('click',showPanel);
 $('close').addEventListener('click',hidePanel);
 $('forget').addEventListener('click',()=>{
  forget();setStatus('Demo-Schlüssel vom Gerät entfernt.');hidePanel();
  // An already loaded map remains until refresh; no accidental removal of other app data.
 });
 $('key-form').addEventListener('submit',event=>{
  event.preventDefault();const key=keyInput.value.trim();
  if(!GOOGLE_KEY.test(key)){setStatus('Schlüssel hat kein gültiges Format.');return;}
  if(!save(key)){setStatus('Lokaler Speicher nicht verfügbar – Schlüssel nicht gespeichert.');return;}
  if(map){setStatus('Neuer Schlüssel gespeichert. Karte für einen Wechsel neu öffnen.');hidePanel();return;}
  loadMap(key);
 });
 $('center').addEventListener('click',()=>{
  if(!map){setStatus('Zuerst Google-Demokarte aktivieren.');return;}
  if(!navigator.geolocation){setStatus('Standortbestimmung nicht verfügbar.');return;}
  setStatus('Standortfreigabe wird angefragt …');
  navigator.geolocation.getCurrentPosition(pos=>{
   const lat=pos.coords.latitude,lng=pos.coords.longitude;
   if(!Number.isFinite(lat)||!Number.isFinite(lng))return setStatus('Kein gültiger Standort.');
   map.setCenter({lat,lng});map.setZoom(16);
   setStatus('Auf deinen Standort zentriert · keine Abbiegenavigation');
  },()=>setStatus('Standort nicht freigegeben oder nicht verfügbar.'),
  {enableHighAccuracy:true,timeout:12000,maximumAge:3000});
 });
 const old=stored();if(GOOGLE_KEY.test(old))loadMap(old);
})();
