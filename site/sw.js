// Caches sind nach PWA-Pfad getrennt. Andere Apps auf github.io bleiben unberührt.
const SCOPE = new URL(self.registration.scope);
const CACHE_PREFIX = 'app-shell:' + SCOPE.pathname + ':';
const CACHE_NAME = CACHE_PREFIX + 'v13';
const ASSETS = ['./', './index.html', './styles.css', './app.js', './core.mjs', './briefs.mjs', './github-links.mjs', './projects.mjs', './github-import.mjs', './quickstart.mjs', './manifest.webmanifest', './icons/app.svg'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME)
      .map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== SCOPE.origin ||
      !url.pathname.startsWith(SCOPE.pathname)) return;
  event.respondWith((async () => {
    try {
      const response = await fetch(event.request);
      if (response.ok) {
        const cache = await caches.open(CACHE_NAME);
        await cache.put(event.request, response.clone());
      }
      return response;
    } catch {
      return (await caches.match(event.request)) || Response.error();
    }
  })());
});
