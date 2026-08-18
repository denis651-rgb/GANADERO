alter table alertas.entregas_notificacion
    add column if not exists proximo_intento_at timestamptz;

create index if not exists idx_entregas_reintento
    on alertas.entregas_notificacion(proximo_intento_at, created_at)
    where estado in ('PENDIENTE', 'ERROR');

create index if not exists idx_entregas_suscripcion
    on alertas.entregas_notificacion(suscripcion_id);

create index if not exists idx_alertas_animal_id
    on alertas.alertas(animal_id)
    where animal_id is not null;

create index if not exists idx_recordatorios_animal_id
    on alertas.recordatorios(animal_id)
    where animal_id is not null;

update alertas.alertas a
set estado = 'ERROR',
    enviada_at = null,
    ultimo_error = coalesce((
        select string_agg(distinct e.ultimo_error, '; ')
        from alertas.entregas_notificacion e
        where e.alerta_id = a.id and e.estado in ('ERROR', 'DESCARTADA')
    ), 'No se pudo entregar la notificación'),
    updated_at = now()
where a.estado = 'ENVIADA'
  and exists (
      select 1 from alertas.entregas_notificacion e
      where e.alerta_id = a.id and e.estado in ('ERROR', 'DESCARTADA')
  )
  and not exists (
      select 1 from alertas.entregas_notificacion e
      where e.alerta_id = a.id and e.estado = 'ENVIADA'
  );

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon')
       and exists (select 1 from pg_roles where rolname = 'authenticated') then
        execute 'revoke all privileges on table public.flyway_schema_history from anon, authenticated';
        if to_regclass('public.flyway_schema_history_backup_20260807') is not null then
            execute 'revoke all privileges on table public.flyway_schema_history_backup_20260807 from anon, authenticated';
        end if;
    end if;
end $$;

alter function sync.registrar_cambio() set search_path = pg_catalog;
alter function core.limpiar_idempotencia_expirada(integer) set search_path = pg_catalog;
alter function auditoria.fn_bloquear_modificacion() set search_path = pg_catalog;
