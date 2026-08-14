alter table alertas.suscripciones_push add column dispositivo_nombre varchar(120);
alter table alertas.suscripciones_push add column ultimo_uso_at timestamptz;
create index idx_push_usuario on alertas.suscripciones_push(empresa_id,usuario_id) where activo;

create table alertas.preferencias_notificacion(
 empresa_id uuid not null references core.empresas(id), usuario_id uuid not null,
 reproduccion boolean not null default true, sanidad boolean not null default true,
 tratamientos boolean not null default true, pesajes boolean not null default true,
 casos_criticos boolean not null default true, criticas boolean not null default true,
 urgentes boolean not null default true, recordatorios boolean not null default true,
 updated_at timestamptz not null default now(), primary key(empresa_id,usuario_id)
);
