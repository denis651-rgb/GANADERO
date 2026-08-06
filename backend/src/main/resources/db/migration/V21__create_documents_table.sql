create table archivos.documentos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    entidad_tipo varchar(60) not null,
    entidad_id uuid,
    nombre_original varchar(500),
    nombre_almacenado varchar(500) not null,
    mime_type varchar(120),
    tamano_bytes bigint,
    es_principal boolean not null default false,
    ancho_px integer,
    alto_px integer,
    created_by uuid not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_documento_empresa_path unique (empresa_id, nombre_almacenado)
);

create index idx_documentos_entidad on archivos.documentos(empresa_id, entidad_tipo, entidad_id);
