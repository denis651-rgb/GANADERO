import { describe, expect, it } from 'vitest'
import { calcularEdadMeses, calcularNacimientoEstimado, categoriaSugerida } from './edad'

describe('calcularNacimientoEstimado', () => {
  it('calcula dieciocho meses desde la recepción', () => {
    expect(calcularNacimientoEstimado('2026-09-04', 18, 'MESES')).toBe('2025-03-04')
  })

  it('ajusta correctamente el último día de meses cortos', () => {
    expect(calcularNacimientoEstimado('2026-03-31', 1, 'MESES')).toBe('2026-02-28')
  })

  it('calcula meses cumplidos y asigna la categoría del rango', () => {
    expect(calcularEdadMeses('2025-03-05', '2026-09-04')).toBe(17)
    expect(categoriaSugerida([
      { codigo: 'TERNERA', sexoAplicable: 'HEMBRA', clasificacionAutomatica: true, edadMinMeses: 0, edadMaxMeses: 12 },
      { codigo: 'VAQUILLA', sexoAplicable: 'HEMBRA', clasificacionAutomatica: true, edadMinMeses: 13, edadMaxMeses: 35 },
    ], 'HEMBRA', '2025-03-04', '2026-09-04')?.codigo).toBe('VAQUILLA')
  })

  it('no clasifica Buey solo por edad (clasificacionAutomatica=false)', () => {
    expect(categoriaSugerida([
      { codigo: 'BUEY', sexoAplicable: 'MACHO', clasificacionAutomatica: false, edadMinMeses: 24 },
    ], 'MACHO', '2020-01-01', '2026-09-04')).toBeUndefined()
  })

  it('ignora categorías automáticas inactivas', () => {
    expect(categoriaSugerida([
      { codigo: 'TERNERA', sexoAplicable: 'HEMBRA', clasificacionAutomatica: true, activo: false, edadMinMeses: 0, edadMaxMeses: 12 },
    ], 'HEMBRA', '2026-08-01', '2026-09-04')).toBeUndefined()
  })
})
