create table seguridad.perfiles_usuario (
    id uuid primary key,
    nombres varchar(120) not null,
    apellidos varchar(120) not null,
    telefono varchar(40),
    avatar_path varchar(500),
    activo boolean not null default true,
    ultimo_acceso_at timestamptz,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0
);

create table seguridad.miembros_empresa (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    usuario_id uuid not null references seguridad.perfiles_usuario(id),
    cargo varchar(120),
    estado varchar(20) not null default 'INVITADO',
    fecha_ingreso date,
    acceso_todas_propiedades boolean not null default false,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_miembro_empresa_usuario unique (empresa_id, usuario_id),
    constraint ck_miembro_estado check (estado in ('INVITADO', 'ACTIVO', 'BLOQUEADO'))
);

create table seguridad.roles (
    id uuid primary key,
    empresa_id uuid references core.empresas(id),
    codigo varchar(80) not null,
    nombre varchar(120) not null,
    descripcion varchar(500),
    es_sistema boolean not null default false,
    activo boolean not null default true,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0
);

create unique index uq_roles_empresa_codigo
    on seguridad.roles (coalesce(empresa_id, '00000000-0000-0000-0000-000000000000'::uuid), codigo);

create table seguridad.permisos (
    id uuid primary key,
    codigo varchar(100) not null unique,
    nombre varchar(150) not null,
    descripcion varchar(500),
    activo boolean not null default true,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0
);

create table seguridad.rol_permisos (
    rol_id uuid not null references seguridad.roles(id),
    permiso_id uuid not null references seguridad.permisos(id),
    primary key (rol_id, permiso_id)
);

create table seguridad.usuario_roles (
    miembro_empresa_id uuid not null references seguridad.miembros_empresa(id),
    rol_id uuid not null references seguridad.roles(id),
    primary key (miembro_empresa_id, rol_id)
);

create table seguridad.usuario_propiedades (
    miembro_empresa_id uuid not null references seguridad.miembros_empresa(id),
    propiedad_id uuid not null,
    primary key (miembro_empresa_id, propiedad_id)
);

create index idx_miembros_usuario_estado
    on seguridad.miembros_empresa (usuario_id, estado);
create index idx_usuario_roles_miembro
    on seguridad.usuario_roles (miembro_empresa_id);
create index idx_usuario_propiedades_miembro
    on seguridad.usuario_propiedades (miembro_empresa_id);

