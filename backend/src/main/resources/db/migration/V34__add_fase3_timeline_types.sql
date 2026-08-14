-- Fase 3: ampliar el catálogo de eventos de la línea de tiempo del animal
-- con los tipos reproductivos y sanitarios (Bloque 25 ampliado)
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
                    'PESAJE_ANULADO','PESAJE_REGISTRADO','QR_ASIGNADO','QR_REEMPLAZADO',
                    'CELO_DETECTADO','SERVICIO_REGISTRADO',
                    'DIAGNOSTICO_GESTACION_REGISTRADO','GESTACION_CONFIRMADA',
                    'GESTACION_DESCARTADA','PERDIDA_GESTACION',
                    'ABORTO_REGISTRADO','PARTO_REGISTRADO','CRIA_REGISTRADA',
                    'DESTETE_REGISTRADO',
                    'VACUNACION_APLICADA','JORNADA_SANITARIA_CONFIRMADA',
                    'CASO_CLINICO_ABIERTO','CASO_CLINICO_CERRADO',
                    'TRATAMIENTO_INICIADO','TRATAMIENTO_APLICADO','TRATAMIENTO_FINALIZADO'));
