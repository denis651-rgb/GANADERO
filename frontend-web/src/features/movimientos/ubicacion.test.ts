import { describe, expect, it } from 'vitest'
import { recuperarOrigen, ubicacionMovimiento } from './ubicacion'
import type { Movimiento, MovimientoDetalle } from './api'

const venta: Movimiento = { id: 'venta', tipo: 'SALIDA_VENTA', estado: 'CONFIRMADO', fechaMovimiento: '2026-09-03', version: 1 }
const detalle: MovimientoDetalle = { id: 'd', animalId: 'a', animalVersionEsperada: 1, propiedadAntes: 'finca', potreroAntes: 'corral', loteAntes: 'lote' }

describe('ubicación histórica del movimiento', () => {
  it('recupera finca, potrero y lote de la venta desde el historial', () => {
    expect(recuperarOrigen(venta, [detalle])).toMatchObject({ origenPropiedadId: 'finca', origenPotreroId: 'corral', origenLoteId: 'lote' })
  })
  it('no inventa un origen común para animales con diferentes ubicaciones', () => {
    expect(recuperarOrigen(venta, [detalle, { ...detalle, potreroAntes: 'otro' }])).toEqual(venta)
    expect(recuperarOrigen(venta, [])).toEqual(venta)
  })
  it('explica las ubicaciones externas y conserva los nombres conocidos', () => {
    expect(ubicacionMovimiento(venta, 'destino', '')).toBe('Salida de la finca por venta')
    expect(ubicacionMovimiento({ ...venta, tipo: 'INGRESO_COMPRA' }, 'origen', '')).toBe('Ingreso desde compra externa')
    expect(ubicacionMovimiento(venta, 'origen', 'Finca / Corral')).toBe('Finca / Corral')
  })
})
