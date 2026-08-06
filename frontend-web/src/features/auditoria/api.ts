import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'

export interface AuditoriaRegistro {
  id: string
  usuarioId?: string
  accion: string
  modulo: string
  entidad: string
  entidadId?: string
  correlationId?: string
  resultado: string
  datos?: Record<string, unknown>
  datosAnteriores?: Record<string, unknown>
  datosNuevos?: Record<string, unknown>
  dispositivo?: string
  ip?: string
  userAgent?: string
  createdAt: string
}

export interface AuditoriaFilters {
  usuarioId?: string
  modulo?: string
  accion?: string
  entidad?: string
  desde?: string
  hasta?: string
  page: number
  size: number
}

export async function listAuditoria(filters: AuditoriaFilters) {
  return (await http.get<ApiResponse<Page<AuditoriaRegistro>>>('/api/v1/auditoria', {
    params: {
      usuarioId: filters.usuarioId || undefined,
      modulo: filters.modulo || undefined,
      accion: filters.accion || undefined,
      entidad: filters.entidad || undefined,
      desde: filters.desde || undefined,
      hasta: filters.hasta || undefined,
      page: filters.page,
      size: filters.size,
    },
  })).data.data
}
