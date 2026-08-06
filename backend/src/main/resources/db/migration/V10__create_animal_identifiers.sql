create table ganado.identificadores_animal (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    tipo varchar(20) not null,
    valor varchar(120) not null,
    principal boolean not null default false,
    estado varchar(20) not null default 'ACTIVO',
    fecha_asignacion timestamptz not null default now(),
    fecha_retiro timestamptz,
    motivo_retiro varchar(1000),
    asignado_por uuid not null,
    retirado_por uuid,
    observaciones varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_identificador_empresa_tipo_valor unique (empresa_id, tipo, valor),
    constraint ck_identificador_tipo check (tipo in ('ARETE','QR','RFID','TATUAJE','OTRO')),
    constraint ck_identificador_estado check (estado in ('ACTIVO','RETIRADO'))
);

create index idx_identificadores_animal on ganado.identificadores_animal(animal_id, estado);
create index idx_identificadores_empresa on ganado.identificadores_animal(empresa_id, tipo, estado);
