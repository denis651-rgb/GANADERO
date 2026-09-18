import { describe, expect, it } from 'vitest'
import { convertirEdadADias, errorEdadObjetivoFueraDeRango, errorRangoEdad, rangoEdadDesdeDias } from '@/features/sanidad/ageRange'
import { pesoReferenciaFijo } from '@/features/sanidad/dosisReferencia'

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

describe('rango de edad guardado en días al editar una actividad', () => {
  it('usa la unidad preferida cuando ambos límites dividen exacto', () => {
    expect(rangoEdadDesdeDias(90, 210, 'MESES')).toEqual({ unidad: 'MESES', minimo: '3', maximo: '7' })
    expect(rangoEdadDesdeDias(60, undefined, 'DIAS')).toEqual({ unidad: 'DIAS', minimo: '60', maximo: '' })
  })

  it('no convierte 200 días en 200 meses cuando la unidad guardada no divide exacto', () => {
    expect(rangoEdadDesdeDias(200, undefined, 'MESES')).toEqual({ unidad: 'DIAS', minimo: '200', maximo: '' })
  })

  it('usa una sola unidad para ambos límites, cayendo a días si alguno no divide exacto', () => {
    expect(rangoEdadDesdeDias(90, 200, 'MESES')).toEqual({ unidad: 'DIAS', minimo: '90', maximo: '200' })
  })

  it('elige la mayor unidad exacta cuando no hay preferida', () => {
    expect(rangoEdadDesdeDias(730, undefined)).toEqual({ unidad: 'ANIOS', minimo: '2', maximo: '' })
    expect(rangoEdadDesdeDias(120, 240)).toEqual({ unidad: 'MESES', minimo: '4', maximo: '8' })
  })

  it('mantiene la unidad por defecto cuando no hay límites', () => {
    expect(rangoEdadDesdeDias(undefined, undefined)).toEqual({ unidad: 'MESES', minimo: '', maximo: '' })
  })
})

describe('peso de referencia fijado por la unidad de dosis', () => {
  it('lo fija cuando la unidad ya expresa el peso', () => {
    expect(pesoReferenciaFijo('ML_POR_50KG')).toBe(50)
    expect(pesoReferenciaFijo('ML_POR_10KG')).toBe(10)
    expect(pesoReferenciaFijo('ML_POR_KG')).toBe(1)
    expect(pesoReferenciaFijo('MG_POR_KG')).toBe(1)
  })

  it('deja que el usuario lo indique con unidades absolutas o sin unidad', () => {
    expect(pesoReferenciaFijo('ML')).toBeUndefined()
    expect(pesoReferenciaFijo('MG')).toBeUndefined()
    expect(pesoReferenciaFijo('')).toBeUndefined()
  })
})

describe('edad objetivo frente al rango de animales elegibles', () => {
  it('acepta la edad objetivo dentro del rango, incluidos los límites', () => {
    expect(errorEdadObjetivoFueraDeRango(210, 90, 240)).toBeUndefined()
    expect(errorEdadObjetivoFueraDeRango(210, 210, 210)).toBeUndefined()
  })

  it('acepta cualquier edad objetivo cuando no hay rango o todavía no se escribió', () => {
    expect(errorEdadObjetivoFueraDeRango(210, undefined, undefined)).toBeUndefined()
    expect(errorEdadObjetivoFueraDeRango(undefined, 90, 120)).toBeUndefined()
  })

  it('explica cuando la edad objetivo supera el máximo de los elegibles', () => {
    expect(errorEdadObjetivoFueraDeRango(210, undefined, 180))
      .toBe('La edad objetivo (210 días) supera la edad máxima de los animales elegibles (180 días). Ajusta una de las dos.')
  })

  it('explica cuando la edad objetivo es menor que el mínimo de los elegibles', () => {
    expect(errorEdadObjetivoFueraDeRango(90, 120, undefined))
      .toBe('La edad objetivo (90 días) es menor que la edad mínima de los animales elegibles (120 días). Ajusta una de las dos.')
  })
})
