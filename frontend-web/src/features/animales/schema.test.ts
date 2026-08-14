import { describe, expect, it } from 'vitest'
import { createAnimalSchema } from './schema'

const id = '00000000-0000-4000-8000-000000000001'

describe('createAnimalSchema', () => {
  it('acepta el contrato requerido por CrearAnimalRequest', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
      razaPrincipalId: id,
      categoriaActualId: id,
      propiedadActualId: id,
      potreroActualId: id,
    }).success).toBe(true)
  })

  it('rechaza el contrato anterior sin referencias obligatorias', () => {
    expect(createAnimalSchema.safeParse({
      codigo: 'A-001',
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
      propiedadId: id,
      potreroId: id,
    }).success).toBe(false)
  })

  it('acepta los UUID determinísticos de los catálogos del backend', () => {
    expect(createAnimalSchema.safeParse({
      codigo: 'A-002',
      sexo: 'HEMBRA',
      proposito: 'DOBLE_PROPOSITO',
      origen: 'NACIDO',
      razaPrincipalId: '50000000-0000-0000-0000-000000000005',
      categoriaActualId: '60000000-0000-0000-0000-000000000002',
      propiedadActualId: '20000000-0000-0000-0000-000000000001',
      potreroActualId: '30000000-0000-0000-0000-000000000001',
    }).success).toBe(true)
  })
})
