-- Fase 1 de la integración con Google Calendar: hasta ahora plan_sanitario_item no distinguía la
-- "hora de ejecución" del día objetivo (POR_EDAD/PERIODICA terminaban siempre a las 00:00) ni
-- permitía más de un horario de aviso por actividad (sólo existía dias_alerta). Ambas columnas son
-- aditivas y no afectan datos existentes.
alter table plan_sanitario_item add column hora_ejecucion text not null default '08:00';
alter table plan_sanitario_item add column horarios_aviso text not null default '["08:00"]';

-- Horizonte de proyección del calendario sanitario (cuántos meses adelante se generan eventos),
-- configurable junto a las demás reglas globales de sanidad. El rango válido (1-24) se valida en
-- ConfiguracionSanitariaService, no acá, para no arriesgar el ADD COLUMN contra filas existentes.
alter table configuracion_sanitaria add column horizonte_proyeccion_meses integer not null default 12;
