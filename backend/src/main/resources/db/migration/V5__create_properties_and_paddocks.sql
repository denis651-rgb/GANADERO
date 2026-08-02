create table core.propiedades (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), codigo varchar(60) not null,
 nombre varchar(160) not null, descripcion varchar(1000), departamento varchar(120), municipio varchar(120),
 localidad varchar(160), direccion_referencia varchar(500), superficie_ha numeric(14,4),
 ubicacion geography(Point,4326), limite_geografico geography(Polygon,4326), activo boolean not null default true,
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(),
 updated_by uuid, version bigint not null default 0,
 constraint uq_propiedad_empresa_codigo unique (empresa_id,codigo),
 constraint ck_propiedad_superficie check (superficie_ha is null or superficie_ha >= 0)
);
create table campo.sectores (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), propiedad_id uuid not null references core.propiedades(id),
 codigo varchar(60) not null, nombre varchar(160) not null, descripcion varchar(1000), activo boolean not null default true,
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(),
 updated_by uuid, version bigint not null default 0, constraint uq_sector_propiedad_codigo unique (propiedad_id,codigo)
);
create table campo.tipos_pasto (
 id uuid primary key, empresa_id uuid references core.empresas(id), codigo varchar(60) not null, nombre varchar(160) not null,
 nombre_cientifico varchar(200), descripcion varchar(1000), activo boolean not null default true
);
create unique index uq_tipo_pasto_empresa_codigo on campo.tipos_pasto
 (coalesce(empresa_id,'00000000-0000-0000-0000-000000000000'::uuid),codigo);
create table campo.potreros (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), propiedad_id uuid not null references core.propiedades(id),
 sector_id uuid references campo.sectores(id), codigo varchar(60) not null, nombre varchar(160) not null,
 superficie_ha numeric(14,4), tipo_pasto_id uuid references campo.tipos_pasto(id), capacidad_ua numeric(12,4),
 tiene_agua boolean not null default false, estado varchar(20) not null default 'DISPONIBLE',
 geometria geography(Polygon,4326), activo boolean not null default true,
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(),
 updated_by uuid, version bigint not null default 0,
 constraint uq_potrero_propiedad_codigo unique (propiedad_id,codigo),
 constraint ck_potrero_superficie check (superficie_ha is null or superficie_ha >= 0),
 constraint ck_potrero_capacidad check (capacidad_ua is null or capacidad_ua >= 0),
 constraint ck_potrero_estado check (estado in ('DISPONIBLE','OCUPADO','DESCANSO','MANTENIMIENTO'))
);
alter table seguridad.usuario_propiedades add constraint fk_usuario_propiedad
 foreign key (propiedad_id) references core.propiedades(id);
create index idx_propiedades_empresa_activo on core.propiedades(empresa_id,activo);
create index idx_sectores_propiedad_activo on campo.sectores(propiedad_id,activo);
create index idx_potreros_empresa_propiedad on campo.potreros(empresa_id,propiedad_id);
create index idx_potreros_sector on campo.potreros(sector_id);
create index idx_potreros_estado on campo.potreros(empresa_id,estado);
insert into campo.tipos_pasto(id,empresa_id,codigo,nombre,nombre_cientifico,descripcion) values
 ('10000000-0000-0000-0000-000000000001',null,'BRACHIARIA','Brachiaria','Urochloa spp.','Pasto tropical de uso ganadero.'),
 ('10000000-0000-0000-0000-000000000002',null,'MOMBASA','Mombasa','Megathyrsus maximus','Pasto de alta producción de biomasa.'),
 ('10000000-0000-0000-0000-000000000003',null,'TANZANIA','Tanzania','Megathyrsus maximus','Pasto tropical para pastoreo rotacional.'),
 ('10000000-0000-0000-0000-000000000004',null,'NATURAL','Pasto natural',null,'Pastura natural sin cultivar.')
on conflict do nothing;
