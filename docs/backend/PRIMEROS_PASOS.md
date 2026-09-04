# Primeros pasos — Backend

1. Abrir `backend/` en tu IDE con JDK 21 configurado.
2. Esperar que Maven descargue las dependencias (`./mvnw dependency:go-offline` si hace falta).
3. Ejecutar `GanaderoApplication` con el perfil `local` (por defecto).
4. Abrir:
   - `http://localhost:8080/actuator/health`
   - `http://localhost:8080/api/v1/system/status`
5. Ejecutar `./mvnw test` para correr la suite completa.

No hay base de datos externa que levantar: al iniciar, Flyway crea `./data/ganadero.db`
(SQLite) automáticamente a partir de `backend/src/main/resources/db/migration`. Para
cambiar la ubicación del archivo usa la variable `GANADERO_DB_PATH` (ver `backend/.env.example`).
