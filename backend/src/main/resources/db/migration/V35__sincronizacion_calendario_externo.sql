-- Fase 2: modelo local de sincronizacion con un calendario externo.
create table configuracion_calendario_externo (
    id text primary key,
    empresa_id text not null,
    proveedor text not null default 'GOOGLE_CALENDAR',
    cuenta_email text,
    calendario_externo_id text,
    calendario_nombre text not null default 'Ganadero - Sanidad',
    zona_horaria text not null default 'America/La_Paz',
    sincronizacion_automatica integer not null default 0,
    estado text not null default 'DESCONECTADO',
    ultimo_error text,
    ultima_sincronizacion text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint uq_config_calendario_empresa unique (empresa_id, proveedor),
    constraint ck_config_calendario_estado check (estado in
        ('DESCONECTADO','PENDIENTE_AUTORIZACION','CONECTADO','ERROR','DESHABILITADO')),
    constraint ck_config_calendario_auto check (sincronizacion_automatica in (0,1))
);

create table correspondencia_calendario_externo (
    id text primary key,
    empresa_id text not null,
    ocurrencia_id text not null references ocurrencia_calendario_sanitario(id),
    proveedor text not null default 'GOOGLE_CALENDAR',
    evento_externo_id text not null,
    etag text,
    enlace_externo text,
    estado text not null default 'SINCRONIZADO',
    version_local_sincronizada integer not null default 0,
    fecha_actualizacion_externa text,
    ultima_sincronizacion text not null,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint uq_correspondencia_ocurrencia unique (ocurrencia_id, proveedor),
    constraint uq_correspondencia_evento unique (empresa_id, proveedor, evento_externo_id),
    constraint ck_correspondencia_estado check (estado in
        ('SINCRONIZADO','PENDIENTE_ACTUALIZAR','PENDIENTE_CANCELAR','ERROR','ELIMINADO'))
);

create table cola_sincronizacion_calendario (
    id text primary key,
    empresa_id text not null,
    ocurrencia_id text not null references ocurrencia_calendario_sanitario(id),
    proveedor text not null default 'GOOGLE_CALENDAR',
    operacion text not null,
    clave_idempotencia text not null,
    payload text,
    estado text not null default 'PENDIENTE',
    intentos integer not null default 0,
    max_intentos integer not null default 8,
    proximo_intento text not null default current_timestamp,
    bloqueado_por text,
    bloqueado_hasta text,
    ultimo_error text,
    codigo_error text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    completado_at text,
    version integer not null default 0,
    constraint uq_cola_calendario_idempotencia unique (clave_idempotencia),
    constraint ck_cola_calendario_operacion check (operacion in ('CREAR','ACTUALIZAR','CANCELAR','ELIMINAR')),
    constraint ck_cola_calendario_estado check (estado in
        ('PENDIENTE','PROCESANDO','REINTENTO','COMPLETADO','ERROR_DEFINITIVO','CANCELADO')),
    constraint ck_cola_calendario_intentos check (intentos >= 0 and max_intentos between 1 and 20)
);

create index idx_cola_calendario_disponible
    on cola_sincronizacion_calendario(estado, proximo_intento, created_at);
create index idx_cola_calendario_ocurrencia
    on cola_sincronizacion_calendario(ocurrencia_id, created_at);
