-- Hora del día (America/La_Paz) a la que salen los avisos que nacen de una fecha sin hora propia:
-- parto probable, destete, retiro de carne o leche y próxima vacunación declarada. Antes se
-- programaban al inicio del día (medianoche, o las 8 p. m. del día anterior en los de destete),
-- una hora a la que nadie los ve.
alter table configuracion add column hora_avisos text not null default '08:00';

-- Los avisos de esos tipos que todavía no llegaron a su fecha y quedaron a medianoche pasan a las
-- 08:00 (12:00 UTC) de su mismo día. 04:00Z es la medianoche de La Paz; 00:00Z es la medianoche UTC
-- que usaba el destete. Los que ya se emitieron o se resolvieron no se tocan.
update alerta
set fecha_programada = substr(fecha_programada, 1, 10) || 'T12:00:00Z',
    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
where estado = 'PROGRAMADA'
  and tipo in ('PARTO_PROXIMO', 'DESTETE_PROXIMO', 'VACUNA_PROXIMA', 'RETIRO_CARNE_VIGENTE', 'RETIRO_LECHE_VIGENTE')
  and (fecha_programada like '%T04:00:00Z' or fecha_programada like '%T00:00:00Z');
