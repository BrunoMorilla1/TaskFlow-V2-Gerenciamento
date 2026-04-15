'use strict';

const CACHE_NAME = 'taskflow-static-v1';
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

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return Promise.allSettled(
                ASSETS_TO_CACHE.map(url => {
                    return cache.add(url).catch(err => console.error('Falha ao cachear:', url, err));
                })
            );
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cache) => {
                    if (cache !== CACHE_NAME) {
                        return caches.delete(cache);
                    }
                })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    if (event.request.method !== 'GET' ||
        event.request.url.includes('/api/v1/') ||
        event.request.url.includes('chrome-extension')) {
        return;
    }

    event.respondWith(
        caches.match(event.request).then((cachedResponse) => {
            const fetchPromise = fetch(event.request).then((networkResponse) => {
                if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
                    const responseToCache = networkResponse.clone();
                    caches.open(CACHE_NAME).then((cache) => {
                        cache.put(event.request, responseToCache);
                    });
                }
                return networkResponse;
            }).catch(() => {
                if (event.request.mode === 'navigate') {
                    return cachedResponse || caches.match(OFFLINE_URL);
                }
                return cachedResponse;
            });

            return cachedResponse || fetchPromise;
        })
    );
});