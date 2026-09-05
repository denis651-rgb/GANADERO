-- Pesos como eventos históricos: se agrega tipo_peso (MEDIDO/ESTIMADO, ausente hasta ahora) y
-- enlaces opcionales a compra/venta/movimiento para que el último peso del animal se derive
-- siempre del historial de pesaje, nunca de un campo paralelo. Se amplían los motivos (columna
-- `tipo`, ya usada como motivo) sumando COMPRA, SALIDA_LOTE y OTRO SIN renombrar los existentes
-- (RUTINA/NACIMIENTO/DESTETE/ENTRADA/VENTA/PESADA_ESPECIAL) para no romper datos ni tests.
-- Recreación completa de tabla: SQLite no permite ALTER de un CHECK existente. Ningún otro
-- objeto referencia pesaje(id) por FK, así que no hay el problema de orden documentado en V2/V3.
create table pesaje_nuevo (
    id text primary key,
    animal_id text not null references animal(id),
    fecha text not null default current_date,
    peso_kg numeric not null,
    tipo text not null default 'RUTINA',
    tipo_peso text,
    condicion_corporal numeric,
    bascula text,
    responsable_id text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    dispositivo text,
    compra_id text references compra(id),
    venta_id text references venta(id),
    movimiento_id text references movimiento(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    motivo_anulacion text,
    anulado_por text,
    fecha_anulacion text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_pesaje_peso check (peso_kg > 0),
    constraint ck_pesaje_condicion check (condicion_corporal is null or condicion_corporal between 1 and 5),
    constraint ck_pesaje_tipo check (tipo in ('RUTINA','NACIMIENTO','DESTETE','ENTRADA','VENTA','PESADA_ESPECIAL','COMPRA','SALIDA_LOTE','OTRO')),
    constraint ck_pesaje_tipo_peso check (tipo_peso is null or tipo_peso in ('MEDIDO','ESTIMADO')),
    constraint ck_pesaje_estado check (estado in ('ACTIVO','ANULADO'))
);

insert into pesaje_nuevo (id, animal_id, fecha, peso_kg, tipo, condicion_corporal, bascula, responsable_id,
    potrero_id, lote_id, dispositivo, cliente_uuid, idempotency_key, estado, motivo_anulacion, anulado_por,
    fecha_anulacion, observaciones, created_at, created_by, updated_at, updated_by, version)
select id, animal_id, fecha, peso_kg, tipo, condicion_corporal, bascula, responsable_id,
    potrero_id, lote_id, dispositivo, cliente_uuid, idempotency_key, estado, motivo_anulacion, anulado_por,
    fecha_anulacion, observaciones, created_at, created_by, updated_at, updated_by, version
from pesaje;

drop table pesaje;
alter table pesaje_nuevo rename to pesaje;

create index idx_pesajes_animal_fecha on pesaje(animal_id, fecha desc, created_at desc);
create index idx_pesajes_fecha on pesaje(fecha desc);
create index idx_pesajes_lote on pesaje(lote_id, fecha desc);
create index idx_pesajes_potrero on pesaje(potrero_id, fecha desc);
create index idx_pesajes_estado on pesaje(estado, fecha desc);
create index idx_pesajes_compra on pesaje(compra_id);
create index idx_pesajes_venta on pesaje(venta_id);
