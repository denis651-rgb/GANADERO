import { describe, expect, it } from 'vitest'
import { createAnimalSchema } from './schema'

const id = '00000000-0000-4000-8000-000000000001'

describe('createAnimalSchema', () => {
  it('acepta el contrato requerido por CrearAnimalRequest', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
      fechaNacimiento: '2026-09-03',
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
      fechaNacimiento: '2026-09-03',
      razaPrincipalId: '50000000-0000-0000-0000-000000000005',
      categoriaActualId: '60000000-0000-0000-0000-000000000002',
      propiedadActualId: '20000000-0000-0000-0000-000000000001',
      potreroActualId: '30000000-0000-0000-0000-000000000001',
    }).success).toBe(true)
  })

  it('rechaza un animal nacido sin fecha de nacimiento', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA', proposito: 'CARNE', origen: 'NACIDO',
      razaPrincipalId: id, categoriaActualId: id, propiedadActualId: id, potreroActualId: id,
    }).success).toBe(false)
  })

  it('rechaza una recepcion anterior al nacimiento', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA', proposito: 'CARNE', origen: 'COMPRADO',
      fechaNacimiento: '2026-09-03', fechaIngreso: '2026-09-02',
      razaPrincipalId: id, categoriaActualId: id, propiedadActualId: id, potreroActualId: id,
    }).success).toBe(false)
  })

  it('acepta edad aproximada trazable para un animal nacido en campo', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA', proposito: 'CARNE', origen: 'NACIDO',
      edadDeclaradaValor: 20, edadDeclaradaUnidad: 'DIAS', fechaReferenciaEdad: '2026-09-04', fuenteEdad: 'ESTIMACION_CAMPO',
      razaPrincipalId: id, categoriaActualId: id, propiedadActualId: id, potreroActualId: id,
    }).success).toBe(true)
  })

  it('no permite mezclar fecha conocida y edad aproximada', () => {
    expect(createAnimalSchema.safeParse({
      sexo: 'HEMBRA', proposito: 'CARNE', origen: 'NACIDO', fechaNacimiento: '2026-09-01',
      edadDeclaradaValor: 3, edadDeclaradaUnidad: 'DIAS', fechaReferenciaEdad: '2026-09-04', fuenteEdad: 'ESTIMACION_CAMPO',
      razaPrincipalId: id, categoriaActualId: id, propiedadActualId: id, potreroActualId: id,
    }).success).toBe(false)
  })
})
