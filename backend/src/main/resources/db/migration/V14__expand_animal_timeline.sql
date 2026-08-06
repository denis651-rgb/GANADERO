alter table ganado.eventos_animal
    add column if not exists titulo varchar(200),
    add column if not exists descripcion varchar(2000),
    add column if not exists modulo_origen varchar(80),
    add column if not exists registro_origen uuid,
    add column if not exists dispositivo varchar(200),
    add column if not exists metadata jsonb not null default '{}'::jsonb,
    add column if not exists created_by uuid;

alter table ganado.eventos_animal drop constraint if exists ck_evento_animal_tipo;
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO','ACTUALIZACION',
                    'IDENTIFICADOR','GENEALOGIA','LOTE','MOVIMIENTO','CUARENTENA'));
