-- Proyección de "ubicación operativa actual" del lote (docs: un lote activo tiene una sola
-- propiedad y un solo potrero operativo actual). No es la fuente de verdad -- el historial de
-- movimientos lo es -- solo se actualiza cuando TODOS los miembros activos elegibles se mueven
-- juntos (identidad completa del lote viaja con ellos). Si los miembros quedan en potreros
-- distintos, se deja null (ubicación operativa "mixta/desconocida") en vez de adivinar.
alter table lote_ganadero add column potrero_actual_id text references potrero(id);

-- Backfill no ambiguo: solo cuando todos los miembros activos de un lote comparten el mismo
-- potrero actual, se usa ese valor. Los lotes vacíos o con miembros repartidos en varios
-- potreros quedan en null (la migración Java de datos históricos los reporta, no los corrige).
update lote_ganadero
set potrero_actual_id = (
    select max(a.potrero_actual_id)
    from membresia_lote m join animal a on a.id = m.animal_id
    where m.lote_id = lote_ganadero.id and m.fecha_salida is null
)
where (
    select count(distinct a.potrero_actual_id)
    from membresia_lote m join animal a on a.id = m.animal_id
    where m.lote_id = lote_ganadero.id and m.fecha_salida is null
) = 1;
