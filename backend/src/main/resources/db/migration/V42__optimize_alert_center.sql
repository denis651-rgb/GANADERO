create index idx_alertas_pendientes_programadas on alertas.alertas(fecha_programada) where estado in('PROGRAMADA','PENDIENTE');
create index idx_alertas_animal on alertas.alertas(empresa_id,animal_id,created_at desc);
