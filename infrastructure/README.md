# Infraestructura

- `compose.yaml`: PostgreSQL/PostGIS local, expuesto en el puerto 55432.
- `render.yaml`: Blueprint de Render que construye `backend/Dockerfile` desde fuente.
- `backup/`: documentación y procedimientos de respaldo.
- `scripts/`: automatizaciones operativas versionables.

Desde la raíz, iniciar la base local con `docker compose -f infrastructure/compose.yaml up -d`.
