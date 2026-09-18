-- Las alertas de actividades del plan pasaron a generarse una sola vez por ocurrencia
-- (actividad + fecha + ubicación) y ya no se asocian a un animal (animal_id = null).
-- Las alertas viejas creadas animal por animal quedarían huérfanas y duplicarían el aviso
-- en la bandeja, así que se cancelan una vez migrado el generador.
update alerta
set estado = 'CANCELADA',
    cancelada_at = current_timestamp,
    motivo_cancelacion = 'Reemplazada por alerta grupal por ocurrencia',
    updated_at = current_timestamp
where tipo in ('ACTIVIDAD_SANITARIA_PROXIMA', 'ACTIVIDAD_SANITARIA_VENCIDA')
  and animal_id is not null
  and estado in ('PROGRAMADA', 'PENDIENTE', 'ENVIADA', 'ATENDIDA', 'ERROR');
