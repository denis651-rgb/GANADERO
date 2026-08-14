import { describe, expect, it } from 'vitest'
import { convertirEdadADias, errorRangoEdad } from '@/features/sanidad/ageRange'

describe('conversión del rango de edad sanitario', () => {
  it('convierte días, meses y años a días', () => {
    expect(convertirEdadADias('60', 'DIAS')).toBe(60)
    expect(convertirEdadADias('4', 'MESES')).toBe(120)
    expect(convertirEdadADias('2', 'ANIOS')).toBe(730)
  })

  it('mantiene vacíos los límites no especificados', () => {
    expect(convertirEdadADias('', 'MESES')).toBeUndefined()
  })

  it('rechaza números negativos o decimales', () => {
    expect(convertirEdadADias('-1', 'DIAS')).toBeUndefined()
    expect(convertirEdadADias('1.5', 'ANIOS')).toBeUndefined()
  })

  it('explica cuando la edad máxima es menor que la mínima', () => {
    expect(errorRangoEdad(60, 4)).toBe('La edad máxima (4 días) no puede ser menor que la mínima (60 días).')
    expect(errorRangoEdad(60, 120)).toBeUndefined()
  })
})
