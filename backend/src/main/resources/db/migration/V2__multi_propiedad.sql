-- Restaura `propiedad` como catalogo real (varias parcelas por operacion) y separa los
-- ajustes globales (moneda, unidades, dias de alerta, PIN) en una tabla `configuracion`
-- propia. Migracion aditiva: preserva cualquier dato ya cargado por el usuario en V1
-- (la fila unica de "mi finca" se convierte en la primera propiedad del catalogo).

-- Este script recrea `propiedad` y `movimiento` (crear con nombre nuevo + copiar datos +
-- soltar la tabla vieja + renombrar la nueva al nombre original) para poder tocar columnas
-- atadas a un CHECK constraint, que SQLite no permite alterar directamente. El orden
-- importa: si en cambio se renombra la tabla ORIGINAL primero (ej. `movimiento` ->
-- `movimiento_old`) y luego se crea una tabla nueva con el nombre original, SQLite
-- reescribe automaticamente las clausulas `references` de OTRAS tablas que apuntaban a la
-- tabla renombrada (ej. `movimiento_detalle.movimiento_id references movimiento(id)` pasa
-- a apuntar a `movimiento_old`), dejando una referencia colgante en cuanto se hace
-- `drop table movimiento_old` (confirmado empiricamente: `pragma legacy_alter_table` NO
-- evita este reescrito en sqlite-jdbc 3.46.1.3). Crear primero con un nombre temporal y
-- soltar la tabla original recien al final evita este problema por completo, porque nunca
-- se renombra la tabla que las demas referencian por nombre.

-- ============================================================
-- 1) Nueva tabla configuracion (fila unica global)
-- ============================================================
create table configuracion (
    id text primary key,
    zona_horaria text not null default 'America/La_Paz',
    moneda text not null default 'BOB',
    unidad_peso text not null default 'KG',
    unidad_superficie text not null default 'HA',
    dias_alerta_preparto integer not null default 15,
    dias_alerta_vacunacion integer not null default 7,
    dias_sin_pesaje integer not null default 30,
    dias_alerta_destete integer not null default 7,
    dias_diagnostico_post_servicio integer not null default 30,
    dias_gestacion_estimada integer not null default 285,
    comprimir_imagenes integer not null default 1,
    calidad_imagen integer not null default 80,
    nombre_usuario text,
    pin_hash text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_configuracion_calidad_imagen check (calidad_imagen between 1 and 100)
);

-- Copia los ajustes que ya estaban en la fila unica de "propiedad" (moneda, PIN, dias de
-- alerta, etc. que el usuario ya haya configurado) hacia la nueva tabla de configuracion.
insert into configuracion (id, zona_horaria, moneda, unidad_peso, unidad_superficie,
    dias_alerta_preparto, dias_alerta_vacunacion, dias_sin_pesaje, dias_alerta_destete,
    dias_diagnostico_post_servicio, dias_gestacion_estimada, comprimir_imagenes, calidad_imagen,
    nombre_usuario, pin_hash)
select '00000000-0000-0000-0000-000000000001', zona_horaria, moneda, unidad_peso, unidad_superficie,
    dias_alerta_preparto, dias_alerta_vacunacion, dias_sin_pesaje, dias_alerta_destete,
    dias_diagnostico_post_servicio, dias_gestacion_estimada, comprimir_imagenes, calidad_imagen,
    nombre_usuario, pin_hash
from propiedad limit 1;

-- ============================================================
-- 2) propiedad vuelve a ser catalogo: se recrea la tabla completa. SQLite no permite
-- `alter table ... drop column` sobre una columna referenciada por un CHECK constraint
-- de tabla (calidad_imagen esta en ck_propiedad_calidad_imagen), asi que en vez de agregar
-- codigo/activo y luego ir soltando columnas una por una, se reconstruye la tabla entera
-- (mismo patron que se usa mas abajo para secuencia_codigo).
-- ============================================================
alter table propiedad rename to propiedad_old;

create table propiedad (
    id text primary key,
    codigo text not null default 'PRP-001',
    nombre text not null,
    descripcion text,
    departamento text,
    municipio text,
    localidad text,
    direccion_referencia text,
    superficie_ha numeric,
    ubicacion_wkt text,
    limite_geografico_wkt text,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0
);
create unique index uq_propiedad_codigo on propiedad(codigo);
create index idx_propiedades_activo on propiedad(activo);

insert into propiedad (id, codigo, nombre, descripcion, departamento, municipio, localidad,
    direccion_referencia, superficie_ha, ubicacion_wkt, limite_geografico_wkt, activo,
    created_at, updated_at, version)
select id, 'PRP-001', nombre, descripcion, departamento, municipio, localidad,
    direccion_referencia, superficie_ha, ubicacion_wkt, limite_geografico_wkt, 1,
    created_at, updated_at, version
from propiedad_old;

drop table propiedad_old;

-- ============================================================
-- 3) propiedad_id real en las tablas que ya lo esperaban en la capa de aplicacion.
-- Todo lo existente queda asociado a la propiedad original (unica hasta ahora).
-- Nota: SQLite no permite `references` junto con un default no-nulo en `add column`
-- ("Cannot add a REFERENCES column with non-NULL default value"), asi que estas columnas
-- se agregan sin la clausula `references` inline; la integridad referencial la sigue
-- validando la capa de aplicacion (igual que el resto de FKs "logicas" del baseline).
-- ============================================================
alter table sector add column propiedad_id text not null
    default '00000000-0000-0000-0000-000000000001';
create index idx_sectores_propiedad on sector(propiedad_id);

alter table potrero add column propiedad_id text not null
    default '00000000-0000-0000-0000-000000000001';
create index idx_potreros_propiedad on potrero(propiedad_id);

alter table lote_ganadero add column propiedad_id text not null
    default '00000000-0000-0000-0000-000000000001';
create index idx_lotes_propiedad on lote_ganadero(propiedad_id);

alter table animal add column propiedad_actual_id text not null
    default '00000000-0000-0000-0000-000000000001';
create index idx_animales_propiedad on animal(propiedad_actual_id);

alter table jornada_sanitaria add column propiedad_id text not null
    default '00000000-0000-0000-0000-000000000001';

-- Movimientos: se recrea la tabla completa para (a) agregar origen/destino_propiedad_id
-- (nullable: INGRESO_COMPRA no tiene origen y el historico previo a esta migracion tampoco
-- tenia forma de saber la propiedad) y (b) agregar 'TRANSFERENCIA_PROPIEDAD' al CHECK de
-- tipo -- es el movimiento que la capa de aplicacion (MovimientoService) ya usa para
-- trasladar animales entre propiedades, pero el CHECK del baseline nunca lo incluyo porque
-- hasta ahora solo existia una propiedad. SQLite no permite alterar un CHECK existente.
-- `movimiento_detalle.movimiento_id references movimiento(id)`: para no dejarla colgando
-- (ver nota al inicio del archivo) la tabla nueva se crea con nombre temporal y la
-- original recien se suelta al final, nunca se renombra la que ya existe.
create table movimiento_nuevo (
    id text primary key,
    tipo text not null,
    estado text not null default 'PENDIENTE',
    fecha_movimiento text not null,
    motivo text,
    origen_propiedad_id text,
    origen_potrero_id text references potrero(id),
    origen_lote_id text references lote_ganadero(id),
    destino_propiedad_id text,
    destino_potrero_id text references potrero(id),
    destino_lote_id text references lote_ganadero(id),
    usuario_crea text,
    usuario_confirma text,
    usuario_anula text,
    usuario_revierte text,
    fecha_confirmacion text,
    fecha_anulacion text,
    fecha_reversion text,
    motivo_anulacion text,
    motivo_reversion text,
    observacion text,
    movimiento_revertido_id text references movimiento_nuevo(id),
    movimiento_reversion_id text references movimiento_nuevo(id),
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_movimiento_tipo check (tipo in (
        'CAMBIO_POTRERO','CAMBIO_LOTE','TRANSFERENCIA_PROPIEDAD','INGRESO_COMPRA','SALIDA_VENTA','CUARENTENA','RETORNO_CUARENTENA'
    )),
    constraint ck_movimiento_estado check (estado in ('PENDIENTE','CONFIRMADO','ANULADO','REVERTIDO'))
);

insert into movimiento_nuevo (id, tipo, estado, fecha_movimiento, motivo, origen_potrero_id, origen_lote_id,
    destino_potrero_id, destino_lote_id, usuario_crea, usuario_confirma, usuario_anula, usuario_revierte,
    fecha_confirmacion, fecha_anulacion, fecha_reversion, motivo_anulacion, motivo_reversion, observacion,
    movimiento_revertido_id, movimiento_reversion_id, created_at, created_by, updated_at, updated_by, version)
select id, tipo, estado, fecha_movimiento, motivo, origen_potrero_id, origen_lote_id,
    destino_potrero_id, destino_lote_id, usuario_crea, usuario_confirma, usuario_anula, usuario_revierte,
    fecha_confirmacion, fecha_anulacion, fecha_reversion, motivo_anulacion, motivo_reversion, observacion,
    movimiento_revertido_id, movimiento_reversion_id, created_at, created_by, updated_at, updated_by, version
from movimiento;

drop table movimiento;
alter table movimiento_nuevo rename to movimiento;

create index idx_movimientos_estado on movimiento(estado, created_at desc);
create index idx_movimientos_tipo on movimiento(tipo);
create index idx_movimientos_revertido on movimiento(movimiento_revertido_id);

alter table movimiento_detalle add column propiedad_antes text;
alter table movimiento_detalle add column propiedad_despues text;

-- ============================================================
-- 4) secuencia_codigo: el CHECK de tipo_entidad no admite ALTER, se recrea la tabla
-- preservando los contadores existentes (para no reciclar codigos ya usados).
-- ============================================================
create table secuencia_codigo_new (
    tipo_entidad text not null,
    ambito_id text not null default '00000000-0000-0000-0000-000000000000',
    anio integer not null default 0,
    ultimo_numero integer not null default 0,
    updated_at text not null default current_timestamp,
    primary key (tipo_entidad, ambito_id, anio),
    constraint ck_secuencia_codigo_tipo check (tipo_entidad in ('PROPIEDAD','SECTOR','POTRERO','ANIMAL','LOTE')),
    constraint ck_secuencia_codigo_numero check (ultimo_numero >= 0)
);
insert into secuencia_codigo_new select * from secuencia_codigo;
drop table secuencia_codigo;
alter table secuencia_codigo_new rename to secuencia_codigo;

-- La propiedad existente ya "gasto" el codigo PRP-001; el contador arranca ahi para que
-- la proxima propiedad creada por el usuario reciba PRP-002 en vez de repetir PRP-001.
insert into secuencia_codigo (tipo_entidad, ambito_id, anio, ultimo_numero)
values ('PROPIEDAD', '00000000-0000-0000-0000-000000000000', 0, 1);
