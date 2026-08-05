import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'
import { createUuid } from '@/shared/utils/uuid'
import type { AnularPesajeInput, Pesaje, PesajeFilters, PesajeLoteInput, RegistrarPesajeInput } from '@/features/pesajes/types'

export async function listPesajes(filters: PesajeFilters) {
  return (await http.get<ApiResponse<Page<Pesaje>>>('/api/v1/pesajes', {
    params: {
      animalId: filters.animalId || undefined,
      propiedadId: filters.propiedadId || undefined,
      page: filters.page,
      size: filters.size,
    },
  })).data.data
}

export async function getPesaje(id: string) {
  return (await http.get<ApiResponse<Pesaje>>(`/api/v1/pesajes/${id}`)).data.data
}

export async function getPesajeHistory(animalId: string) {
  return (await http.get<ApiResponse<Pesaje[]>>(`/api/v1/pesajes/animal/${animalId}`)).data.data
}

export async function registrarPesaje(input: RegistrarPesajeInput) {
  return (await http.post<ApiResponse<Pesaje>>('/api/v1/pesajes', input, {
    headers: { 'Idempotency-Key': input.idempotencyKey ?? createUuid() },
  })).data.data
}

export async function registrarPesajeLote(input: PesajeLoteInput) {
  return (await http.post<ApiResponse<Pesaje[]>>('/api/v1/pesajes/lote', input, {
    headers: { 'Idempotency-Key': input.idempotencyKey ?? createUuid() },
  })).data.data
}

export async function anularPesaje(id: string, input: AnularPesajeInput) {
  return (await http.post<ApiResponse<Pesaje>>(`/api/v1/pesajes/${id}/anular`, input, {
    headers: { 'Idempotency-Key': createUuid() },
  })).data.data
}
