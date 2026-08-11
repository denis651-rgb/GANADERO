import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

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

export async function listPotreros() {
  return (await http.get<ApiResponse<Potrero[]>>('/api/v1/potreros')).data.data
}

export async function listTiposPasto() {
  return (await http.get<ApiResponse<TipoPasto[]>>('/api/v1/tipos-pasto')).data.data
}

export async function createPotrero(input: Omit<Potrero, 'id' | 'activo' | 'version'>) {
  return (await http.post<ApiResponse<Potrero>>('/api/v1/potreros', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function updatePotrero(id: string, input: UpdatePotreroInput) {
  return (await http.patch<ApiResponse<Potrero>>(`/api/v1/potreros/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
