/* FahrCockpit map host: immediate keyless OSM + OPTIONAL Google Demo map.
 * No routing or turn-by-turn navigation in either provider.
 * Leaflet is vendored (BSD-2), live tiles require network and OSMF policy.
 */
(()=>{'use strict';
 const KEY='fahrcockpit-map-demo-key-v1';
 const SCRIPT_BASE='https://maps.googleapis.com/maps/api/js';
 const CALLBACK='__fahrcockpitMapsReady';
 const GOOGLE_KEY=/^AIza[0-9A-Za-z_-]{25,90}$/;
 const $=id=>document.getElementById(id);
 const mapEl=$('google-map'),osmEl=$('osm-map'),status=$('status');
 const panel=$('settings-panel'),keyInput=$('key'),googleBtn=$('google'),osmBtn=$('osm');
 let googleMap=null,osmMap=null,provider='osm',loading=false,script=null,loadTimer=null,position=null,positionMarker=null;
 const setStatus=msg=>{status.textContent=msg;};
 const stored=()=>{try{return localStorage.getItem(KEY)||'';}catch{return '';}};
 const save=key=>{try{localStorage.setItem(KEY,key);return true;}catch{return false;}};
 const forget=()=>{try{localStorage.removeItem(KEY);}catch{}};
 const scriptUrl=key=>SCRIPT_BASE+'?'+new URLSearchParams({key,loading:'async',callback:CALLBACK,auth_referrer_policy:'origin',v:'weekly',language:'de'}).toString();
 const point=()=>position||{lat:51.1657,lng:10.4515};
 function showPanel(){panel.hidden=false;keyInput.value='';keyInput.focus();}
 function hidePanel(){panel.hidden=true;keyInput.value='';}
 function enableGoogleButton(){googleBtn.hidden=!GOOGLE_KEY.test(stored());}
 function updateButtons(){osmBtn.setAttribute('aria-pressed',String(provider==='osm'));googleBtn.setAttribute('aria-pressed',String(provider==='google'));}
 function startOSM(){
  if(!globalThis.L?.map){setStatus('OpenStreetMap konnte nicht geladen werden.');return;}
  // Render immediately, without requesting any credentials or location.
  osmMap=L.map(osmEl,{zoomControl:true,attributionControl:true,preferCanvas:true}).setView([51.1657,10.4515],6);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{
   maxZoom:19,minZoom:2,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap-Mitwirkende</a>'
  }).addTo(osmMap);
  setStatus('OpenStreetMap aktiv · keine Abbiegenavigation');
  updateButtons();
 }
 function selectOSM(message){
  provider='osm';osmEl.hidden=false;mapEl.hidden=true;
  if(osmMap){setTimeout(()=>osmMap.invalidateSize(),50);}
  if(message)setStatus(message);
  updateButtons();hidePanel();
 }
 function failGoogle(message){
  loading=false;clearTimeout(loadTimer);loadTimer=null;
  if(script){script.remove();script=null;}
  selectOSM(message+' OpenStreetMap bleibt verfügbar.');
 }
 function mapsReady(){
  if(!loading)return;clearTimeout(loadTimer);loadTimer=null;
  try{
   if(!globalThis.google?.maps?.Map)throw Error('Google nicht verfügbar');
   if(!googleMap){googleMap=new google.maps.Map(mapEl,{
    center:point(),zoom:position?16:6,mapTypeControl:false,
    fullscreenControl:false,streetViewControl:false,gestureHandling:'greedy',clickableIcons:true
   });}
   provider='google';mapEl.hidden=false;osmEl.hidden=true;
   googleMap.setCenter(point());
   loading=false;hidePanel();updateButtons();
   setStatus('Google-Demokarte aktiv · keine Abbiegenavigation');
  }catch{failGoogle('Google-Demo konnte nicht angezeigt werden.');}
 }
 globalThis[CALLBACK]=mapsReady;
 globalThis.gm_authFailure=()=>failGoogle('Google-Demo-Zugang abgelehnt.');
 function loadGoogle(key){
  if(!GOOGLE_KEY.test(key)){setStatus('Google Demo Key optional: gültigen Schlüssel eingeben oder OSM nutzen.');return;}
  if(googleMap){provider='google';mapEl.hidden=false;osmEl.hidden=true;updateButtons();hidePanel();setStatus('Google-Demokarte aktiv');return;}
  if(loading)return;
  loading=true;hidePanel();setStatus('Optionale Google-Karte lädt …');
  script=document.createElement('script');script.async=true;script.referrerPolicy='origin';
  script.src=scriptUrl(key);script.onerror=()=>failGoogle('Google-Karte nicht erreichbar.');
  loadTimer=setTimeout(()=>failGoogle('Google-Karte lädt zu lange.'),18000);
  document.head.append(script);
 }
 $('settings').addEventListener('click',showPanel);
 $('close').addEventListener('click',hidePanel);
 osmBtn.addEventListener('click',()=>selectOSM('OpenStreetMap aktiv · keine Abbiegenavigation'));
 googleBtn.addEventListener('click',()=>loadGoogle(stored()));
 $('forget').addEventListener('click',()=>{
  forget();enableGoogleButton();selectOSM('Optionale Google-Zugangsdaten gelöscht. OpenStreetMap aktiv.');
 });
 $('key-form').addEventListener('submit',event=>{
  event.preventDefault();const key=keyInput.value.trim();
  if(!GOOGLE_KEY.test(key)){setStatus('Google Demo Key nicht angegeben. OpenStreetMap bleibt aktiv.');return;}
  if(!save(key)){setStatus('Speicherfehler – OpenStreetMap bleibt aktiv.');return;}
  enableGoogleButton();
  if(googleMap){setStatus('Google-Key gespeichert. Für den Wechsel Karte neu öffnen.');hidePanel();return;}
  loadGoogle(key);
 });
 $('center').addEventListener('click',()=>{
  if(!navigator.geolocation){setStatus('Standortbestimmung nicht verfügbar.');return;}
  setStatus('Standortfreigabe wird angefragt …');
  navigator.geolocation.getCurrentPosition(pos=>{
   const lat=pos.coords.latitude,lng=pos.coords.longitude;
   if(!Number.isFinite(lat)||!Number.isFinite(lng))return setStatus('Kein gültiger Standort.');
   position={lat,lng};
   if(provider==='google'&&googleMap){googleMap.setCenter(position);googleMap.setZoom(16);}
   else if(osmMap){
    osmMap.setView([lat,lng],16);
    if(positionMarker){positionMarker.setLatLng([lat,lng]);}
    else {positionMarker=L.circleMarker([lat,lng],{radius:8,weight:3,color:'#fff',fillColor:'#267bfa',fillOpacity:1}).addTo(osmMap);}
   }
   setStatus('Auf deinen Standort zentriert · keine Abbiegenavigation');
  },()=>setStatus('Standort nicht freigegeben oder nicht verfügbar.'),
  {enableHighAccuracy:true,timeout:12000,maximumAge:3000});
 });
 startOSM();
 enableGoogleButton();
})();