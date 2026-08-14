import { describe, expect, it } from 'vitest'
import { filtrarAnimalesPorOrigen, movementSearchAvailable } from './MovimientosPage'

const animales = [
  { codigo: 'ANI-001', nombre: 'Uno', propiedadActualId: 'propiedad-a', potreroActualId: 'potrero-a', loteActualId: 'lote-a' },
  { codigo: 'ANI-002', nombre: 'Dos', propiedadActualId: 'propiedad-a', potreroActualId: 'potrero-b', loteActualId: 'lote-a' },
  { codigo: 'ANI-003', nombre: 'Tres', propiedadActualId: 'propiedad-a', potreroActualId: 'potrero-a', loteActualId: 'lote-b' },
  { codigo: 'ANI-004', nombre: 'Cuatro', propiedadActualId: 'propiedad-b', potreroActualId: 'potrero-c' },
]

describe('MovimientosPage search availability', () => {
  it('no muestra un buscador que el contrato paginado no puede resolver correctamente', () => {
    expect(movementSearchAvailable).toBe(false)
  })

  it('no muestra animales hasta seleccionar una propiedad de origen', () => {
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: '', potreroId: '', loteId: '' })).toEqual([])
  })

  it('filtra progresivamente por propiedad, potrero y lote', () => {
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: 'propiedad-a', potreroId: '', loteId: '' }).map((animal) => animal.codigo)).toEqual(['ANI-001', 'ANI-002', 'ANI-003'])
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: 'propiedad-a', potreroId: 'potrero-a', loteId: '' }).map((animal) => animal.codigo)).toEqual(['ANI-001', 'ANI-003'])
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: 'propiedad-a', potreroId: '', loteId: 'lote-a' }).map((animal) => animal.codigo)).toEqual(['ANI-001', 'ANI-002'])
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: 'propiedad-a', potreroId: 'potrero-a', loteId: 'lote-a' }).map((animal) => animal.codigo)).toEqual(['ANI-001'])
  })

  it('aplica la búsqueda después de los filtros de ubicación', () => {
    expect(filtrarAnimalesPorOrigen(animales, { propiedadId: 'propiedad-a', potreroId: '', loteId: '' }, 'tres').map((animal) => animal.codigo)).toEqual(['ANI-003'])
  })
})
