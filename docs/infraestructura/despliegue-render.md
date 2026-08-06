# Despliegue en Render

Render usa `backend/Dockerfile`, Java 21 y build multietapa con `mvn clean verify`. Configure todas las variables de `backend/.env.example` como secretos del servicio. Mantenga bootstrap y estado técnico deshabilitados. El health check es `/actuator/health` y no expone detalles.

Para Supabase Pooler use `DATABASE_URL`, `DATABASE_USERNAME` y `DATABASE_PASSWORD`. También se aceptan los alias `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. Para las claves de verificación se acepta `SUPABASE_JWKS_URI` o `SUPABASE_JWKS_URL`.

`SUPABASE_SERVICE_ROLE_KEY` es exclusiva del backend. No cargue la clave publicable ni la clave anónima en Render, y nunca exponga la clave de servicio en Cloudflare Pages.
