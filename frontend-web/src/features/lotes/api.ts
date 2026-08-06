import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'

export type EstadoLote = 'ABIERTO' | 'CERRADO'

export interface Lote {
  id: string
  propiedadId: string
  codigo: string
  nombre: string
  descripcion?: string
  estado: EstadoLote
  fechaApertura: string
  fechaCierre?: string
  version: number
}

export interface Membresia {
  id: string
  loteId: string
  animalId: string
  fechaIngreso: string
  fechaSalida?: string
  motivoSalida?: string
}

export interface CreateLoteInput {
  propiedadId: string
  codigo: string
  nombre: string
  descripcion?: string
  fechaApertura?: string
}

export async function listLotes(filters: { estado?: EstadoLote | ''; search?: string; page: number; size: number }) {
  return (await http.get<ApiResponse<Page<Lote>>>('/api/v1/lotes', {
    params: { estado: filters.estado || undefined, search: filters.search || undefined, page: filters.page, size: filters.size },
  })).data.data
}

export async function getLote(id: string) { return (await http.get<ApiResponse<Lote>>(`/api/v1/lotes/${id}`)).data.data }

export async function createLote(input: CreateLoteInput) {
  return (await http.post<ApiResponse<Lote>>('/api/v1/lotes', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function updateLote(id: string, input: Partial<CreateLoteInput> & { version: number }) {
  return (await http.patch<ApiResponse<Lote>>(`/api/v1/lotes/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cerrarLote(id: string, version: number) {
  return (await http.post<ApiResponse<Lote>>(`/api/v1/lotes/${id}/cerrar`, { version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listMembresias(loteId: string, activos = true) {
  return (await http.get<ApiResponse<Membresia[]>>(`/api/v1/lotes/${loteId}/animales`, { params: { activos } })).data.data
}

export async function addAnimales(loteId: string, animalIds: string[]) {
  return (await http.post<ApiResponse<Membresia[]>>(`/api/v1/lotes/${loteId}/animales`, { animalIds }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function retirarAnimales(loteId: string, animalIds: string[], motivo?: string) {
  return (await http.post<ApiResponse<Membresia[]>>(`/api/v1/lotes/${loteId}/retirar-animales`, { animalIds, motivo }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
