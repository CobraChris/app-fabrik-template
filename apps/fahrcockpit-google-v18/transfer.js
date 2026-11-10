/* Explicit local Google Demo Key pairing for FahrCockpit.
 * - Chrome and an installed Live-Karte may have different browser storage.
 * - The original key is never sent to GitHub or placed in a URL.
 * - On user action, a copied key is encrypted locally with a single-use
 *   AES-256-GCM session secret passed in an HTTPS URL fragment.
 * - Only authenticated ciphertext is returned via the installed-app intent.
 */
(async()=>{'use strict';
 const display=document.getElementById('message');
 const link=document.getElementById('send');
 const alternate=document.getElementById('alternate');
 const clipboard=document.getElementById('clipboard');
 const retry=document.getElementById('retry');
 const pasteForm=document.getElementById('paste-form');
 const pastedKey=document.getElementById('pasted-key');
 const VALID_GOOGLE_KEY=/^AIza[0-9A-Za-z_-]{25,90}$/;
 const MAX_SESSION_AGE_MS=4*60*1000;
 const startedAt=Date.now();
 const token=new URLSearchParams(location.hash.slice(1)).get('k')||'';
 // Remove the session secret from the visible URL/history immediately.
 history.replaceState(null,'',location.pathname);
 let cryptoKey=null,paired=false;
 function decode(s){
  if(!/^[A-Za-z0-9_-]{43}$/.test(s||''))throw Error('Sitzung ungültig');
  return Uint8Array.from(atob(s.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));
 }
 function encode(bytes){
  let raw='';for(const b of bytes)raw+=String.fromCharCode(b);
  return btoa(raw).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
 }
 function storageKey(){
  try{
   for(const name of ['livekarte-google-demo-key-v1','fahrcockpit-map-demo-key-v1']){
    const key=localStorage.getItem(name)||'';
    if(VALID_GOOGLE_KEY.test(key))return key;
   }
  }catch{}
  return '';
 }
 function sessionValid(){
  if(cryptoKey&&Date.now()-startedAt<MAX_SESSION_AGE_MS)return true;
  display.textContent='Die einmalige Verbindung ist abgelaufen. Öffne FahrCockpit und tippe erneut auf „Google aus Live-Karte übernehmen“.';
  alternate.hidden=true;link.hidden=true;
  return false;
 }
 async function prepare(key){
  if(!sessionValid())return;
  if(!VALID_GOOGLE_KEY.test(key)){
   display.textContent='Kein gültiger Google-Demoschlüssel gefunden. Kopiere ihn aus der installierten Live-Karte.';
   alternate.hidden=false;link.hidden=true;
   return;
  }
  try{
   const iv=crypto.getRandomValues(new Uint8Array(12));
   const encrypted=new Uint8Array(await crypto.subtle.encrypt({name:'AES-GCM',iv},
     cryptoKey,new TextEncoder().encode(key)));
   const packet=new Uint8Array(iv.length+encrypted.length);
   packet.set(iv);packet.set(encrypted,iv.length);
   link.href='intent://transfer?data='+encode(packet)
    +'#Intent;scheme=fahrcockpit;package=com.cobrachris.fahrcockpit;end';
   link.hidden=false;alternate.hidden=true;paired=true;
   display.textContent='Google-Demoschlüssel gefunden. Tippe auf „Mit FahrCockpit verbinden“, um ihn verschlüsselt zu übertragen.';
   pastedKey.value='';
  }catch{
   display.textContent='Die verschlüsselte Übernahme konnte nicht vorbereitet werden. Starte sie erneut aus FahrCockpit.';
  }
 }
 async function checkBrowserKey(){
  if(paired||!sessionValid())return;
  const key=storageKey();
  if(VALID_GOOGLE_KEY.test(key)){await prepare(key);return;}
  alternate.hidden=false;
  display.textContent='Dieser Chrome-Browser hat keinen gespeicherten Google-Demoschlüssel. Nutze die Kopieren-Funktion deiner installierten Live-Karte.';
 }
 clipboard.addEventListener('click',async()=>{
  if(!sessionValid())return;
  try{
   if(!navigator.clipboard?.readText)throw Error('clipboard unavailable');
   const key=await navigator.clipboard.readText();
   if(VALID_GOOGLE_KEY.test(key)){await prepare(key);return;}
   display.textContent='In der Zwischenablage liegt kein Google-Demoschlüssel. Kopiere ihn zunächst aus deiner Live-Karte.';
  }catch{
   display.textContent='Chrome hat das automatische Lesen nicht erlaubt. Du kannst den kopierten Schlüssel alternativ in das Feld unten einfügen.';
   pasteForm.hidden=false;
   pastedKey.focus();
  }
 });
 retry.addEventListener('click',()=>{void checkBrowserKey();});
 pasteForm.addEventListener('submit',event=>{
  event.preventDefault();
  const value=pastedKey.value.trim();
  void prepare(value);
 });
 document.addEventListener('visibilitychange',()=>{
  if(!document.hidden)void checkBrowserKey();
 });
 window.addEventListener('focus',()=>{void checkBrowserKey();});
 if(!globalThis.crypto?.subtle){
  display.textContent='Sichere Browser-Verschlüsselung ist nicht verfügbar. Prüfe, dass Chrome HTTPS verwendet.';
  return;
 }
 try{
  const secret=decode(token);
  cryptoKey=await crypto.subtle.importKey('raw',secret,{name:'AES-GCM'},false,['encrypt']);
  secret.fill(0);
  await checkBrowserKey();
 }catch{
  display.textContent='Ungültige Übernahmesitzung. Öffne FahrCockpit und starte die Google-Verknüpfung erneut.';
 }
})();