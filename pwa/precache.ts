import { createHash } from 'node:crypto';

/** Turns the emitted files into the sorted list of URLs (relative to the worker) to keep offline. */
export function precacheList(files: readonly string[]): string[] {
  const keep = files.filter(
    (f) => f !== 'sw.js' && !f.endsWith('.map') && !f.split('/').some((part) => part.startsWith('.'))
  );
  // The HTML is added to the bundle after plugins run, so always list the shell explicitly.
  return [...new Set(['./', './index.html', ...keep.map((f) => `./${f}`)])].sort();
}

/** Source of the service worker: precache everything, serve from cache, fall back to the network. */
/** `contentSeed` should change whenever any listed file's bytes change, so caches are renewed. */
export function buildServiceWorker(urls: readonly string[], contentSeed = ''): string {
  const version = createHash('sha1').update(urls.join('\n') + contentSeed).digest('hex').slice(0, 10);
  return `// Generated at build time. Cache version: ${version}
const CACHE = 'color-lines-${version}';
const PRECACHE = ${JSON.stringify(urls)};

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(PRECACHE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith('color-lines-') && k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;
  if (new URL(request.url).origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    // Pages: prefer the network so updates arrive, fall back to the cached shell when offline.
    event.respondWith(
      fetch(request).catch(() => caches.match('./', { ignoreSearch: true }).then((hit) => hit || caches.match('./index.html')))
    );
    return;
  }

  event.respondWith(
    // ignoreVary: module scripts send an Origin header the precached copies were stored without.
    caches.match(request, { ignoreVary: true }).then(
      (hit) =>
        hit ||
        fetch(request).then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        })
    )
  );
});
`;
}
