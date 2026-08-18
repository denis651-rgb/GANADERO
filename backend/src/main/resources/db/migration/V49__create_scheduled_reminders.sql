create table alertas.recordatorios (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    creado_por uuid not null,
    titulo varchar(200) not null,
    mensaje varchar(1000) not null,
    severidad varchar(20) not null,
    animal_id uuid references ganado.animales(id),
    fecha_evento timestamptz not null,
    proxima_ejecucion timestamptz not null,
    cantidad_notificaciones integer not null,
    intervalo_minutos integer,
    notificaciones_generadas integer not null default 0,
    estado varchar(20) not null default 'ACTIVO',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint ck_recordatorio_severidad check (severidad in ('INFO','WARNING','URGENTE','CRITICA')),
    constraint ck_recordatorio_estado check (estado in ('ACTIVO','PAUSADO','COMPLETADO','CANCELADO')),
    constraint ck_recordatorio_cantidad check (cantidad_notificaciones between 1 and 10),
    constraint ck_recordatorio_intervalo check (
        (cantidad_notificaciones = 1 and intervalo_minutos is null)
        or (cantidad_notificaciones > 1 and intervalo_minutos >= 15)
    ),
    constraint ck_recordatorio_generadas check (
        notificaciones_generadas between 0 and cantidad_notificaciones
    ),
    constraint ck_recordatorio_fechas check (proxima_ejecucion <= fecha_evento)
);

create index idx_recordatorios_pendientes
    on alertas.recordatorios(proxima_ejecucion)
    where estado = 'ACTIVO';
create index idx_recordatorios_empresa
    on alertas.recordatorios(empresa_id, created_at desc);
