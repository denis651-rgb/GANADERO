import { describe, expect, it } from 'vitest'
import { calcularEdadMeses, calcularNacimientoEstimado, categoriaSugerida, formatearEdadDetallada } from './edad'

describe('formatearEdadDetallada', () => {
  const hoy = '2026-10-09'

  it('formatea en días para animales con menos de 1 mes', () => {
    expect(formatearEdadDetallada('2026-10-09', hoy)).toBe('0 días')
    expect(formatearEdadDetallada('2026-10-08', hoy)).toBe('1 día')
    expect(formatearEdadDetallada('2026-09-24', hoy)).toBe('15 días')
    expect(formatearEdadDetallada('2026-09-11', hoy)).toBe('28 días')
  })

  it('formatea en meses y días para animales entre 1 mes y menos de 1 año', () => {
    expect(formatearEdadDetallada('2026-09-09', hoy)).toBe('1 mes')
    expect(formatearEdadDetallada('2026-08-25', hoy)).toBe('1 mes y 14 días')
    expect(formatearEdadDetallada('2026-07-09', hoy)).toBe('3 meses')
    expect(formatearEdadDetallada('2026-07-05', hoy)).toBe('3 meses y 4 días')
    expect(formatearEdadDetallada('2025-11-09', hoy)).toBe('11 meses')
  })

  it('formatea en años y meses para animales de 1 año o más', () => {
    expect(formatearEdadDetallada('2025-10-09', hoy)).toBe('1 año')
    expect(formatearEdadDetallada('2025-04-09', hoy)).toBe('1 año 6 m')
    expect(formatearEdadDetallada('2023-11-09', hoy)).toBe('2 años 11 m')
  })
})

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
