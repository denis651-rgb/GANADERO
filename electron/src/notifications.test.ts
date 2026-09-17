import { describe, expect, it } from 'vitest'
import { agruparPorTipoYVencimiento, type AlertaPendiente } from './notifications'

function alerta(id: string, overrides: Partial<AlertaPendiente> = {}): AlertaPendiente {
  return {
    id,
    tipo: 'MOVIMIENTO_PENDIENTE',
    titulo: 'Movimiento pendiente',
    mensaje: 'Existe un movimiento ganadero que requiere seguimiento.',
    severidad: 'WARNING',
    fechaVencimiento: null,
    ...overrides,
  }
}

describe('agruparPorTipoYVencimiento', () => {
  it('junta en un solo grupo las alertas de un movimiento con 30 animales (mismo tipo, sin vencimiento)', () => {
    const alertas = Array.from({ length: 30 }, (_, i) => alerta(`a-${i}`))
    const grupos = agruparPorTipoYVencimiento(alertas)
    expect(grupos).toHaveLength(1)
    expect(grupos[0]).toHaveLength(30)
  })

  it('junta 30 avisos de vacunación del mismo sábado aunque cada mensaje mencione un animal distinto', () => {
    // VACUNA_PROXIMA arma el mensaje con el código de cada animal (MotorAlertasService), así
    // que el texto nunca es idéntico entre animales — la fecha de vencimiento compartida es la
    // señal correcta de que es "una sola actividad para 30 animales".
    const vencimientoSabado = '2026-09-19T12:00:00Z'
    const alertas = Array.from({ length: 30 }, (_, i) => alerta(`v-${i}`, {
      tipo: 'VACUNA_PROXIMA',
      titulo: 'Vacuna próxima',
      mensaje: `ANI-0000${i} tiene una vacunación prevista para el sábado 19 de septiembre.`,
      fechaVencimiento: vencimientoSabado,
    }))
    const grupos = agruparPorTipoYVencimiento(alertas)
    expect(grupos).toHaveLength(1)
    expect(grupos[0]).toHaveLength(30)
  })

  it('no mezcla alertas del mismo tipo con vencimientos distintos', () => {
    const grupos = agruparPorTipoYVencimiento([
      alerta('a-1', { tipo: 'VACUNA_PROXIMA', fechaVencimiento: '2026-09-19T12:00:00Z' }),
      alerta('a-2', { tipo: 'VACUNA_PROXIMA', fechaVencimiento: '2026-09-26T12:00:00Z' }),
    ])
    expect(grupos).toHaveLength(2)
  })

  it('no mezcla alertas de distinto tipo aunque compartan mensaje', () => {
    const grupos = agruparPorTipoYVencimiento([
      alerta('a-1'),
      alerta('a-2', { tipo: 'VACUNA_VENCIDA', titulo: 'Vacuna vencida', mensaje: 'La vacunación de Luna está vencida.' }),
    ])
    expect(grupos).toHaveLength(2)
  })

  it('conserva una alerta suelta como su propio grupo de un solo elemento', () => {
    const grupos = agruparPorTipoYVencimiento([alerta('a-1')])
    expect(grupos).toEqual([[alerta('a-1')]])
  })

  it('devuelve una lista vacía cuando no hay alertas', () => {
    expect(agruparPorTipoYVencimiento([])).toEqual([])
  })
})
