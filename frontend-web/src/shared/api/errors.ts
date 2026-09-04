import axios from 'axios'
import type { ApiErrorBody } from '@/shared/api/types'

export class AppError extends Error {
  readonly status?: number
  readonly code?: string
  readonly correlationId?: string

  constructor(message: string, options?: { status?: number; code?: string; correlationId?: string }) {
    super(message)
    this.name = 'AppError'
    this.status = options?.status
    this.code = options?.code
    this.correlationId = options?.correlationId
  }
}

export function normalizeApiError(error: unknown): AppError {
  if (error instanceof AppError) return error
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data
    if (!error.response) {
      return new AppError('No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.', { code: 'NETWORK_ERROR' })
    }
    const fieldLabels: Record<string, string> = {
      condicionCorporalActual: 'Condición corporal',
      pesoIngresoKg: 'Peso al ingreso',
      pesoNacimientoKg: 'Peso al nacer',
      pesoIngresoEstimado: 'Tipo de peso al ingreso',
      precioAdquisicion: 'Precio de adquisición',
      fechaNacimiento: 'Fecha de nacimiento',
      fechaIngreso: 'Fecha de ingreso',
    }
    const details = body?.fieldErrors?.map(({ field, message }) => `${fieldLabels[field] ?? field}: ${message}`).join('; ')
    return new AppError(details || body?.message || error.message, {
      status: error.response.status,
      code: body?.code,
      correlationId: body?.correlationId,
    })
  }
  return new AppError(error instanceof Error ? error.message : 'Ocurrió un error inesperado.')
}
