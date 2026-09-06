-- Trazabilidad completa de la ejecución sanitaria (sección 22 del pedido): dosis recomendada vs.
-- aplicada, peso usado (medido/estimado, con fecha), producto aplicado vs. recomendado (con
-- motivo si difieren), lugar de aplicación, versión exacta de la actividad ejecutada,
-- instrucciones congeladas al momento de aplicar, y enlace al evento de calendario que se
-- cumplió (si vino de uno). También amplía `estado` más allá de APLICADA/ANULADA.
--
-- Igual que V4 (mismo archivo, mismo razonamiento): nada declara `references
-- aplicacion_sanitaria(id)` en todo el esquema, así que renombrar la tabla original primero es
-- seguro para esta tabla en particular (a diferencia de plan_sanitario_item/jornada_sanitaria en
-- V27, que sí tienen hijos).

alter table aplicacion_sanitaria rename to aplicacion_sanitaria_old;

create table aplicacion_sanitaria (
    id text primary key,
    jornada_id text references jornada_sanitaria(id),
    plan_item_id text references plan_sanitario_item(id),
    animal_id text not null references animal(id),
    producto_id text,
    lote_producto_id text,
    dosis numeric,
    unidad_dosis text,
    dosis_recomendada numeric,
    dosis_aplicada numeric,
    peso_utilizado_kg numeric,
    peso_tipo text,
    peso_fecha text,
    producto_aplicado_texto text,
    motivo_cambio_producto text,
    motivo_ajuste_dosis text,
    via_administracion text,
    lugar_aplicacion text,
    version_actividad_id text references plan_sanitario_item(id),
    instrucciones_aplicadas_texto text,
    evento_calendario_id text,
    fecha_aplicacion text not null,
    proxima_aplicacion text,
    retiro_carne_hasta text,
    retiro_leche_hasta text,
    aplicado_por text,
    resultado text,
    observaciones text,
    idempotency_key text not null unique,
    estado text not null default 'APLICADO',
    origen_registro text not null default 'APLICADA_FINCA',
    created_at text not null default current_timestamp,
    created_by text,
    version integer not null default 0,
    constraint uq_aplicacion_jornada_animal unique (jornada_id, animal_id),
    constraint ck_aplicacion_estado check (estado in (
        'APLICADO','NO_APLICADO','APLICADO_PARCIAL','RECHAZADO','POSPUESTO','ANULADO'
    )),
    constraint ck_aplicacion_dosis check (dosis is null or dosis > 0),
    constraint ck_aplicacion_peso_tipo check (peso_tipo is null or peso_tipo in ('MEDIDO','ESTIMADO')),
    constraint ck_aplicacion_lugar check (lugar_aplicacion is null or lugar_aplicacion in (
        'CUELLO','TABLA_DEL_CUELLO','REGION_ESCAPULAR','LOMO','LINEA_DORSAL','BOCA','FOSA_NASAL','TODO_EL_CUERPO','OTRO','NO_APLICA'
    )),
    constraint ck_aplicacion_origen_registro check (origen_registro in ('APLICADA_FINCA','DECLARADA_PROVEEDOR'))
);

-- estado: APLICADA->APLICADO, ANULADA->ANULADO (mapeo directo, sección 22); dosis_recomendada y
-- dosis_aplicada arrancan iguales a la dosis histórica única que ya existía (nunca hubo una
-- distinción hasta ahora, así que no hay forma de saber si hubo un ajuste real en el pasado).
insert into aplicacion_sanitaria (id, jornada_id, plan_item_id, animal_id, producto_id, lote_producto_id,
    dosis, unidad_dosis, dosis_recomendada, dosis_aplicada, via_administracion, fecha_aplicacion,
    proxima_aplicacion, retiro_carne_hasta, retiro_leche_hasta, aplicado_por, resultado, observaciones,
    idempotency_key, estado, origen_registro, created_at, created_by, version)
select id, jornada_id, plan_item_id, animal_id, producto_id, lote_producto_id,
    dosis, unidad_dosis, dosis, dosis, via_administracion, fecha_aplicacion,
    proxima_aplicacion, retiro_carne_hasta, retiro_leche_hasta, aplicado_por, resultado, observaciones,
    idempotency_key,
    case estado when 'APLICADA' then 'APLICADO' when 'ANULADA' then 'ANULADO' else estado end,
    origen_registro, created_at, created_by, version
from aplicacion_sanitaria_old;

drop table aplicacion_sanitaria_old;

create index idx_aplicaciones_animal_fecha on aplicacion_sanitaria(animal_id, fecha_aplicacion desc);
create index idx_aplicaciones_evento on aplicacion_sanitaria(evento_calendario_id);
