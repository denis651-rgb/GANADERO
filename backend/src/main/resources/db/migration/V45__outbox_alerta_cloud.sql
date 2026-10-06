-- Migración V45: Cola Outbox para Relay Cloud (Firebase) y Configuración de Telegram Bot
create table if not exists outbox_alerta_cloud (
    id text primary key,
    empresa_id text not null,
    tipo_modulo text not null,
    subtipo text not null,
    animal_id text,
    animal_codigo text,
    titulo text not null,
    mensaje text not null,
    fecha_programada text not null,
    prioridad text not null default 'MEDIA',
    estado_sincronizacion text not null default 'PENDIENTE_SUBIDA',
    intentos integer not null default 0,
    ultimo_error text,
    origen_tipo text,
    origen_id text,
    creado_en text not null default current_timestamp,
    sincronizado_en text,
    constraint ck_outbox_alerta_estado check (estado_sincronizacion in
        ('PENDIENTE_SUBIDA','SINCRONIZADO','PENDIENTE_CANCELACION','CANCELADO','ERROR_DEFINITIVO'))
);

create index if not exists idx_outbox_alerta_estado
    on outbox_alerta_cloud(estado_sincronizacion, creado_en);

create table if not exists configuracion_telegram (
    id text primary key,
    empresa_id text not null,
    bot_token text,
    chat_id_principal text,
    chat_ids_json text,
    activo integer not null default 1,
    notificar_sanidad integer not null default 1,
    notificar_reproduccion integer not null default 1,
    hora_notificacion text not null default '07:00',
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint uq_config_telegram_empresa unique (empresa_id),
    constraint ck_config_telegram_activo check (activo in (0, 1)),
    constraint ck_config_telegram_sanidad check (notificar_sanidad in (0, 1)),
    constraint ck_config_telegram_reproduccion check (notificar_reproduccion in (0, 1))
);
