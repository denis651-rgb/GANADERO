-- Fase 3.1: núcleo de reproducción (celos, servicios y diagnósticos de gestación)

create schema if not exists reproduccion;

create table reproduccion.celos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    fecha_deteccion timestamptz not null,
    tipo_deteccion varchar(30) not null,
    intensidad varchar(20),
    detectado_por uuid,
    observaciones text,
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    anulado_at timestamptz,
    anulado_by uuid,
    motivo_anulacion text,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_celo_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_celo_tipo check (tipo_deteccion in ('VISUAL','TORO_MARCADOR','PODOMETRO','SENSOR','OTRO')),
    constraint ck_celo_intensidad check (intensidad is null or intensidad in ('BAJA','MEDIA','ALTA')),
    constraint ck_celo_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_celos_empresa_fecha on reproduccion.celos(empresa_id, fecha_deteccion desc, created_at desc);
create index idx_celos_animal_fecha on reproduccion.celos(empresa_id, animal_id, fecha_deteccion desc, created_at desc);

create table reproduccion.servicios (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    hembra_id uuid not null references ganado.animales(id),
    celo_id uuid references reproduccion.celos(id),
    fecha_servicio timestamptz not null,
    tipo_servicio varchar(30) not null,
    macho_id uuid references ganado.animales(id),
    codigo_semen varchar(100),
    proveedor_semen varchar(160),
    tecnico_id uuid,
    numero_intento int not null default 1,
    fecha_diagnostico_recomendada timestamptz not null,
    observaciones text,
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(30) not null default 'PENDIENTE_DIAGNOSTICO',
    anulado_at timestamptz,
    anulado_by uuid,
    motivo_anulacion text,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_servicio_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_servicio_tipo check (tipo_servicio in ('MONTA_NATURAL','INSEMINACION_ARTIFICIAL','TRANSFERENCIA_EMBRIONARIA')),
    constraint ck_servicio_intento check (numero_intento > 0),
    constraint ck_servicio_estado check (estado in ('REGISTRADO','PENDIENTE_DIAGNOSTICO','GESTACION_CONFIRMADA','NO_PRENADA','FINALIZADO','ANULADO'))
);

create index idx_servicios_empresa_fecha on reproduccion.servicios(empresa_id, fecha_servicio desc, created_at desc);
create index idx_servicios_animal_fecha on reproduccion.servicios(empresa_id, hembra_id, fecha_servicio desc, created_at desc);
create index idx_servicios_celo on reproduccion.servicios(celo_id);

create table reproduccion.diagnosticos_gestacion (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    servicio_id uuid references reproduccion.servicios(id),
    fecha_diagnostico timestamptz not null,
    resultado varchar(20) not null,
    metodo varchar(20),
    dias_gestacion_estimados int,
    fecha_probable_parto date,
    veterinario_id uuid,
    observaciones text,
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    motivo_anulacion varchar(1000),
    anulado_por uuid,
    fecha_anulacion timestamptz,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_diagnostico_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_diagnostico_resultado check (resultado in ('POSITIVO','NEGATIVO','DUDOSO','PERDIDA_GESTACION')),
    constraint ck_diagnostico_metodo check (metodo is null or metodo in ('PALPACION','ECOGRAFIA','SANGRE','OTRO')),
    constraint ck_diagnostico_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_diagnosticos_empresa_fecha on reproduccion.diagnosticos_gestacion(empresa_id, fecha_diagnostico desc, created_at desc);
create index idx_diagnosticos_animal_fecha on reproduccion.diagnosticos_gestacion(empresa_id, animal_id, fecha_diagnostico desc, created_at desc);
create index idx_diagnosticos_servicio on reproduccion.diagnosticos_gestacion(servicio_id);

-- Sincronización offline (la función sync.registrar_cambio es genérica por esquema/tabla)
create trigger trg_cambio_reproduccion_celos after insert or update or delete on reproduccion.celos
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_reproduccion_servicios after insert or update or delete on reproduccion.servicios
    for each row execute function sync.registrar_cambio();
create trigger trg_cambio_reproduccion_diagnosticos after insert or update or delete on reproduccion.diagnosticos_gestacion
    for each row execute function sync.registrar_cambio();
