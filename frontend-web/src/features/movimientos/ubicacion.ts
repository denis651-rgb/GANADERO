import type { Movimiento, MovimientoDetalle } from './api'

// Use the recorded location at the time of the movement, never the animal's current location.
export function recuperarOrigen(movimiento: Movimiento, detalles: MovimientoDetalle[]): Movimiento {
  if (movimiento.origenPropiedadId || movimiento.origenPotreroId || movimiento.origenLoteId || movimiento.tipo === 'INGRESO_COMPRA') return movimiento
  const ubicaciones = new Set(detalles.map((d) => JSON.stringify([d.propiedadAntes ?? null, d.potreroAntes ?? null, d.loteAntes ?? null])))
  if (ubicaciones.size !== 1 || !detalles.length) return movimiento
  const primero = detalles[0]
  return { ...movimiento, origenPropiedadId: primero.propiedadAntes, origenPotreroId: primero.potreroAntes, origenLoteId: primero.loteAntes }
}

export function ubicacionMovimiento(movimiento: Movimiento, lado: 'origen' | 'destino', nombre: string) {
  if (lado === 'destino' && movimiento.tipo === 'SALIDA_VENTA') return 'Salida de la finca por venta'
  if (nombre) return nombre
  if (lado === 'origen' && movimiento.tipo === 'INGRESO_COMPRA') return 'Ingreso desde compra externa'
  return lado === 'origen' ? 'Sin origen único registrado' : 'Destino no registrado'
}
