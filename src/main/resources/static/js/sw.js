'use strict';

const CACHE_NAME = `taskflow-static-v1-${Date.now()}`;
const OFFLINE_URL = '/login';

const ASSETS_TO_CACHE = [
    '/',
    '/login',
    '/cadastro',
    '/css/landing.css',
    '/css/login.css',
    '/js/landing.js',
    '/js/login.js',
    '/favicon.ico',
    '/site.webmanifest',
    '/apple-touch-icon.png',
    '/favicon-32x32.png',
    '/favicon-16x16.png'
];

// ==================== INSTALL ====================
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(async (cache) => {
            for (const url of ASSETS_TO_CACHE) {
                try {
                    await cache.add(url);
                } catch (err) {
                    console.warn('[SW] Falha ao cachear:', url);
                }
            }
        })
    );

    self.skipWaiting();
});

// ==================== ACTIVATE ====================
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(
                keys.map((key) => {
                    if (key !== CACHE_NAME) {
                        return caches.delete(key);
                    }
                })
            )
        )
    );

    self.clients.claim();
});

// ==================== FETCH STRATEGY ====================
self.addEventListener('fetch', (event) => {
    const req = event.request;

    if (
        req.method !== 'GET' ||
        req.url.includes('/api/v1/') ||
        req.url.includes('chrome-extension')
    ) {
        return;
    }

    event.respondWith(networkFirstWithCache(req));
});

// ==================== STRATEGY ====================
async function networkFirstWithCache(request) {
    try {
        const networkResponse = await fetch(request);

        const cache = await caches.open(CACHE_NAME);

        if (networkResponse && networkResponse.status === 200) {
            cache.put(request, networkResponse.clone());
        }

        return networkResponse;

    } catch (err) {
        const cached = await caches.match(request);

        if (cached) return cached;

        // fallback SPA / login route
        if (request.mode === 'navigate') {
            return caches.match(OFFLINE_URL);
        }

        return new Response('Offline', {
            status: 503,
            statusText: 'Offline'
        });
    }
}