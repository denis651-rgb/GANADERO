create table ganado.razas (
 id uuid primary key, empresa_id uuid references core.empresas(id), codigo varchar(60) not null,
 nombre varchar(160) not null, especie varchar(80) not null default 'BOVINO', descripcion varchar(1000),
 activo boolean not null default true
);
create unique index uq_raza_empresa_codigo on ganado.razas
 (coalesce(empresa_id,'00000000-0000-0000-0000-000000000000'::uuid),codigo);

create table ganado.categorias_animal (
 id uuid primary key, empresa_id uuid references core.empresas(id), codigo varchar(60) not null,
 nombre varchar(160) not null, sexo_aplicable varchar(10) not null, edad_min_meses integer,
 edad_max_meses integer, descripcion varchar(1000), activo boolean not null default true,
 constraint ck_categoria_sexo check (sexo_aplicable in ('MACHO','HEMBRA','AMBOS')),
 constraint ck_categoria_edades check (edad_min_meses is null or edad_min_meses >= 0)
);
create unique index uq_categoria_empresa_codigo on ganado.categorias_animal
 (coalesce(empresa_id,'00000000-0000-0000-0000-000000000000'::uuid),codigo);

create table ganado.animales (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), codigo varchar(60) not null,
 nombre varchar(160), sexo varchar(10) not null, fecha_nacimiento date, fecha_nacimiento_estimada boolean not null default false,
 raza_principal_id uuid not null references ganado.razas(id), categoria_actual_id uuid not null references ganado.categorias_animal(id),
 color varchar(100), proposito varchar(30) not null, origen varchar(20) not null,
 propiedad_actual_id uuid not null references core.propiedades(id), potrero_actual_id uuid not null references campo.potreros(id),
 lote_actual_id uuid, estado varchar(20) not null default 'ACTIVO', fecha_ingreso date not null,
 precio_adquisicion numeric(16,2), peso_nacimiento_kg numeric(10,3), condicion_corporal_actual numeric(4,2),
 foto_principal_path varchar(500), observaciones varchar(2000),
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(),
 updated_by uuid, version bigint not null default 0,
 constraint uq_animal_empresa_codigo unique (empresa_id,codigo),
 constraint ck_animal_sexo check (sexo in ('MACHO','HEMBRA')),
 constraint ck_animal_proposito check (proposito in ('CARNE','LECHE','REPRODUCCION','DOBLE_PROPOSITO')),
 constraint ck_animal_origen check (origen in ('NACIDO','COMPRADO','TRANSFERIDO')),
 constraint ck_animal_estado check (estado in ('ACTIVO','VENDIDO','MUERTO','PERDIDO','TRANSFERIDO','DESCARTADO')),
 constraint ck_animal_precio check (precio_adquisicion is null or precio_adquisicion >= 0),
 constraint ck_animal_peso check (peso_nacimiento_kg is null or peso_nacimiento_kg >= 0),
 constraint ck_animal_condicion check (condicion_corporal_actual is null or condicion_corporal_actual between 1 and 5)
);

create table ganado.eventos_animal (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), animal_id uuid not null references ganado.animales(id),
 tipo varchar(30) not null, fecha_evento timestamptz not null default now(), estado_anterior varchar(20),
 estado_nuevo varchar(20), motivo varchar(1000), registrado_por uuid not null,
 created_at timestamptz not null default now(),
 constraint ck_evento_animal_tipo check (tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO'))
);

create index idx_animales_empresa_estado on ganado.animales(empresa_id,estado);
create index idx_animales_ubicacion on ganado.animales(empresa_id,propiedad_actual_id,potrero_actual_id);
create index idx_animales_categoria on ganado.animales(empresa_id,categoria_actual_id);
create index idx_animales_lote on ganado.animales(empresa_id,lote_actual_id);
create index idx_eventos_animal_fecha on ganado.eventos_animal(animal_id,fecha_evento desc);

insert into ganado.razas(id,empresa_id,codigo,nombre,especie) values
 ('50000000-0000-0000-0000-000000000001',null,'BRAHMAN','Brahman','BOVINO'),
 ('50000000-0000-0000-0000-000000000002',null,'NELORE','Nelore','BOVINO'),
 ('50000000-0000-0000-0000-000000000003',null,'HOLSTEIN','Holstein','BOVINO'),
 ('50000000-0000-0000-0000-000000000004',null,'PARDO_SUIZO','Pardo Suizo','BOVINO'),
 ('50000000-0000-0000-0000-000000000005',null,'MESTIZO','Mestizo','BOVINO') on conflict do nothing;

insert into ganado.categorias_animal(id,empresa_id,codigo,nombre,sexo_aplicable,edad_min_meses,edad_max_meses) values
 ('60000000-0000-0000-0000-000000000001',null,'TERNERO','Ternero','MACHO',0,12),
 ('60000000-0000-0000-0000-000000000002',null,'TERNERA','Ternera','HEMBRA',0,12),
 ('60000000-0000-0000-0000-000000000003',null,'VAQUILLA','Vaquilla','HEMBRA',13,35),
 ('60000000-0000-0000-0000-000000000004',null,'NOVILLO','Novillo','MACHO',13,35),
 ('60000000-0000-0000-0000-000000000005',null,'VACA','Vaca','HEMBRA',36,null),
 ('60000000-0000-0000-0000-000000000006',null,'TORO','Toro','MACHO',24,null),
 ('60000000-0000-0000-0000-000000000007',null,'BUEY','Buey','MACHO',24,null) on conflict do nothing;
