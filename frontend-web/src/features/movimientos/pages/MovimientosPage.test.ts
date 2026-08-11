import { describe, expect, it } from 'vitest'
import { movementSearchAvailable } from './MovimientosPage'

describe('MovimientosPage search availability', () => {
  it('no muestra un buscador que el contrato paginado no puede resolver correctamente', () => {
    expect(movementSearchAvailable).toBe(false)
  })
})
