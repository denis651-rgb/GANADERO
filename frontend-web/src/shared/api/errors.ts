import axios from 'axios'
import { SessionExpiredError } from '@/shared/api/http'
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
  if (error instanceof SessionExpiredError) {
    return new AppError(error.message, { status: 401, code: 'SESSION_EXPIRED' })
  }
  if (error instanceof AppError) return error
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data
    if (!error.response) {
      return new AppError('No se pudo conectar con el backend. Revisa que Spring Boot esté iniciado.', { code: 'NETWORK_ERROR' })
    }
    return new AppError(body?.message ?? error.message, {
      status: error.response.status,
      code: body?.code,
      correlationId: body?.correlationId,
    })
  }
  return new AppError(error instanceof Error ? error.message : 'Ocurrió un error inesperado.')
}
