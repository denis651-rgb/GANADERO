# Bootstrap inicial

1. Configure temporalmente `APP_BOOTSTRAP_ENABLED=true`, un `APP_BOOTSTRAP_TOKEN` aleatorio y `SUPABASE_SERVICE_ROLE_KEY` solo en Render.
2. Envíe `POST /bootstrap/empresa-inicial` con `X-Bootstrap-Token` e `Idempotency-Key`.
3. Guarde los identificadores retornados y verifique empresa, propietario, rol y propiedad.
4. Configure inmediatamente `APP_BOOTSTRAP_ENABLED=false` y redepliegue.

Reutilizar una clave con otro payload devuelve conflicto. No incluya IDs, roles ni permisos arbitrarios en el cuerpo.
