import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type { Compra, CompraDetalle, CompraInput, CompraPage, DependenciaCompra, EstadoCompra, ResumenCompraAnimal } from '@/features/compras/types'

export async function listCompras(params: { estado?: EstadoCompra; page: number; size: number }) {
  return (await http.get<ApiResponse<CompraPage>>('/api/v1/compras', {
    params: { estado: params.estado || undefined, page: params.page, size: params.size },
  })).data.data
}

export async function getCompra(id: string) {
  return (await http.get<ApiResponse<Compra>>(`/api/v1/compras/${id}`)).data.data
}

export async function getCompraDetalles(id: string) {
  return (await http.get<ApiResponse<CompraDetalle[]>>(`/api/v1/compras/${id}/detalles`)).data.data
}

export async function getCompraDependencias(id: string) {
  return (await http.get<ApiResponse<DependenciaCompra[]>>(`/api/v1/compras/${id}/dependencias`)).data.data
}

export async function getResumenCompraAnimal(animalId: string) {
  return (await http.get<ApiResponse<ResumenCompraAnimal | null>>(`/api/v1/compras/animal/${animalId}`)).data.data
}

export async function crearCompra(input: CompraInput) {
  return (await http.post<ApiResponse<Compra>>('/api/v1/compras', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function actualizarCompra(id: string, input: CompraInput) {
  return (await http.put<ApiResponse<Compra>>(`/api/v1/compras/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function confirmarCompra(id: string, version: number) {
  return (await http.post<ApiResponse<Compra>>(`/api/v1/compras/${id}/confirmar`, { version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function anularCompra(id: string, motivo: string, version: number) {
  return (await http.post<ApiResponse<Compra>>(`/api/v1/compras/${id}/anular`, { motivo, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
