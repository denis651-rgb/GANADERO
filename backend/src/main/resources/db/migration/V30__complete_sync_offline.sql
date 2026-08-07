-- ETAPA 10 - Completar sincronización offline
-- 10.1 Estados canónicos de operación
alter table sync.operaciones drop constraint if exists ck_operacion_estado;

update sync.operaciones
set estado = case estado
    when 'PENDIENTE'   then 'PENDING'
    when 'PROCESANDO'  then 'PROCESSING'
    when 'APLICADA'    then 'SYNCED'
    when 'DUPLICADA'   then 'SYNCED'
    when 'RECHAZADA'   then 'REJECTED'
    when 'ERROR'       then 'RETRYABLE'
    when 'CONFLICTO'   then 'CONFLICT'
    else 'RETRYABLE'
end;

alter table sync.operaciones
    add constraint ck_operacion_estado check (estado in
        ('PENDING', 'PROCESSING', 'SYNCED', 'CONFLICT', 'REJECTED', 'RETRYABLE'));

-- 10.3 Idempotencia completa: hash del payload por operación
alter table sync.operaciones
    add column if not exists payload_hash varchar(64);

-- 10.9 Reintentos controlados
alter table sync.operaciones
    add column if not exists attempts integer not null default 0,
    add column if not exists next_retry_at timestamptz,
    add column if not exists last_error varchar(2000);

create index if not exists idx_operaciones_retry
    on sync.operaciones (empresa_id, estado)
    where estado in ('PENDING', 'RETRYABLE');

-- 10.6 Cambios de permisos: registrar altas/bajas de membresías en el pull
-- `registrar_cambio` se vuelve tolerante a filas sin empresa (perfiles, roles)
-- y el trigger se instala en miembros_empresa, la tabla que sí tiene empresa_id.
create or replace function sync.registrar_cambio()
returns trigger
language plpgsql
as
$$
declare
    registro jsonb;
    empresa_uuid uuid;
    entidad_uuid uuid;
    dispositivo_uuid uuid;
begin
    if tg_op = 'DELETE' then
        registro := to_jsonb(old);
    else
        registro := to_jsonb(new);
    end if;

    entidad_uuid :=
        nullif(registro ->> 'id', '')::uuid;

    if tg_table_schema = 'core'
       and tg_table_name = 'empresas' then

        empresa_uuid := entidad_uuid;
    else
        empresa_uuid :=
            nullif(registro ->> 'empresa_id', '')::uuid;
    end if;

    if empresa_uuid is null then
        return null;
    end if;

    begin
        dispositivo_uuid :=
            nullif(
                current_setting(
                    'app.dispositivo_id',
                    true
                ),
                ''
            )::uuid;
    exception
        when others then
            dispositivo_uuid := null;
    end;

    insert into sync.cambios (
        empresa_id,
        tabla,
        entidad_id,
        tipo_cambio,
        datos,
        dispositivo_origen
    )
    values (
        empresa_uuid,
        tg_table_schema || '.' || tg_table_name,
        entidad_uuid,
        tg_op,
        registro,
        dispositivo_uuid
    );

    if tg_op = 'DELETE' then
        return old;
    end if;

    return new;
end;
$$;

create trigger trg_cambio_miembros_empresa
    after insert or update or delete on seguridad.miembros_empresa
    for each row execute function sync.registrar_cambio();
