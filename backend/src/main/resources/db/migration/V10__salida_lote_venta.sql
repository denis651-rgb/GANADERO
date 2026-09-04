-- Reparar únicamente el defecto conocido: venta confirmada, animal vendido,
-- membresía aún abierta y snapshot que conservó el mismo lote tras la venta.
-- Se usa la hora real de confirmación, no se inventa una hora para fecha_venta.
create temporary table salidas_venta_reparar as
select ml.id as membresia_id, ml.animal_id, ml.lote_id, md.id as detalle_id,
       mv.id as movimiento_id, mv.fecha_confirmacion, mv.usuario_confirma
from membresia_lote ml
join animal a on a.id=ml.animal_id
join movimiento_detalle md on md.animal_id=a.id and md.lote_antes=ml.lote_id and md.lote_despues=ml.lote_id
join movimiento mv on mv.id=md.movimiento_id
where ml.fecha_salida is null and a.estado='VENDIDO' and a.lote_actual_id=ml.lote_id
  and mv.tipo='SALIDA_VENTA' and mv.estado='CONFIRMADO'
  and julianday(mv.fecha_confirmacion) >= julianday(ml.fecha_ingreso)
  and mv.id=(select m2.id from movimiento m2 join movimiento_detalle d2 on d2.movimiento_id=m2.id
             where d2.animal_id=a.id and m2.tipo='SALIDA_VENTA' and m2.estado='CONFIRMADO'
             order by julianday(m2.fecha_confirmacion) desc, m2.id desc limit 1);

update membresia_lote
set fecha_salida=(select r.fecha_confirmacion from salidas_venta_reparar r where r.membresia_id=membresia_lote.id),
    salida_por=(select r.usuario_confirma from salidas_venta_reparar r where r.membresia_id=membresia_lote.id),
    motivo_salida='Movimiento SALIDA_VENTA',
    observacion=coalesce(observacion || ' | ', '') || 'Salida recuperada de venta confirmada: ' ||
        (select r.movimiento_id from salidas_venta_reparar r where r.membresia_id=membresia_lote.id),
    version=version+1
where id in (select membresia_id from salidas_venta_reparar);

update animal set lote_actual_id=null, version=version+1,
    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now')
where id in (select animal_id from salidas_venta_reparar);

-- Mantener coherente la reversión de la venta: el destino correcto es sin lote.
update movimiento_detalle set lote_despues=null
where id in (select detalle_id from salidas_venta_reparar);

drop table salidas_venta_reparar;
