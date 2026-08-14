# Cron de Supabase para el motor de alertas

Ejecute `setup_cron.sql` manualmente en el SQL Editor de producción y reemplace
`<BACKEND_URL>` y `<GANADERO_CRON_SECRET>`.

## Backend

- `GANADERO_ALERTAS_SCHEDULER_ENABLED=false`
- `APP_INTERNAL_JOBS_ENABLED=true`
- `GANADERO_CRON_SECRET=<valor aleatorio largo>`

Los endpoints `/api/internal/jobs/**` no usan JWT de usuarios. Exigen el encabezado
`X-Ganadero-Cron-Secret`, validado mediante comparación de tiempo constante. Cuando
los jobs están deshabilitados responden como recurso inexistente y un secreto inválido
responde HTTP 401.

## Supabase

El script guarda la URL y el secreto en Vault. Los trabajos de `pg_cron` los leen desde
`vault.decrypted_secrets`, por lo que el secreto no queda escrito directamente en la
definición visible de cada job.

| Job | Endpoint | Frecuencia |
|---|---|---|
| `ganadero-activar-alertas` | `POST /api/internal/jobs/alertas/activar` | cada 5 min |
| `ganadero-procesar-notificaciones` | `POST /api/internal/jobs/notificaciones/procesar` | cada 5 min |
| `ganadero-generar-alertas-pesajes` | `POST /api/internal/jobs/alertas/pesajes/generar` | diariamente 00:15 Bolivia |

Verificación:

- `select * from cron.job;`
- `select * from net._http_response order by created desc limit 5;`
