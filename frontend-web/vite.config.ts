import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiUrl = env.VITE_API_URL || 'http://localhost:8080'

  return {
    plugins: [
      react(),
      VitePWA({
        registerType: 'prompt',
        includeAssets: [
          'logo.svg',
          'icons/icon-192.png',
          'icons/icon-512.png',
          'icons/icon-maskable-192.png',
          'icons/icon-maskable-512.png',
          'icons/apple-touch-icon-180.png',
        ],
        manifest: {
          id: '/',
          name: 'GANADERO',
          short_name: 'GANADERO',
          description: 'Gestión ganadera para web y trabajo de campo',
          theme_color: '#1f6a45',
          background_color: '#f4f7f2',
          display: 'standalone',
          display_override: ['standalone', 'minimal-ui'],
          start_url: '/',
          scope: '/',
          lang: 'es-BO',
          orientation: 'any',
          categories: ['business', 'agriculture', 'productivity'],
          icons: [
            { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
            { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
            { src: '/icons/icon-maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
            { src: '/icons/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          ],
        },
        workbox: {
          navigateFallback: '/index.html',
          navigateFallbackDenylist: [/^\/api\//],
          cleanupOutdatedCaches: true,
          clientsClaim: true,
          skipWaiting: false,
          ignoreURLParametersMatching: [/^utm_/, /^source/, /^v$/],
          runtimeCaching: [
            {
              urlPattern: ({ url, request }) => request.method === 'GET' && url.pathname.startsWith('/api/v1/auth/'),
              handler: 'NetworkOnly',
              method: 'GET',
            },
            {
              urlPattern: ({ url, request }) => request.method === 'GET' && url.pathname.startsWith('/api/v1/sync/'),
              handler: 'NetworkOnly',
              method: 'GET',
            },
            {
              urlPattern: ({ url, request }) => request.method === 'GET' && url.pathname.startsWith('/api/'),
              handler: 'NetworkFirst',
              method: 'GET',
              options: {
                cacheName: 'ganadero-api-v1',
                networkTimeoutSeconds: 4,
                expiration: { maxEntries: 150, maxAgeSeconds: 300 },
                cacheableResponse: { statuses: [0, 200] },
              },
            },
            {
              urlPattern: ({ url, request }) => request.method === 'GET' && /\.(png|jpe?g|gif|svg|webp|avif|ico)(\?.*)?$/.test(url.pathname),
              handler: 'CacheFirst',
              method: 'GET',
              options: {
                cacheName: 'ganadero-imagenes-v1',
                expiration: { maxEntries: 100, maxAgeSeconds: 7 * 24 * 60 * 60 },
                cacheableResponse: { statuses: [0, 200] },
              },
            },
          ],
        },
        devOptions: {
          enabled: true,
        },
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: apiUrl,
          changeOrigin: true,
        },
        '/actuator': {
          target: apiUrl,
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      css: true,
    },
  }
})
