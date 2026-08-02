# Base de datos

Las migraciones ejecutables de Flyway permanecen en `backend/src/main/resources/db/migration` para que Spring Boot las incluya en el JAR. Flyway es la única autoridad para modificar el esquema y una migración ya aplicada nunca debe editarse ni renombrarse.

`database/migrations` queda reservado para documentación, exportaciones o una futura reorganización controlada; no contiene una segunda copia de las migraciones. `database/seeds` y `database/diagrams` alojan material no ejecutable cuando exista.
