-- La línea de tiempo del animal admite eventos de los controles sanitarios
-- individuales. `TipoEventoAnimal` ya declara CONTROL_NEONATAL_REGISTRADO,
-- CONTROL_ECTOPARASITARIO_REGISTRADO y EXAMEN_REPRODUCTIVO_REGISTRADO, pero el
-- CHECK `ck_evento_animal_tipo` del baseline V1 no los incluía y ninguna
-- migración posterior lo actualizó. Al guardar un control neonatal (o un
-- control ectoparasitario de un animal, o un examen reproductivo), la inserción
-- del evento fallaba con SQLITE_CONSTRAINT_CHECK y abortaba toda la operación.
--
-- Se reconstruye la tabla con el CHECK ampliado. Nada declara
-- `references evento_animal(id)` en el esquema, así que renombrar y recrear es
-- seguro (mismo criterio que V29 para aplicacion_sanitaria).

alter table evento_animal rename to evento_animal_old;

create table evento_animal (
    id text primary key,
    animal_id text not null references animal(id),
    tipo text not null,
    titulo text,
    descripcion text,
    modulo_origen text,
    registro_origen text,
    dispositivo text,
    metadata text not null default '{}',
    fecha_evento text not null default current_timestamp,
    fecha_tecnica text,
    estado_anterior text,
    estado_nuevo text,
    motivo text,
    usuario_id text,
    dispositivo_id text,
    idempotency_key text,
    registrado_por text,
    created_by text,
    created_at text not null default current_timestamp,
    constraint ck_evento_animal_tipo check (tipo in (
        'ANIMAL_ACTUALIZADO','COMPRA_REGISTRADA','CUARENTENA_FINALIZADA','CUARENTENA_INICIADA',
        'ESTADO_CAMBIADO','FOTO_AGREGADA','FOTO_ELIMINADA','FOTO_PRINCIPAL_CAMBIADA',
        'GENEALOGIA_ACTUALIZADA','GENEALOGIA_REGISTRADA','IDENTIFICADOR_ACTUALIZADO',
        'IDENTIFICADOR_ASIGNADO','IDENTIFICADOR_PRINCIPAL','IDENTIFICADOR_REEMPLAZADO',
        'IDENTIFICADOR_RETIRADO','INGRESO_REGISTRADO','LOTE_ASIGNADO','LOTE_CAMBIADO',
        'LOTE_REMOVIDO','MOVIMIENTO_REGISTRADO','MOVIMIENTO_REVERTIDO','NACIMIENTO_REGISTRADO',
        'ORIGEN_SYNC','PESAJE_ANULADO','PESAJE_REGISTRADO','QR_ASIGNADO','QR_REEMPLAZADO',
        'CELO_DETECTADO','SERVICIO_REGISTRADO','DIAGNOSTICO_GESTACION_REGISTRADO',
        'GESTACION_CONFIRMADA','GESTACION_DESCARTADA','PERDIDA_GESTACION','ABORTO_REGISTRADO',
        'PARTO_REGISTRADO','CRIA_REGISTRADA','DESTETE_REGISTRADO','VACUNACION_APLICADA',
        'JORNADA_SANITARIA_CONFIRMADA','CASO_CLINICO_ABIERTO','CASO_CLINICO_CERRADO',
        'TRATAMIENTO_INICIADO','TRATAMIENTO_APLICADO','TRATAMIENTO_FINALIZADO','VENTA_REGISTRADA',
        'CONTROL_NEONATAL_REGISTRADO','CONTROL_ECTOPARASITARIO_REGISTRADO','EXAMEN_REPRODUCTIVO_REGISTRADO'
    ))
);

insert into evento_animal (id, animal_id, tipo, titulo, descripcion, modulo_origen, registro_origen,
    dispositivo, metadata, fecha_evento, fecha_tecnica, estado_anterior, estado_nuevo, motivo,
    usuario_id, dispositivo_id, idempotency_key, registrado_por, created_by, created_at)
select id, animal_id, tipo, titulo, descripcion, modulo_origen, registro_origen,
    dispositivo, metadata, fecha_evento, fecha_tecnica, estado_anterior, estado_nuevo, motivo,
    usuario_id, dispositivo_id, idempotency_key, registrado_por, created_by, created_at
from evento_animal_old;

drop table evento_animal_old;

create unique index uq_eventos_animal_idempotencia on evento_animal(animal_id, idempotency_key) where idempotency_key is not null;
create index idx_eventos_animal_consulta on evento_animal(animal_id, fecha_evento desc);
