-- Etapa 6: estandarizar la línea de tiempo del animal (Bloques 23-36)

-- 1. Columnas del modelo estándar de evento (Bloque 24)
alter table ganado.eventos_animal
    add column if not exists idempotency_key varchar(300),
    add column if not exists fecha_tecnica timestamptz,
    add column if not exists usuario_id uuid,
    add column if not exists dispositivo_id uuid;

-- 2. Retirar el check histórico ANTES de normalizar tipos (el catálogo viejo
--    rechaza los valores nuevos, por eso el backfill debe ir primero)
alter table ganado.eventos_animal drop constraint if exists ck_evento_animal_tipo;

-- 3. Backfill de datos existentes
update ganado.eventos_animal set fecha_tecnica = created_at where fecha_tecnica is null;
update ganado.eventos_animal set usuario_id = created_by where usuario_id is null and created_by is not null;

-- 4. Migrar tipos históricos al catálogo estándar (Bloque 25)
update ganado.eventos_animal set tipo = case tipo
    when 'NACIMIENTO'    then 'NACIMIENTO_REGISTRADO'
    when 'COMPRA'        then 'COMPRA_REGISTRADA'
    when 'INGRESO'       then 'INGRESO_REGISTRADO'
    when 'CAMBIO_ESTADO' then 'ESTADO_CAMBIADO'
    when 'ACTUALIZACION' then 'ANIMAL_ACTUALIZADO'
    when 'IDENTIFICADOR' then 'IDENTIFICADOR_ASIGNADO'
    when 'GENEALOGIA'    then 'GENEALOGIA_REGISTRADA'
    when 'LOTE'          then 'LOTE_ASIGNADO'
    when 'MOVIMIENTO'    then 'MOVIMIENTO_REGISTRADO'
    when 'CUARENTENA'    then 'CUARENTENA_INICIADA'
    when 'LOTE_INGRESO'  then 'LOTE_ASIGNADO'
    when 'LOTE_SALIDA'   then 'LOTE_REMOVIDO'
    when 'LOTE_CAMBIO'   then 'LOTE_CAMBIADO'
    when 'REVERSION'     then 'MOVIMIENTO_REVERTIDO'
    else tipo
end
where tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO','ACTUALIZACION',
               'IDENTIFICADOR','GENEALOGIA','LOTE','MOVIMIENTO','CUARENTENA',
               'LOTE_INGRESO','LOTE_SALIDA','LOTE_CAMBIO','REVERSION');

-- 5. Check del catálogo estándar de tipos (Bloque 25)
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('ANIMAL_ACTUALIZADO','COMPRA_REGISTRADA','CUARENTENA_FINALIZADA',
                    'CUARENTENA_INICIADA','ESTADO_CAMBIADO','FOTO_AGREGADA',
                    'GENEALOGIA_ACTUALIZADA','GENEALOGIA_REGISTRADA',
                    'IDENTIFICADOR_ACTUALIZADO','IDENTIFICADOR_ASIGNADO',
                    'IDENTIFICADOR_PRINCIPAL','IDENTIFICADOR_REEMPLAZADO',
                    'IDENTIFICADOR_RETIRADO','INGRESO_REGISTRADO','LOTE_ASIGNADO',
                    'LOTE_CAMBIADO','LOTE_REMOVIDO','MOVIMIENTO_REGISTRADO',
                    'MOVIMIENTO_REVERTIDO','NACIMIENTO_REGISTRADO','ORIGEN_SYNC',
                    'PESAJE_ANULADO','PESAJE_REGISTRADO','QR_ASIGNADO','QR_REEMPLAZADO'));

-- 6. Idempotencia (Bloque 27): clave modulo|registroOrigenId|tipo|animalId
create unique index if not exists uq_eventos_animal_idempotencia
    on ganado.eventos_animal (empresa_id, animal_id, idempotency_key)
    where idempotency_key is not null;

-- 7. Índice de consulta para la línea de tiempo paginada
create index if not exists idx_eventos_animal_consulta
    on ganado.eventos_animal (empresa_id, animal_id, fecha_evento desc);
