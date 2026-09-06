-- Compra formal: encabezado de toda incorporación de animales comprados (individual o por lote).
-- Antes AnimalService.create/createBatch insertaban animales sueltos con origen=COMPRADO sin
-- ningún registro comercial propio; ahora esa incorporación pertenece siempre a una Compra.
create table compra (
    id text primary key,
    codigo text not null unique,
    proveedor_id text not null references proveedor(id),
    fecha_recepcion text not null, -- timestamp ISO-8601 con hora
    modalidad_precio text not null,
    moneda text not null default 'BOB',
    cantidad_animales integer not null,
    precio_unitario numeric,
    precio_total numeric not null,
    precio_unitario_referencial numeric not null,
    propiedad_id text not null references propiedad(id),
    potrero_id text not null references potrero(id),
    lote_ganadero_id text references lote_ganadero(id),
    proposito text,
    observaciones text,
    estado text not null default 'BORRADOR',
    motivo_anulacion text,
    anulado_por text,
    fecha_anulacion text,
    origen_migracion text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_compra_modalidad check (modalidad_precio in ('POR_UNIDAD','POR_TROPA')),
    constraint ck_compra_estado check (estado in ('BORRADOR','CONFIRMADA','ANULADA')),
    constraint ck_compra_cantidad check (cantidad_animales > 0),
    constraint ck_compra_precio_total check (precio_total >= 0),
    constraint ck_compra_precio_unitario check (precio_unitario is null or precio_unitario >= 0),
    constraint ck_compra_origen_migracion check (origen_migracion is null or origen_migracion = 'MIGRADO')
);

create index idx_compra_estado on compra(estado, fecha_recepcion desc);
create index idx_compra_proveedor on compra(proveedor_id);

create table compra_detalle (
    id text primary key,
    compra_id text not null references compra(id),
    animal_id text references animal(id), -- null mientras la compra está en BORRADOR
    numero_linea integer not null,
    precio_asignado numeric not null,
    peso_ingreso_kg numeric,
    tipo_peso text,
    metodo_peso text,
    propiedad_id text not null references propiedad(id),
    potrero_id text not null references potrero(id),
    lote_ganadero_id text references lote_ganadero(id),
    codigo_solicitado text,
    nombre text,
    sexo text,
    raza_id text references raza(id),
    proposito text,
    fecha_nacimiento text,
    fecha_nacimiento_estimada integer not null default 0,
    edad_declarada_valor integer,
    edad_declarada_unidad text,
    fecha_referencia_edad text,
    fuente_edad_declarada text,
    observacion_estimacion text,
    categoria_actual_id text references categoria_animal(id),
    categoria_manual_motivo text,
    observaciones text,
    created_at text not null default current_timestamp,
    constraint ck_compra_detalle_precio check (precio_asignado >= 0),
    constraint ck_compra_detalle_peso check (peso_ingreso_kg is null or peso_ingreso_kg > 0),
    constraint ck_compra_detalle_tipo_peso check (tipo_peso is null or tipo_peso in ('MEDIDO','ESTIMADO')),
    constraint uq_compra_detalle_linea unique (compra_id, numero_linea)
);

create index idx_compra_detalle_compra on compra_detalle(compra_id);
create index idx_compra_detalle_animal on compra_detalle(animal_id);
