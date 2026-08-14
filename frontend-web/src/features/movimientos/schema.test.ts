import { describe, expect, it } from 'vitest'
import { anularMovimientoSchema, crearMovimientoSchema, revertirMovimientoSchema } from '@/features/movimientos/schema'

const UUID_A = '00000000-0000-0000-0000-000000000101'
const UUID_B = '00000000-0000-0000-0000-000000000102'
const ANIMAL = '00000000-0000-0000-0000-000000000201'

describe('crearMovimientoSchema', () => {
  it('acepta un cambio de potrero válido', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'CAMBIO_POTRERO',
      origenPotreroId: UUID_A,
      destinoPotreroId: UUID_B,
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(true)
  })

  it('rechaza un cambio de potrero sin potrero de destino', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'CAMBIO_POTRERO',
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(false)
  })

  it('rechaza un cambio de lote sin lote de destino', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'CAMBIO_LOTE',
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(false)
  })

  it('rechaza transferencia con la misma propiedad de origen y destino', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'TRANSFERENCIA_PROPIEDAD',
      origenPropiedadId: UUID_A,
      destinoPropiedadId: UUID_A,
      destinoPotreroId: UUID_B,
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(false)
  })

  it('rechaza transferencia sin potrero de destino', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'TRANSFERENCIA_PROPIEDAD',
      origenPropiedadId: UUID_A,
      destinoPropiedadId: UUID_B,
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(false)
  })

  it('rechaza cambio de potrero al mismo potrero', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'CAMBIO_POTRERO',
      origenPotreroId: UUID_A,
      destinoPotreroId: UUID_A,
      animales: [{ animalId: ANIMAL, version: 3 }],
    })
    expect(result.success).toBe(false)
  })

  it('rechaza sin animales', () => {
    const result = crearMovimientoSchema.safeParse({ tipo: 'SALIDA_VENTA', animales: [] })
    expect(result.success).toBe(false)
  })

  it('rechaza un animal que no es UUID', () => {
    const result = crearMovimientoSchema.safeParse({
      tipo: 'SALIDA_VENTA',
      animales: [{ animalId: 'x', version: 1 }],
    })
    expect(result.success).toBe(false)
  })
})

describe('anularMovimientoSchema', () => {
  it('acepta un motivo válido', () => {
    expect(anularMovimientoSchema.safeParse({ motivo: 'Movimiento registrado por error' }).success).toBe(true)
  })

  it('rechaza un motivo vacío', () => {
    expect(anularMovimientoSchema.safeParse({ motivo: '' }).success).toBe(false)
  })
})

describe('revertirMovimientoSchema', () => {
  it('acepta un motivo válido', () => {
    expect(revertirMovimientoSchema.safeParse({ motivo: 'Destino incorrecto' }).success).toBe(true)
  })

  it('rechaza un motivo vacío', () => {
    expect(revertirMovimientoSchema.safeParse({ motivo: '' }).success).toBe(false)
  })
})
