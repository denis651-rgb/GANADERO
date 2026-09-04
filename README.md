# GANADERO

GANADERO es una aplicación de gestión ganadera de escritorio para Windows: backend Spring
Boot embebido, SQLite local y una ventana Electron con el frontend React. Un solo usuario
local, sin login remoto ni dependencias de nube — todos los datos e imágenes viven en
`%APPDATA%/Ganadero/`.

## Estructura

```text
backend/             API Spring Boot modular, SQLite + Flyway
frontend-web/        aplicación React, TypeScript y Vite
electron/             shell de escritorio: proceso principal, empaquetado, instalador
database/            documentación de base de datos
docs/                arquitectura, API y manuales por aplicación
.github/workflows/   CI de backend y frontend
```

## Requisitos

- JDK 21 completo (con `jlink`/`jpackage`, no solo un JRE) — necesario para correr el
  backend y para que `electron/` genere el runtime embebido.
- Node.js en la versión indicada por `frontend-web/.node-version`.

No hace falta Docker ni una base de datos externa: SQLite vive en un archivo local.

## Ejecución local

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend (navegador, para desarrollo rápido de UI):

```bash
cd frontend-web
npm ci
npm run dev
```

Como app de escritorio (Electron, con el backend embebido):

```bash
cd electron
npm install
npm run dev
```

Ver `electron/README.md` para el flujo de empaquetado (instalador `.exe`).

## Verificación y build

```bash
cd backend
./mvnw clean verify
```

```bash
cd frontend-web
npm ci
npm run typecheck
npm run lint
npm run test
npm run build
```

## Ramas

- `main`: versión estable.
- `develop`: integración.
- `feature/*`: funcionalidades y estabilizaciones.
- `fix/*`: correcciones.

Los cambios se revisan mediante Pull Request; no se fusionan automáticamente ramas de trabajo.

## Migraciones

Las migraciones ejecutables están en `backend/src/main/resources/db/migration`. Flyway es la
autoridad del esquema. No editar ni renombrar migraciones aplicadas. Consulte
[database/README.md](database/README.md).

## Documentación

- [Backend](docs/backend/PRIMEROS_PASOS.md)
- [Frontend](docs/frontend/PRIMEROS_PASOS.md)
- [Escritorio (Electron)](electron/README.md)
