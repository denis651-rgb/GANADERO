import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'

export interface Potrero {
  id: string
  propiedadId: string
  sectorId?: string
  codigo: string
  nombre: string
  superficieHa?: number
  tipoPastoId?: string
  capacidadUa?: number
  tieneAgua: boolean
  estado: 'DISPONIBLE' | 'OCUPADO' | 'DESCANSO' | 'MANTENIMIENTO'
  geometriaWkt?: string
  activo: boolean
  version: number
}

export type UpdatePotreroInput = Partial<Pick<Potrero, 'propiedadId' | 'sectorId' | 'codigo' | 'nombre' | 'superficieHa' | 'tipoPastoId' | 'capacidadUa' | 'tieneAgua' | 'estado' | 'geometriaWkt' | 'activo'>> & {
  quitarSector?: boolean
  quitarTipoPasto?: boolean
  quitarSuperficie?: boolean
  quitarCapacidad?: boolean
  version: number
}

export interface TipoPasto { id: string; codigo: string; nombre: string }

export async function listPotreros(filters: { propiedadId?: string; estado?: Potrero['estado'] | ''; sectorId?: string; page: number; size: number }) {
  return (await http.get<ApiResponse<Page<Potrero>>>('/api/v1/potreros', {
    params: { propiedadId: filters.propiedadId || undefined, estado: filters.estado || undefined, sectorId: filters.sectorId || undefined, page: filters.page, size: filters.size },
  })).data.data
}

export async function listAllPotreros() {
  return (await http.get<ApiResponse<Page<Potrero>>>('/api/v1/potreros', {
    params: { page: 0, size: 500 },
  })).data.data.content
}

export async function listTiposPasto() {
  return (await http.get<ApiResponse<TipoPasto[]>>('/api/v1/tipos-pasto')).data.data
}

export async function createPotrero(input: Omit<Potrero, 'id' | 'codigo' | 'activo' | 'version'> & { codigo?: string }) {
  return (await http.post<ApiResponse<Potrero>>('/api/v1/potreros', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function updatePotrero(id: string, input: UpdatePotreroInput) {
  return (await http.patch<ApiResponse<Potrero>>(`/api/v1/potreros/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
