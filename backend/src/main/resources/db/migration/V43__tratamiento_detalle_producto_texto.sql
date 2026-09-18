-- Trazabilidad del producto aplicado en un protocolo de tratamiento sin depender de un
-- inventario de medicamentos (que no existe en el sistema). producto_id/lote_producto_id
-- siguen reservados para el futuro módulo de inventario.
alter table tratamiento_detalle add column producto_texto text;
