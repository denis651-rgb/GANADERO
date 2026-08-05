create table sync.dispositivos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    usuario_id uuid not null references seguridad.perfiles_usuario(id),
    codigo_dispositivo varchar(120) not null,
    nombre varchar(160),
    plataforma varchar(30) not null default 'WEB',
    version_app varchar(60),
    estado varchar(20) not null default 'ACTIVO',
    ultimo_seen_at timestamptz not null default now(),
    ultimo_cursor bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_dispositivo_empresa_codigo unique (empresa_id, codigo_dispositivo),
    constraint ck_dispositivo_plataforma check (plataforma in ('WEB','ANDROID','IOS')),
    constraint ck_dispositivo_estado check (estado in ('ACTIVO','BLOQUEADO','DADO_DE_BAJA'))
);

create table sync.operaciones (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    dispositivo_id uuid not null references sync.dispositivos(id),
    usuario_id uuid not null,
    cliente_id uuid not null,
    tipo varchar(40) not null,
    entidad varchar(40) not null,
    entidad_id uuid,
    datos jsonb not null,
    version_cliente bigint not null default 0,
    estado varchar(20) not null default 'PENDIENTE',
    resultado_codigo varchar(60),
    resultado_mensaje varchar(500),
    resultado_servidor jsonb,
    version_servidor bigint,
    conflictos jsonb,
    idempotency_key varchar(200),
    created_at timestamptz not null default now(),
    applied_at timestamptz,
    constraint uq_operacion_empresa_dispositivo_cliente unique (empresa_id, dispositivo_id, cliente_id),
    constraint ck_operacion_estado check (estado in ('PENDIENTE','APLICADA','DUPLICADA','RECHAZADA','ERROR','CONFLICTO'))
);

create index idx_operaciones_empresa_estado on sync.operaciones(empresa_id, estado, created_at);
create index idx_operaciones_entidad on sync.operaciones(empresa_id, entidad, entidad_id);

create table sync.cambios (
    id bigserial primary key,
    empresa_id uuid not null,
    tabla varchar(60) not null,
    entidad_id uuid not null,
    tipo_cambio varchar(20) not null,
    datos jsonb not null,
    dispositivo_origen varchar(120),
    created_at timestamptz not null default now(),
    constraint ck_cambio_tipo check (tipo_cambio in ('INSERT','UPDATE','DELETE'))
);

create index idx_cambios_empresa_id on sync.cambios(empresa_id, id);
create index idx_cambios_entidad on sync.cambios(empresa_id, tabla, entidad_id);

create or replace function sync.registrar_cambio()
returns trigger language plpgsql as $$
declare
    dev varchar(120);
begin
    dev := nullif(current_setting('sync.dispositivo', true), '');
    if tg_op = 'DELETE' then
        insert into sync.cambios(empresa_id, tabla, entidad_id, tipo_cambio, datos, dispositivo_origen)
        values (old.empresa_id, tg_table_name, old.id, 'DELETE', row_to_json(old)::jsonb, dev);
        return old;
    end if;
    insert into sync.cambios(empresa_id, tabla, entidad_id, tipo_cambio, datos, dispositivo_origen)
    values (new.empresa_id, tg_table_name, new.id, tg_op::text, row_to_json(new)::jsonb, dev);
    return new;
end $$;

create trigger trg_cambio_animales after insert or update or delete on ganado.animales
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_identificadores after insert or update or delete on ganado.identificadores_animal
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_lotes after insert or update or delete on ganado.lotes_ganaderos
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_membresias after insert or update or delete on ganado.membresias_lote
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_movimientos after insert or update or delete on ganado.movimientos
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_movimiento_detalles after insert or update or delete on ganado.movimiento_detalles
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_pesajes after insert or update or delete on produccion.pesajes
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_propiedades after insert or update or delete on core.propiedades
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_potreros after insert or update or delete on campo.potreros
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_empresas after insert or update or delete on core.empresas
    for each row execute function sync.registrar_cambio();
