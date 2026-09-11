import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export type ModalidadVenta = 'EN_PIE' | 'CARNEADO'

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
  telefonoComprador?: string
  modalidad: ModalidadVenta
  precioUnitario?: number
  grupoVentaId?: string
}

export interface VentaInput {
  animalId: string
  fechaVenta?: string
  comprador: string
  precio: number
  moneda?: string
  observaciones?: string
  pesajeExistenteId?: string
  pesoVentaKg?: number
  tipoPeso?: 'MEDIDO' | 'ESTIMADO'
  dispositivo?: string
  telefonoComprador?: string
  modalidad?: ModalidadVenta
}

export interface VentaLoteInput {
  animalIds: string[]
  fechaVenta?: string
  comprador: string
  telefonoComprador?: string
  modalidad: ModalidadVenta
  precioCabeza?: number
  precioKg?: number
  pesosVentaKg?: Record<string, number>
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

export async function registrarVentaLote(input: VentaLoteInput) {
  return (await http.post<ApiResponse<Venta[]>>('/api/v1/ventas/lote', input, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  })).data.data
}
