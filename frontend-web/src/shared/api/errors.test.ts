import { describe, expect, it } from 'vitest'
import { normalizeApiError } from './errors'

describe('normalizeApiError', () => {
  it('muestra el campo rechazado en lugar del aviso genérico', () => {
    const error = normalizeApiError({ isAxiosError: true, response: { status: 400, data: {
      code: 'VALIDATION_ERROR', message: 'La solicitud contiene datos inválidos.', correlationId: 'test-id',
      fieldErrors: [{ field: 'condicionCorporalActual', message: 'debe ser mayor o igual a 1.0' }],
    } } })
    expect(error.message).toBe('Condición corporal: debe ser mayor o igual a 1.0')
    expect(error.status).toBe(400)
    expect(error.correlationId).toBe('test-id')
  })

  it('conserva el mensaje del servidor cuando no hay detalles', () => {
    expect(normalizeApiError({ isAxiosError: true, response: { status: 409, data: {
      message: 'El registro cambió.', fieldErrors: [],
    } } }).message).toBe('El registro cambió.')
  })
})
