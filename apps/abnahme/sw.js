// Nur diese Demo-App verwalten; keine App-Fabrik- oder Fremd-Caches löschen.
const scope = new URL(self.registration.scope);
const prefix = 'app-shell:' + scope.pathname + ':';
const cacheName = prefix + 'v1';
const files = ['./', './index.html', './styles.css', './app.js', './counter.mjs',
  './manifest.webmanifest', './icons/app.svg'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(cacheName).then(cache => cache.addAll(files)));
  self.skipWaiting();
});
self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const old = await caches.keys();
    await Promise.all(old.filter(key => key.startsWith(prefix) && key !== cacheName)
      .map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== scope.origin ||
      !url.pathname.startsWith(scope.pathname)) return;
  event.respondWith((async () => {
    try {
      const response = await fetch(event.request);
      if (response.ok) (await caches.open(cacheName)).put(event.request, response.clone()).catch(() => {});
      return response;
    } catch {
      return (await caches.match(event.request)) || Response.error();
    }
  })());
});
