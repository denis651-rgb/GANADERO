-- Los lotes existentes conservan NULL: sin límite configurado.
alter table lote_ganadero add column cantidad_maxima integer
    check (cantidad_maxima is null or (cantidad_maxima > 0 and typeof(cantidad_maxima) = 'integer'));

-- SQLite serializa escrituras. Estas restricciones protegen también movimientos
-- y restauraciones, incluso cuando dos solicitudes compiten por el último cupo.
create trigger lote_capacidad_ingreso before insert on membresia_lote
when new.fecha_salida is null and
    (select cantidad_maxima from lote_ganadero where id = new.lote_id) is not null and
    (select count(*) from membresia_lote where lote_id = new.lote_id and fecha_salida is null) >=
    (select cantidad_maxima from lote_ganadero where id = new.lote_id)
begin
    select raise(abort, 'LOT_CAPACITY_EXCEEDED');
end;

create trigger lote_capacidad_reingreso before update of lote_id, fecha_salida on membresia_lote
when new.fecha_salida is null and
    (select cantidad_maxima from lote_ganadero where id = new.lote_id) is not null and
    (select count(*) from membresia_lote where lote_id = new.lote_id and fecha_salida is null and id <> old.id) >=
    (select cantidad_maxima from lote_ganadero where id = new.lote_id)
begin
    select raise(abort, 'LOT_CAPACITY_EXCEEDED');
end;

create trigger lote_capacidad_reducir before update of cantidad_maxima on lote_ganadero
when new.cantidad_maxima is not null and new.cantidad_maxima <
    (select count(*) from membresia_lote where lote_id = new.id and fecha_salida is null)
begin
    select raise(abort, 'LOT_CAPACITY_EXCEEDED');
end;
