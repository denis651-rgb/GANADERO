alter table seguridad.perfiles_usuario
    add column if not exists email varchar(180);

create unique index if not exists uq_perfiles_usuario_email_lower
    on seguridad.perfiles_usuario (lower(email)) where email is not null;

create table if not exists auditoria.registros (
    id uuid primary key,
    empresa_id uuid references core.empresas(id),
    usuario_id uuid,
    accion varchar(80) not null,
    modulo varchar(80) not null,
    entidad varchar(120) not null,
    entidad_id uuid,
    correlation_id varchar(100),
    resultado varchar(30) not null,
    datos jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_auditoria_empresa_fecha
    on auditoria.registros (empresa_id, created_at desc);

create table if not exists seguridad.bootstrap_ejecuciones (
    id uuid primary key,
    idempotency_key varchar(200) not null unique,
    payload_hash varchar(64) not null,
    estado varchar(20) not null,
    empresa_id uuid references core.empresas(id),
    usuario_id uuid,
    miembro_id uuid,
    propiedad_id uuid references core.propiedades(id),
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    constraint ck_bootstrap_estado check (estado in ('PROCESANDO','COMPLETADO','FALLIDO'))
);

insert into seguridad.permisos (id,codigo,nombre,descripcion,modulo,activo)
values ('00000000-0000-0000-0001-000000000037','SISTEMA_ESTADO_VER','Ver estado del sistema',
        'Permite consultar el estado técnico mínimo de la aplicación.','SISTEMA',true)
on conflict (codigo) do update set nombre=excluded.nombre, descripcion=excluded.descripcion,
    modulo=excluded.modulo, activo=true;

insert into seguridad.rol_permisos (rol_id,permiso_id)
select r.id,p.id from seguridad.roles r join seguridad.permisos p on p.codigo='SISTEMA_ESTADO_VER'
where r.codigo in ('PROPIETARIO','ADMINISTRADOR') and r.empresa_id is null
on conflict do nothing;

create index if not exists idx_miembros_empresa_estado on seguridad.miembros_empresa(empresa_id,estado);
create index if not exists idx_usuario_propiedades_propiedad on seguridad.usuario_propiedades(propiedad_id);
