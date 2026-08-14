-- Un traslado de propiedad sin lote de destino debe retirar al animal del lote anterior.
-- Esta migracion corrige los registros creados antes de que esa regla se aplicara en el servicio.
update ganado.membresias_lote membresia
set fecha_salida = greatest(now(), membresia.fecha_ingreso),
    motivo_salida = coalesce(membresia.motivo_salida, 'REPARACION_CAMBIO_PROPIEDAD'),
    version = membresia.version + 1
from ganado.animales animal,
     ganado.lotes_ganaderos lote
where membresia.animal_id = animal.id
  and lote.id = membresia.lote_id
  and membresia.fecha_salida is null
  and (lote.empresa_id <> animal.empresa_id
       or lote.propiedad_id <> animal.propiedad_actual_id);

update ganado.animales animal
set lote_actual_id = null,
    updated_at = now(),
    version = animal.version + 1
from ganado.lotes_ganaderos lote
where lote.id = animal.lote_actual_id
  and (lote.empresa_id <> animal.empresa_id
       or lote.propiedad_id <> animal.propiedad_actual_id);
