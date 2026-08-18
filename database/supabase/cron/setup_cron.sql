-- Cron seguro para el motor de alertas.
-- Ejecute una sola vez y reemplace los dos valores entre <>.
-- Vault conserva los valores cifrados; los jobs no guardan el secreto en cron.job.
create extension if not exists pg_cron with schema extensions;
create extension if not exists pg_net with schema extensions;
create extension if not exists supabase_vault with schema vault;

select vault.create_secret('<BACKEND_URL>', 'ganadero_backend_url', 'URL del backend GANADERO');
select vault.create_secret('<GANADERO_CRON_SECRET>', 'ganadero_cron_secret', 'Secreto de Supabase Cron');

select cron.schedule(
    'ganadero-activar-alertas',
    '*/5 * * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/alertas/activar',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

select cron.schedule(
    'ganadero-procesar-notificaciones',
    '*/5 * * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/notificaciones/procesar',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

select cron.schedule(
    'ganadero-generar-alertas-pesajes',
    '15 4 * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/alertas/pesajes/generar',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

select cron.schedule(
    'ganadero-generar-alertas-vacunacion',
    '5 4 * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/alertas/vacunacion/generar',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

select cron.schedule(
    'ganadero-procesar-tratamientos-vencidos',
    '*/15 * * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/alertas/tratamientos/vencidos',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

select cron.schedule(
    'ganadero-procesar-recordatorios',
    '* * * * *',
    $$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_backend_url')
               || '/api/internal/jobs/alertas/recordatorios/procesar',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'X-Ganadero-Cron-Secret',
            (select decrypted_secret from vault.decrypted_secrets where name = 'ganadero_cron_secret')
        )
    )
    $$
);

-- Verificación: select * from cron.job;
-- Deshabilitar: select cron.unschedule('ganadero-activar-alertas');
--               select cron.unschedule('ganadero-procesar-notificaciones');
--               select cron.unschedule('ganadero-generar-alertas-pesajes');
--               select cron.unschedule('ganadero-generar-alertas-vacunacion');
--               select cron.unschedule('ganadero-procesar-tratamientos-vencidos');
--               select cron.unschedule('ganadero-procesar-recordatorios');
