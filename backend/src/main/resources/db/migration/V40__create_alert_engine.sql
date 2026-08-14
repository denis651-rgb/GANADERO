create table alertas.alertas (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), animal_id uuid references ganado.animales(id),
 tipo varchar(60) not null, titulo varchar(200) not null, mensaje varchar(1000) not null, severidad varchar(20) not null,
 fecha_programada timestamptz not null, fecha_vencimiento timestamptz, origen_tipo varchar(60) not null, origen_id uuid,
 estado varchar(30) not null default 'PROGRAMADA', metadata jsonb not null default '{}'::jsonb,
 enviada_at timestamptz, atendida_at timestamptz, resuelta_at timestamptz, cancelada_at timestamptz,
 atendida_por uuid, resuelta_por uuid, motivo_cancelacion varchar(500), intentos_envio integer not null default 0,
 ultimo_error text, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 constraint ck_alerta_estado check(estado in('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','RESUELTA','CANCELADA','ERROR')),
 constraint ck_alerta_severidad check(severidad in('INFO','WARNING','URGENTE','CRITICA'))
);
create unique index uq_alerta_origen_activa on alertas.alertas(empresa_id,tipo,origen_tipo,origen_id)
 where origen_id is not null and estado in('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','ERROR');
create index idx_alertas_programadas on alertas.alertas(fecha_programada) where estado='PROGRAMADA';
create index idx_alertas_empresa_estado on alertas.alertas(empresa_id,estado,fecha_programada desc);

create table alertas.suscripciones_push (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), usuario_id uuid not null,
 endpoint text not null, p256dh text not null, auth text not null, user_agent varchar(500), activo boolean not null default true,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique(usuario_id,endpoint)
);
create table alertas.entregas_notificacion (
 id uuid primary key, alerta_id uuid not null references alertas.alertas(id), suscripcion_id uuid references alertas.suscripciones_push(id),
 estado varchar(20) not null default 'PENDIENTE', intentos integer not null default 0, ultimo_error text,
 enviada_at timestamptz, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 constraint ck_entrega_estado check(estado in('PENDIENTE','ENVIADA','ERROR','DESCARTADA')),
 unique(alerta_id,suscripcion_id)
);

alter table core.configuraciones_empresa add column dias_alerta_destete integer not null default 7;
alter table core.configuraciones_empresa add constraint ck_config_alerta_destete check(dias_alerta_destete>=0);
