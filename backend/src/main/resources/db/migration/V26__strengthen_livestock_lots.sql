-- Etapa 4: fortalecer lotes ganaderos (Bloques 17, 23, 31)

-- 1. Renombrar estado ABIERTO -> ACTIVO
update ganado.lotes_ganaderos set estado = 'ACTIVO' where estado = 'ABIERTO';
alter table ganado.lotes_ganaderos drop constraint if exists ck_lote_estado;
alter table ganado.lotes_ganaderos add constraint ck_lote_estado check (estado in ('ACTIVO','CERRADO'));

-- 2. Motivo de cierre del lote
alter table ganado.lotes_ganaderos add column if not exists motivo_cierre varchar(1000);

-- 3. Campos de negocio de la membresía (ingreso/retiro)
alter table ganado.membresias_lote add column if not exists motivo_ingreso varchar(1000);
alter table ganado.membresias_lote add column if not exists observacion varchar(2000);
alter table ganado.membresias_lote add column if not exists modo varchar(20) not null default 'PARCIAL';
alter table ganado.membresias_lote add column if not exists version bigint not null default 0;
alter table ganado.membresias_lote add constraint ck_membresia_modo check (modo in ('ATOMICO','PARCIAL'));

-- 4. Índice único parcial: un animal activo en un solo lote
create unique index if not exists uq_membresia_lote_activa_animal
    on ganado.membresias_lote(empresa_id, animal_id) where fecha_salida is null;

-- 5. Índice de historial de membresías por lote
create index if not exists idx_membresias_lote_historial
    on ganado.membresias_lote(lote_id, fecha_ingreso desc);

-- 6. Tipos de timeline de lotes en el animal
alter table ganado.eventos_animal drop constraint if exists ck_evento_animal_tipo;
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO','ACTUALIZACION',
                    'IDENTIFICADOR','GENEALOGIA','LOTE','MOVIMIENTO','CUARENTENA',
                    'LOTE_INGRESO','LOTE_SALIDA','LOTE_CAMBIO'));
