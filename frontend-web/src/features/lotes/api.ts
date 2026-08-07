import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'

export type EstadoLote = 'ACTIVO' | 'CERRADO'
export type ModoIngreso = 'ATOMICO' | 'PARCIAL'

export interface Lote {
  id: string
  propiedadId: string
  codigo: string
  nombre: string
  descripcion?: string
  estado: EstadoLote
  fechaApertura: string
  fechaCierre?: string
  motivoCierre?: string
  version: number
}

export interface Membresia {
  id: string
  loteId: string
  animalId: string
  fechaIngreso: string
  fechaSalida?: string
  motivoIngreso?: string
  motivoSalida?: string
  observacion?: string
  modo?: string
  ingresadoPor?: string
  salidaPor?: string
  version: number
}

export interface ResultadoAccion {
  animalId: string
  estado: 'OK' | 'ERROR'
  mensaje: string
}

export interface IngresoMasivoResultado {
  ok: boolean
  procesados: number
  ingresados: number
  resultados: ResultadoAccion[]
}

export interface RetiroMasivoResultado {
  ok: boolean
  procesados: number
  retirados: number
  resultados: ResultadoAccion[]
}

export interface CreateLoteInput {
  propiedadId: string
  codigo: string
  nombre: string
  descripcion?: string
  fechaApertura?: string
}

export interface IngresoLoteInput {
  animalIds: string[]
  modo?: ModoIngreso
  fechaIngreso?: string
  motivo?: string
  observacion?: string
}

export interface RetiroLoteInput {
  animalIds: string[]
  fechaSalida?: string
  motivo?: string
}

export interface HistorialLoteParams {
  animalId?: string
  desde?: string
  hasta?: string
  motivoIngreso?: string
  motivoSalida?: string
  page: number
  size: number
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

export async function cerrarLote(id: string, version: number, motivo?: string, fechaCierre?: string) {
  return (await http.post<ApiResponse<Lote>>(`/api/v1/lotes/${id}/cerrar`, { version, motivo, fechaCierre }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listMembresias(loteId: string, activos = true) {
  return (await http.get<ApiResponse<Membresia[]>>(`/api/v1/lotes/${loteId}/animales`, { params: { activos } })).data.data
}

export async function addAnimales(loteId: string, input: IngresoLoteInput) {
  return (await http.post<ApiResponse<IngresoMasivoResultado>>(`/api/v1/lotes/${loteId}/animales`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function retirarAnimales(loteId: string, input: RetiroLoteInput) {
  return (await http.post<ApiResponse<RetiroMasivoResultado>>(`/api/v1/lotes/${loteId}/retirar-animales`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function historialLote(loteId: string, params: HistorialLoteParams) {
  return (await http.get<ApiResponse<Page<Membresia>>>(`/api/v1/lotes/${loteId}/historial`, { params })).data.data
}
