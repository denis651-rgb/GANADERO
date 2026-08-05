import { describe, expect, it } from 'vitest'
import { anularPesajeSchema, pesajeLoteSchema, registrarPesajeSchema } from '@/features/pesajes/schema'

const ANIMAL = '00000000-0000-0000-0000-000000000001'
const LOTE = '00000000-0000-0000-0000-000000000002'

describe('registrarPesajeSchema', () => {
  it('acepta un pesaje válido con peso en texto', () => {
    const result = registrarPesajeSchema.safeParse({ animalId: ANIMAL, pesoKg: '150', tipo: 'RUTINA' })
    expect(result.success).toBe(true)
    if (result.success) expect(result.data.pesoKg).toBe(150)
  })

  it('rechaza un peso no positivo', () => {
    const result = registrarPesajeSchema.safeParse({ animalId: ANIMAL, pesoKg: '0', tipo: 'RUTINA' })
    expect(result.success).toBe(false)
  })

  it('rechaza un animal que no es UUID', () => {
    const result = registrarPesajeSchema.safeParse({ animalId: 'x', pesoKg: '150', tipo: 'RUTINA' })
    expect(result.success).toBe(false)
  })

  it('convierte campos vacíos a undefined', () => {
    const result = registrarPesajeSchema.safeParse({
      animalId: ANIMAL,
      pesoKg: '150',
      tipo: 'RUTINA',
      propiedadId: '',
      condicionCorporal: '',
    })
    expect(result.success).toBe(true)
    if (result.success) {
      expect(result.data.propiedadId).toBeUndefined()
      expect(result.data.condicionCorporal).toBeUndefined()
    }
  })
})

describe('pesajeLoteSchema', () => {
  it('acepta un pesaje por lote válido', () => {
    const result = pesajeLoteSchema.safeParse({ loteId: LOTE, pesoKg: '200' })
    expect(result.success).toBe(true)
    if (result.success) expect(result.data.pesoKg).toBe(200)
  })

  it('rechaza sin lote', () => {
    const result = pesajeLoteSchema.safeParse({ pesoKg: '200' })
    expect(result.success).toBe(false)
  })
})

describe('anularPesajeSchema', () => {
  it('acepta un motivo válido', () => {
    expect(anularPesajeSchema.safeParse({ motivo: 'Peso registrado por error' }).success).toBe(true)
  })

  it('rechaza un motivo vacío', () => {
    expect(anularPesajeSchema.safeParse({ motivo: '' }).success).toBe(false)
  })
})
