create table ganado.movimientos (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    tipo varchar(40) not null,
    estado varchar(20) not null default 'PENDIENTE',
    fecha_movimiento date not null,
    motivo varchar(1000),
    origen_propiedad_id uuid references core.propiedades(id),
    origen_potrero_id uuid references campo.potreros(id),
    origen_lote_id uuid references ganado.lotes_ganaderos(id),
    destino_propiedad_id uuid references core.propiedades(id),
    destino_potrero_id uuid references campo.potreros(id),
    destino_lote_id uuid references ganado.lotes_ganaderos(id),
    usuario_crea uuid not null,
    usuario_confirma uuid,
    usuario_anula uuid,
    fecha_confirmacion timestamptz,
    fecha_anulacion timestamptz,
    motivo_anulacion varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint ck_movimiento_tipo check (tipo in (
        'CAMBIO_POTRERO','CAMBIO_LOTE','TRANSFERENCIA_PROPIEDAD','INGRESO_COMPRA','SALIDA_VENTA','CUARENTENA','RETORNO_CUARENTENA'
    )),
    constraint ck_movimiento_estado check (estado in ('PENDIENTE','CONFIRMADO','ANULADO'))
);

create table ganado.movimiento_detalles (
    id uuid primary key,
    movimiento_id uuid not null references ganado.movimientos(id),
    animal_id uuid not null references ganado.animales(id),
    empresa_id uuid not null references core.empresas(id),
    estado_antes varchar(20),
    estado_despues varchar(20),
    constraint uq_movimiento_detalle_animal unique (movimiento_id, animal_id)
);

create index idx_movimientos_empresa_estado on ganado.movimientos(empresa_id, estado, created_at desc);
create index idx_movimientos_tipo on ganado.movimientos(empresa_id, tipo);
create index idx_movimiento_detalles_animal on ganado.movimiento_detalles(animal_id);
