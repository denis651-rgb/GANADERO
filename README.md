# GANADERO

GANADERO es una aplicación de gestión ganadera organizada como monorepositorio. Conserva un backend modular Spring Boot, un frontend React y PostgreSQL/PostGIS, con integración de Supabase Auth y Storage.

## Estructura

```text
backend/             API Spring Boot y migraciones Flyway
frontend-web/        aplicación React, TypeScript y Vite
database/            documentación de base de datos
infrastructure/      Docker Compose, Render, respaldos y scripts
docs/                arquitectura, API, manual y documentación por aplicación
.github/workflows/   CI de backend y frontend
```

## Requisitos

- Java 21.
- Node.js en la versión indicada por `frontend-web/.node-version`.
- Docker.
- PostgreSQL/PostGIS (incluido en el Compose local).

## Variables de entorno

Use `.env.example` como catálogo y los ejemplos de cada aplicación como punto de partida. Copie únicamente las variables necesarias a archivos locales ignorados. Nunca confirme secretos ni exponga `SUPABASE_SERVICE_ROLE_KEY` en el frontend.

## Ejecución local

Base de datos:

```bash
docker compose -f infrastructure/compose.yaml up -d
```

Backend en Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Backend en Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd frontend-web
npm ci
npm run dev
```

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

Las migraciones ejecutables están en `backend/src/main/resources/db/migration`. Flyway es la autoridad del esquema. No editar ni renombrar migraciones aplicadas. Consulte [database/README.md](database/README.md).

## Documentación

- [Backend](docs/backend/PRIMEROS_PASOS.md)
- [Frontend](docs/frontend/PRIMEROS_PASOS.md)
