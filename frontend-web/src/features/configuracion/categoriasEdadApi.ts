import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type { CategoriaAnimal } from '@/features/animales/types'

export interface RangoCategoriaInput {
  codigo: string
  nombre: string
  sexoAplicable: 'MACHO' | 'HEMBRA' | 'AMBOS'
  edadMinMeses?: number
  edadMaxMeses?: number
  descripcion?: string
  clasificacionAutomatica: boolean
  ordenEvaluacion: number
  confirmarHueco?: boolean
  id?: string
}

export interface ResultadoReclasificacion {
  procesados: number
  actualizados: number
  omitidos: number
  errores: number
}

const BASE = '/api/v1/configuracion/categorias-edad'

export async function listCategoriasEdad() {
  return (await http.get<ApiResponse<CategoriaAnimal[]>>(BASE)).data.data
}

export async function crearCategoriaEdad(input: RangoCategoriaInput) {
  return (await http.post<ApiResponse<CategoriaAnimal>>(BASE, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function actualizarCategoriaEdad(id: string, input: RangoCategoriaInput) {
  return (await http.put<ApiResponse<CategoriaAnimal>>(`${BASE}/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cambiarEstadoCategoriaEdad(id: string, activo: boolean) {
  return (await http.patch<ApiResponse<void>>(`${BASE}/${id}/estado`, { activo }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function eliminarCategoriaEdad(id: string) {
  return (await http.delete<ApiResponse<void>>(`${BASE}/${id}`, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function simularImpactoCategoriaEdad(input: RangoCategoriaInput) {
  return (await http.post<ApiResponse<{ animalesAfectados: number }>>(`${BASE}/simular`, input)).data.data
}

export async function reclasificarCategoriasEdad() {
  return (await http.post<ApiResponse<ResultadoReclasificacion>>(`${BASE}/reclasificar`, {}, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
