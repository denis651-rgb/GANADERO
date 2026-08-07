import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export type EstadoInvitacion = 'PENDIENTE' | 'ACEPTADA' | 'VENCIDA' | 'CANCELADA' | 'ERROR_ENVIO'

export interface InvitacionResponse {
  id: string
  email: string
  estado: EstadoInvitacion
  fechaEnvio?: string
  fechaVencimiento?: string
  fechaAceptacion?: string
  fechaCancelacion?: string
  intentosEnvio: number
  ultimoErrorCodigo?: string
  ultimoErrorMensaje?: string
  motivoCancelacion?: string
  invitadoPor: string
  version: number
}

export interface InvitacionPage {
  items: InvitacionResponse[]
  total: number
  page: number
  size: number
}

export interface ActivacionInvitacionResponse {
  usuarioId: string
  nombres: string
  apellidos: string
  empresaId: string
  nombreEmpresa: string
  miembroEmpresaId: string
  roles: string[]
  permisos: string[]
  propiedadesPermitidas: string[]
  invitacion: InvitacionResponse
}

export interface ListarInvitacionesParams {
  estado?: EstadoInvitacion
  email?: string
  page?: number
  size?: number
}

export async function listarInvitaciones(params: ListarInvitacionesParams = {}) {
  return (await http.get<ApiResponse<InvitacionPage>>('/api/v1/usuarios/invitaciones', { params })).data.data
}

export async function consultarInvitacion(id: string) {
  return (await http.get<ApiResponse<InvitacionResponse>>(`/api/v1/usuarios/invitaciones/${id}`)).data.data
}

export async function crearInvitacion(input: { email: string; cargo?: string }) {
  return (await http.post<ApiResponse<InvitacionResponse>>(
    '/api/v1/usuarios/invitaciones',
    input,
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function reenviarInvitacion(id: string, version: number) {
  return (await http.post<ApiResponse<InvitacionResponse>>(
    `/api/v1/usuarios/invitaciones/${id}/reenviar`,
    { version },
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function cancelarInvitacion(id: string, motivo: string, version: number) {
  return (await http.post<ApiResponse<InvitacionResponse>>(
    `/api/v1/usuarios/invitaciones/${id}/cancelar`,
    { motivo, version },
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function activarInvitacion() {
  return (await http.post<ApiResponse<ActivacionInvitacionResponse>>(
    '/api/v1/auth/activar-invitacion',
    {},
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}
