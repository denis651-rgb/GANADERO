-- Fase 2 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, secciones 3.2 y 7):
-- historial sanitario declarado por el vendedor al comprar un animal, distinto de lo que
-- aplica el propio equipo. `aplicacion_sanitaria` necesita un CHECK constraint nuevo para
-- origen_registro y SQLite no permite agregarlo con `alter table`, asi que se reconstruye
-- la tabla (mismo patron que V3 con plan_sanitario_item). A diferencia de esa migracion,
-- aqui nada declara `references aplicacion_sanitaria(id)` (verificado en todo db/migration),
-- asi que renombrar la tabla original primero es seguro — no hay el riesgo de referencia
-- colgante que documenta V2__multi_propiedad.sql para movimiento/plan_sanitario_item.

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
    via_administracion text,
    fecha_aplicacion text not null,
    proxima_aplicacion text,
    retiro_carne_hasta text,
    retiro_leche_hasta text,
    aplicado_por text,
    resultado text,
    observaciones text,
    idempotency_key text not null unique,
    estado text not null default 'APLICADA',
    origen_registro text not null default 'APLICADA_FINCA',
    created_at text not null default current_timestamp,
    created_by text,
    version integer not null default 0,
    constraint uq_aplicacion_jornada_animal unique (jornada_id, animal_id),
    constraint ck_aplicacion_estado check (estado in ('APLICADA','ANULADA')),
    constraint ck_aplicacion_dosis check (dosis is null or dosis > 0),
    constraint ck_aplicacion_origen_registro check (origen_registro in ('APLICADA_FINCA','DECLARADA_PROVEEDOR'))
);

-- origen_registro no se selecciona: todo lo existente hasta ahora vino de jornadas
-- confirmadas por el propio equipo, asi que cae correctamente en el default APLICADA_FINCA.
insert into aplicacion_sanitaria (id, jornada_id, plan_item_id, animal_id, producto_id, lote_producto_id,
    dosis, unidad_dosis, via_administracion, fecha_aplicacion, proxima_aplicacion, retiro_carne_hasta,
    retiro_leche_hasta, aplicado_por, resultado, observaciones, idempotency_key, estado, created_at,
    created_by, version)
select id, jornada_id, plan_item_id, animal_id, producto_id, lote_producto_id,
    dosis, unidad_dosis, via_administracion, fecha_aplicacion, proxima_aplicacion, retiro_carne_hasta,
    retiro_leche_hasta, aplicado_por, resultado, observaciones, idempotency_key, estado, created_at,
    created_by, version
from aplicacion_sanitaria_old;

drop table aplicacion_sanitaria_old;
