/* Single-use on-device key handoff for an already configured Live-Karte.
 * AES-GCM key arrives in a URL *fragment*, never in an HTTP request.
 * The actual Google Demo Key stays in browser localStorage and is sent only
 * as AES-GCM ciphertext to the installed FahrCockpit app on a user click.
 */
(async()=>{'use strict';
 const display=document.getElementById('message'),link=document.getElementById('send');
 const VALID_GOOGLE_KEY=/^AIza[0-9A-Za-z_-]{25,90}$/;
 function decode(s){
  if(!/^[A-Za-z0-9_-]{43}$/.test(s||''))throw Error('Sitzung ungültig');
  return Uint8Array.from(atob(s.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));
 }
 function encode(bytes){
  let raw='';for(const b of bytes)raw+=String.fromCharCode(b);
  return btoa(raw).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
 }
 const code=new URLSearchParams(location.hash.slice(1)).get('k')||'';
 // Remove session material from visible address/history before any other work.
 history.replaceState(null,'',location.pathname);
 if(!globalThis.crypto?.subtle){display.textContent='Sicherer Browser-Speicher nicht verfügbar.';return;}
 let browserKey='';
 try{
  browserKey=localStorage.getItem('livekarte-google-demo-key-v1')
    ||localStorage.getItem('fahrcockpit-map-demo-key-v1')||'';
 }catch{}
 if(!VALID_GOOGLE_KEY.test(browserKey)){
  display.textContent='Hier ist noch kein Google-Demoschlüssel hinterlegt. Öffne deine Live-Karte in genau diesem Browser und aktiviere dort Google. Kehre anschließend zu FahrCockpit zurück.';
  return;
 }
 try{
  const secret=decode(code);
  const key=await crypto.subtle.importKey('raw',secret,{name:'AES-GCM'},false,['encrypt']);
  const iv=crypto.getRandomValues(new Uint8Array(12));
  const cipher=new Uint8Array(await crypto.subtle.encrypt({name:'AES-GCM',iv},
    key,new TextEncoder().encode(browserKey)));
  const packed=new Uint8Array(iv.length+cipher.length);
  packed.set(iv);packed.set(cipher,iv.length);
  // Chrome's intent URL specifies the package explicitly; no key in URL.
  link.href='intent://transfer?data='+encode(packed)
    +'#Intent;scheme=fahrcockpit;package=com.cobrachris.fahrcockpit;end';
  link.hidden=false;
  display.textContent='Deine vorhandene Google-Konfiguration wurde gefunden. Tippe einmal auf „Mit FahrCockpit verbinden“.';
 }catch{
  display.textContent='Die Übernahme konnte nicht vorbereitet werden. Starte sie erneut aus FahrCockpit.';
 }
})();