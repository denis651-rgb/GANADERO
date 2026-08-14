-- El repositorio de movimientos registra al actor tanto en las columnas
-- funcionales (usuario_*) como en la auditoria tecnica comun del sistema.
alter table ganado.movimientos
    add column if not exists created_by uuid,
    add column if not exists updated_by uuid;

-- Los movimientos existentes conservan como actor inicial al usuario que los creo.
update ganado.movimientos
set created_by = coalesce(created_by, usuario_crea),
    updated_by = coalesce(updated_by, usuario_crea)
where created_by is null
   or updated_by is null;
