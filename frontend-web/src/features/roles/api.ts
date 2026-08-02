import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Permiso { id: string; codigo: string; nombre: string; modulo: string }
export interface Rol { id: string; codigo: string; nombre: string; descripcion?: string; sistema: boolean; activo: boolean; version: number; permisos: Permiso[] }

export async function listRoles() { return (await http.get<ApiResponse<Rol[]>>('/api/v1/roles')).data.data }
export async function listPermisos() { return (await http.get<ApiResponse<Permiso[]>>('/api/v1/roles/permisos')).data.data }
export async function createRol(input: { codigo: string; nombre: string; descripcion?: string }) { return (await http.post<ApiResponse<Rol>>('/api/v1/roles', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function updateRol(id: string, input: Partial<Rol> & { version: number }) { return (await http.patch<ApiResponse<Rol>>(`/api/v1/roles/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
export async function assignPermisos(id: string, ids: string[], version: number) { return (await http.put<ApiResponse<Rol>>(`/api/v1/roles/${id}/permisos`, { ids, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data }
