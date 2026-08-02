import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface UsuarioRol { id: string; codigo: string; nombre: string }
export interface Miembro { id: string; usuarioId: string; nombres: string; apellidos: string; telefono?: string; cargo?: string; estado: 'ACTIVO' | 'BLOQUEADO' | 'INACTIVO'; accesoTodasPropiedades: boolean; perfilVersion: number; version: number; roles: UsuarioRol[]; propiedadesPermitidas: string[] }

export async function listUsuarios() { return (await http.get<ApiResponse<Miembro[]>>('/api/v1/usuarios')).data.data }
export async function createUsuario(input: { email: string; nombres: string; apellidos: string; telefono?: string; cargo?: string; accesoTodasPropiedades: boolean; roles: string[]; propiedades: string[] }) { return (await http.post<ApiResponse<Miembro>>('/api/v1/usuarios/invitaciones', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function setUsuarioEstado(id: string, action: 'activar' | 'bloquear', version: number) { return (await http.post<ApiResponse<Miembro>>(`/api/v1/usuarios/${id}/${action}`, { version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function assignRoles(id: string, ids: string[], version: number) { return (await http.put<ApiResponse<Miembro>>(`/api/v1/usuarios/${id}/roles`, { ids, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
