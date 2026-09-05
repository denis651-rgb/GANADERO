-- Permite marcar actividades sanitarias que expresamente incluyen animales con edad desconocida
-- (por defecto siguen excluidos de los filtros de elegibilidad por edad, igual que hasta ahora).
alter table plan_sanitario_item add column permite_edad_desconocida integer not null default 0;
