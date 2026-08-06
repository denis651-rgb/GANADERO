create table ganado.lotes_ganaderos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    propiedad_id uuid not null references core.propiedades(id),
    codigo varchar(60) not null,
    nombre varchar(160) not null,
    descripcion varchar(1000),
    estado varchar(20) not null default 'ABIERTO',
    fecha_apertura date not null default current_date,
    fecha_cierre date,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_lote_empresa_codigo unique (empresa_id, codigo),
    constraint ck_lote_estado check (estado in ('ABIERTO','CERRADO')),
    constraint ck_lote_fechas check (fecha_cierre is null or fecha_cierre >= fecha_apertura)
);

create table ganado.membresias_lote (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    lote_id uuid not null references ganado.lotes_ganaderos(id),
    animal_id uuid not null references ganado.animales(id),
    fecha_ingreso timestamptz not null default now(),
    fecha_salida timestamptz,
    motivo_salida varchar(1000),
    ingresado_por uuid not null,
    salida_por uuid,
    created_at timestamptz not null default now(),
    constraint ck_membresia_periodo check (fecha_salida is null or fecha_salida >= fecha_ingreso)
);

alter table ganado.animales drop constraint if exists ck_animal_lote_referencia;
create index idx_membresias_lote_activas on ganado.membresias_lote(lote_id, animal_id) where fecha_salida is null;
create index idx_membresias_animal on ganado.membresias_lote(animal_id, fecha_ingreso desc);
create index idx_lotes_empresa_estado on ganado.lotes_ganaderos(empresa_id, estado);
