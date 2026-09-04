import { describe, expect, it } from 'vitest'
import { calcularCapacidadRecomendada } from './capacidad'

describe('calcularCapacidadRecomendada', () => {
  const brachiaria = { id: '1', codigo: 'BRACHIARIA', nombre: 'Brachiaria' }

  it('aplica la carga del pasto y el factor de agua', () => {
    expect(calcularCapacidadRecomendada(50, brachiaria, true)).toBe(75)
    expect(calcularCapacidadRecomendada(50, brachiaria, false)).toBe(52.5)
  })

  it('no inventa capacidad si faltan superficie o pasto', () => {
    expect(calcularCapacidadRecomendada(0, brachiaria, true)).toBeUndefined()
    expect(calcularCapacidadRecomendada(50, undefined, true)).toBeUndefined()
  })
})
