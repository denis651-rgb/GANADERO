import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Venta {
  id: string
  animalId: string
  movimientoId: string
  fechaVenta: string
  comprador: string
  precio: number
  moneda: string
  pesoVentaKg?: number
  observaciones?: string
  createdBy: string
  createdAt: string
  version: number
}

export interface VentaInput {
  animalId: string
  fechaVenta?: string
  comprador: string
  precio: number
  moneda?: string
  pesoVentaKg?: number
  observaciones?: string
}

export interface VentaFilters {
  animalId?: string
  desde?: string
  hasta?: string
}

export async function listVentas(filters: VentaFilters = {}) {
  return (await http.get<ApiResponse<Venta[]>>('/api/v1/ventas', {
    params: {
      animalId: filters.animalId || undefined,
      desde: filters.desde || undefined,
      hasta: filters.hasta || undefined,
    },
  })).data.data
}

export async function registrarVenta(input: VentaInput) {
  return (await http.post<ApiResponse<Venta>>('/api/v1/ventas', input, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  })).data.data
}
