-- Fase 3.2: partos, crías, abortos y destetes

create table reproduccion.partos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    madre_id uuid not null references ganado.animales(id),
    servicio_id uuid references reproduccion.servicios(id),
    diagnostico_gestacion_id uuid references reproduccion.diagnosticos_gestacion(id),
    fecha_parto date not null,
    tipo_parto varchar(20) not null default 'NORMAL',
    dificultad varchar(30) not null default 'SIN_ASISTENCIA',
    asistido boolean not null default false,
    responsable_id uuid,
    resultado_madre varchar(30),
    numero_crias int not null default 1,
    observaciones varchar(1000),
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    anulado_at timestamptz,
    anulado_by uuid,
    motivo_anulacion varchar(1000),
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_parto_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_parto_tipo check (tipo_parto in ('NORMAL','PREMATURO','DISTOCICO','CESAREA','OTRO')),
    constraint ck_parto_dificultad check (dificultad in ('SIN_ASISTENCIA','ASISTENCIA_LEVE','ASISTENCIA_MODERADA','ASISTENCIA_DIFICIL','CESAREA')),
    constraint ck_parto_crias check (numero_crias >= 1),
    constraint ck_parto_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_partos_empresa_fecha on reproduccion.partos(empresa_id, fecha_parto desc, created_at desc);
create index idx_partos_animal_fecha on reproduccion.partos(empresa_id, madre_id, fecha_parto desc, created_at desc);
create index idx_partos_servicio on reproduccion.partos(servicio_id);
create unique index uq_parto_gestacion_activo on reproduccion.partos(empresa_id, diagnostico_gestacion_id)
    where diagnostico_gestacion_id is not null and estado = 'ACTIVO';

create table reproduccion.crias_parto (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    parto_id uuid not null references reproduccion.partos(id),
    animal_cria_id uuid references ganado.animales(id),
    sexo varchar(10) not null,
    peso_nacimiento_kg numeric(10,3),
    nombre varchar(160),
    estado_nacimiento varchar(20) not null,
    hora_nacimiento time,
    observaciones text,
    cliente_uuid uuid,
    idempotency_key varchar(200),
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_cria_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint uq_cria_parto_animal unique (parto_id, animal_cria_id),
    constraint ck_cria_sexo check (sexo in ('MACHO','HEMBRA')),
    constraint ck_cria_peso check (peso_nacimiento_kg is null or peso_nacimiento_kg >= 0),
    constraint ck_cria_estado check (estado_nacimiento in ('VIVO','MUERTO','NATIMUERTO'))
);

create index idx_crias_parto on reproduccion.crias_parto(empresa_id, parto_id);
create index idx_crias_animal on reproduccion.crias_parto(animal_cria_id);

create table reproduccion.abortos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    servicio_id uuid references reproduccion.servicios(id),
    gestacion_id uuid references reproduccion.diagnosticos_gestacion(id),
    fecha_evento date not null,
    edad_gestacional_estimada int,
    causa varchar(300),
    diagnostico text,
    veterinario_id uuid,
    observaciones varchar(1000),
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_aborto_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_aborto_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_abortos_empresa_fecha on reproduccion.abortos(empresa_id, fecha_evento desc, created_at desc);
create index idx_abortos_animal_fecha on reproduccion.abortos(empresa_id, animal_id, fecha_evento desc, created_at desc);
create index idx_abortos_servicio on reproduccion.abortos(servicio_id);
create index idx_abortos_diagnostico on reproduccion.abortos(gestacion_id);

create table reproduccion.destetes (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_cria_id uuid not null references ganado.animales(id),
    madre_id uuid not null references ganado.animales(id),
    fecha_destete date not null,
    peso_destete_kg numeric(10,3),
    tipo_destete varchar(20) not null,
    motivo varchar(500),
    responsable_id uuid,
    observaciones varchar(1000),
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_destete_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_destete_estado check (estado in ('ACTIVO','ANULADO')),
    constraint ck_destete_tipo check (tipo_destete in ('NORMAL','PRECOZ','TEMPORAL','FORZADO','OTRO')),
    constraint ck_destete_peso check (peso_destete_kg is null or peso_destete_kg > 0)
);

create index idx_destetes_empresa_fecha on reproduccion.destetes(empresa_id, fecha_destete desc, created_at desc);
create index idx_destetes_animal_fecha on reproduccion.destetes(empresa_id, animal_cria_id, fecha_destete desc, created_at desc);

-- Sincronización offline (función sync.registrar_cambio genérica por esquema/tabla)
create trigger trg_cambio_reproduccion_partos after insert or update or delete on reproduccion.partos
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_reproduccion_crias after insert or update or delete on reproduccion.crias_parto
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_reproduccion_abortos after insert or update or delete on reproduccion.abortos
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_reproduccion_destetes after insert or update or delete on reproduccion.destetes
    for each row execute function sync.registrar_cambio();
