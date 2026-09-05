import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type { Proveedor, ProveedorInput } from '@/features/proveedores/types'

export async function buscarProveedores(q: string, soloActivos = true) {
  return (await http.get<ApiResponse<Proveedor[]>>('/api/v1/proveedores', {
    params: { q: q || undefined, soloActivos },
  })).data.data
}

export async function getProveedor(id: string) {
  return (await http.get<ApiResponse<Proveedor>>(`/api/v1/proveedores/${id}`)).data.data
}

export async function crearProveedor(input: ProveedorInput) {
  return (await http.post<ApiResponse<Proveedor>>('/api/v1/proveedores', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function actualizarProveedor(id: string, input: ProveedorInput) {
  return (await http.put<ApiResponse<Proveedor>>(`/api/v1/proveedores/${id}`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cambiarEstadoProveedor(id: string, activo: boolean, version: number) {
  return (await http.patch<ApiResponse<Proveedor>>(`/api/v1/proveedores/${id}/estado`, { activo, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
