create index idx_pesajes_propiedad on produccion.pesajes(empresa_id, propiedad_id, fecha desc);
create index idx_pesajes_responsable on produccion.pesajes(empresa_id, responsable_id, fecha desc);
create index idx_pesajes_estado on produccion.pesajes(empresa_id, estado, fecha desc);
