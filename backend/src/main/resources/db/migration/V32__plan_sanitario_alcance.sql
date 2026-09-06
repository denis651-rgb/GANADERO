-- Sección 18: ya no existe "un solo plan activo" a nivel global — distintas propiedades (o toda
-- la empresa, cuando propiedad_id es null) pueden tener su propio plan ACTIVO simultáneamente.
-- La detección de actividades solapadas/incompatibles entre planes activos del mismo alcance
-- pasa a ser un chequeo de aplicación (no bloqueante), no una restricción de esquema.
drop index if exists uq_plan_activo;
alter table plan_sanitario add column propiedad_id text references propiedad(id);
create index idx_plan_sanitario_propiedad on plan_sanitario(propiedad_id, estado);
