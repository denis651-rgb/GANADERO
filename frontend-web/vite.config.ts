import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiUrl = env.VITE_API_URL || 'http://localhost:8080'

  return {
    plugins: [react()],
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
      // Las pruebas corren en la zona horaria del negocio (Bolivia, UTC-4), no en la de la máquina: así un
      // error de fechas (new Date('2024-01-01') mostrando el 31/12/2023) se detecta igual en cualquier CI.
      env: { TZ: 'America/La_Paz' },
    },
  }
})
