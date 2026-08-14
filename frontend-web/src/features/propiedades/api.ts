import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Propiedad {
  id: string
  codigo: string
  nombre: string
  descripcion?: string
  departamento?: string
  municipio?: string
  localidad?: string
  direccionReferencia?: string
  superficieHa?: number
  activo: boolean
  version: number
}

export interface Sector {
  id: string
  propiedadId: string
  codigo: string
  nombre: string
  descripcion?: string
  activo: boolean
  version: number
}

export type CreatePropiedad = Pick<Propiedad, 'nombre'> & Partial<Pick<Propiedad, 'codigo' | 'descripcion' | 'departamento' | 'municipio' | 'localidad' | 'direccionReferencia' | 'superficieHa'>>

export async function listPropiedades() {
  return (await http.get<ApiResponse<Propiedad[]>>('/api/v1/propiedades')).data.data
}

export async function createPropiedad(input: CreatePropiedad) {
  return (await http.post<ApiResponse<Propiedad>>('/api/v1/propiedades', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function updatePropiedad(id: string, input: Partial<Propiedad> & { version: number }) {
  return (await http.patch<ApiResponse<Propiedad>>(`/api/v1/propiedades/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listSectores(propiedadId: string) {
  return (await http.get<ApiResponse<Sector[]>>(`/api/v1/propiedades/${propiedadId}/sectores`)).data.data
}

export async function createSector(propiedadId: string, input: { codigo?: string; nombre: string; descripcion?: string }) {
  return (await http.post<ApiResponse<Sector>>(`/api/v1/propiedades/${propiedadId}/sectores`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function updateSector(id: string, input: Pick<Sector, 'nombre' | 'activo' | 'version'> & { codigo?: string; descripcion?: string }) {
  return (await http.patch<ApiResponse<Sector>>(`/api/v1/sectores/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
