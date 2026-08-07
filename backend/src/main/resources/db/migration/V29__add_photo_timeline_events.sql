-- Etapa 9: nuevos eventos de timeline para fotos del animal
alter table ganado.eventos_animal drop constraint if exists ck_evento_animal_tipo;
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('ANIMAL_ACTUALIZADO','COMPRA_REGISTRADA','CUARENTENA_FINALIZADA',
                    'CUARENTENA_INICIADA','ESTADO_CAMBIADO','FOTO_AGREGADA',
                    'FOTO_ELIMINADA','FOTO_PRINCIPAL_CAMBIADA',
                    'GENEALOGIA_ACTUALIZADA','GENEALOGIA_REGISTRADA',
                    'IDENTIFICADOR_ACTUALIZADO','IDENTIFICADOR_ASIGNADO',
                    'IDENTIFICADOR_PRINCIPAL','IDENTIFICADOR_REEMPLAZADO',
                    'IDENTIFICADOR_RETIRADO','INGRESO_REGISTRADO','LOTE_ASIGNADO',
                    'LOTE_CAMBIADO','LOTE_REMOVIDO','MOVIMIENTO_REGISTRADO',
                    'MOVIMIENTO_REVERTIDO','NACIMIENTO_REGISTRADO','ORIGEN_SYNC',
                    'PESAJE_ANULADO','PESAJE_REGISTRADO','QR_ASIGNADO','QR_REEMPLAZADO'));
