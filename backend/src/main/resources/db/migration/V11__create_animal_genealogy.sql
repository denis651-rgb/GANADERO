create table ganado.parentescos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    tipo_parentesco varchar(10) not null,
    animal_padre_id uuid references ganado.animales(id),
    nombre_externo varchar(160),
    raza_externa_id uuid references ganado.razas(id),
    registro_genealogico varchar(160),
    fecha_registro timestamptz not null default now(),
    registrado_por uuid not null,
    created_at timestamptz not null default now(),
    constraint uq_parentesco_animal_tipo unique (animal_id, tipo_parentesco),
    constraint ck_parentesco_tipo check (tipo_parentesco in ('MADRE','PADRE')),
    constraint ck_parentesco_origen check (
        animal_padre_id is not null or nombre_externo is not null or raza_externa_id is not null
    ),
    constraint ck_parentesco_autoreferencia check (animal_padre_id is null or animal_padre_id <> animal_id)
);

create index idx_parentescos_animal on ganado.parentescos(animal_id);
create index idx_parentescos_padre on ganado.parentescos(animal_padre_id);
create index idx_parentescos_empresa on ganado.parentescos(empresa_id);
