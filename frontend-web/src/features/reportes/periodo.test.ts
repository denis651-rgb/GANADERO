import { describe, expect, it } from 'vitest'
import { calcularRangoPeriodo } from './periodo'

describe('calcularRangoPeriodo', () => {
  it('calcula el rango de un trimestre', () => {
    expect(calcularRangoPeriodo('TRIMESTRE', 2026, 1)).toEqual({ desde: '2026-01-01', hasta: '2026-03-31' })
    expect(calcularRangoPeriodo('TRIMESTRE', 2026, 2)).toEqual({ desde: '2026-04-01', hasta: '2026-06-30' })
    expect(calcularRangoPeriodo('TRIMESTRE', 2026, 4)).toEqual({ desde: '2026-10-01', hasta: '2026-12-31' })
  })

  it('calcula el rango de un semestre', () => {
    expect(calcularRangoPeriodo('SEMESTRE', 2026, 1)).toEqual({ desde: '2026-01-01', hasta: '2026-06-30' })
    expect(calcularRangoPeriodo('SEMESTRE', 2026, 2)).toEqual({ desde: '2026-07-01', hasta: '2026-12-31' })
  })

  it('calcula el rango de un año completo', () => {
    expect(calcularRangoPeriodo('ANIO', 2026)).toEqual({ desde: '2026-01-01', hasta: '2026-12-31' })
  })

  it('respeta años bisiestos en el trimestre de febrero', () => {
    expect(calcularRangoPeriodo('TRIMESTRE', 2028, 1)).toEqual({ desde: '2028-01-01', hasta: '2028-03-31' })
  })
})
