-- "Mover lote": preparación/fotografía (staging) previa a la confirmación transaccional.
-- Congela los miembros activos del lote en el momento de preparar, junto con su elegibilidad
-- y restricciones sanitarias evaluadas contra el destino propuesto, para que la confirmación
-- nunca recalcule silenciosamente ni incorpore animales que ingresaron al lote después.
create table preparacion_movimiento_lote (
    id text primary key,
    lote_origen_id text not null references lote_ganadero(id),
    propiedad_origen_id text,
    potrero_origen_id text,
    modalidad text not null check (modalidad in ('LOTE_COMPLETO','SELECCION_PARCIAL')),
    destino_propiedad_id text not null,
    destino_potrero_id text not null,
    accion_lote text not null check (accion_lote in
        ('MANTENER_LOTE','CAMBIAR_A_LOTE_EXISTENTE','CREAR_NUEVO_LOTE','DEJAR_SIN_LOTE')),
    lote_destino_id text references lote_ganadero(id),
    nuevo_lote_nombre text,
    nuevo_lote_codigo text,
    nuevo_lote_descripcion text,
    fecha_efectiva text not null,
    motivo text,
    observaciones text,
    estado text not null default 'VIGENTE' check (estado in ('VIGENTE','CONFIRMADA','EXPIRADA','CANCELADA')),
    fecha_captura text not null default current_timestamp,
    fecha_expiracion text not null,
    movimiento_resultante_id text references movimiento(id),
    lote_resultante_id text references lote_ganadero(id),
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0
);

create index idx_preparacion_lote_origen on preparacion_movimiento_lote(lote_origen_id);
create index idx_preparacion_lote_estado on preparacion_movimiento_lote(estado, fecha_expiracion);

create table preparacion_movimiento_lote_miembro (
    id text primary key,
    preparacion_id text not null references preparacion_movimiento_lote(id),
    animal_id text not null references animal(id),
    animal_codigo text,
    animal_nombre text,
    animal_estado text,
    propiedad_origen_id text,
    potrero_origen_id text,
    lote_origen_id text,
    animal_version integer not null default 0,
    elegible integer not null default 1,
    motivo_exclusion text,
    seleccionado integer not null default 0,
    restricciones text,
    fecha_captura text not null default current_timestamp,
    constraint uq_preparacion_lote_miembro unique (preparacion_id, animal_id)
);

create index idx_preparacion_lote_miembro_prep on preparacion_movimiento_lote_miembro(preparacion_id);

-- Excepciones registradas al aceptar una advertencia sanitaria (no bloqueante) al confirmar.
create table preparacion_movimiento_lote_autorizacion (
    id text primary key,
    preparacion_id text not null references preparacion_movimiento_lote(id),
    animal_id text not null references animal(id),
    tipo_restriccion text not null,
    motivo text not null,
    usuario_id text,
    fecha text not null default current_timestamp
);

create index idx_preparacion_lote_autorizacion_prep on preparacion_movimiento_lote_autorizacion(preparacion_id);
