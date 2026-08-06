# GANADERO Frontend

Frontend inicial de GANADERO basado en la guía maestra del proyecto.

## Incluye

- React + TypeScript + Vite.
- Aplicación responsive para escritorio, tablet y móvil.
- PWA instalable con actualización controlada.
- Supabase Auth y modo de autenticación local para desarrollo.
- Cliente HTTP con JWT y `X-Correlation-Id`.
- TanStack Query para estado del servidor.
- React Hook Form + Zod para formularios.
- Dexie/IndexedDB para datos y operaciones offline.
- Cola inicial de sincronización e idempotencia.
- Navegación y carpetas para todos los módulos de la guía.
- Pantalla funcional de animales y formulario inicial.
- Pruebas con Vitest.
- Configuración para Cloudflare Pages.

## Requisitos

- Node.js 22.12 o superior.
- Backend GANADERO en `http://localhost:8080`.

## Primer arranque

```powershell
Copy-Item .env.example .env.local
npm ci
npm run dev
```

Abre `http://localhost:5173`.

En modo `mock`, utiliza los datos que aparecen precargados en el login. Para Supabase cambia:

```env
VITE_AUTH_MODE=supabase
VITE_SUPABASE_URL=https://TU_PROYECTO.supabase.co
VITE_SUPABASE_ANON_KEY=TU_ANON_KEY
```

## Verificaciones

```powershell
npm run typecheck
npm run lint
npm run test
npm run build
```

## Backend compatible

El panel consulta:

```text
GET /api/v1/system/status
```

La pantalla de animales queda preparada para:

```text
GET  /api/v1/animales
POST /api/v1/animales
```

Consulta `../docs/frontend/PRIMEROS_PASOS.md` y `../docs/frontend/ARQUITECTURA_FRONTEND.md`.

## Archivo `package-lock.json`

El archivo `package-lock.json` está versionado. Use siempre `npm ci` para instalaciones reproducibles y no lo regenere sin una actualización intencional de dependencias.
