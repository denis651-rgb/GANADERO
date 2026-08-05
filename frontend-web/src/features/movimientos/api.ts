import { http } from '@/shared/api/http'
import type { ApiResponse, Page } from '@/shared/api/types'

export type TipoMovimiento = 'CAMBIO_POTRERO' | 'CAMBIO_LOTE' | 'TRANSFERENCIA_PROPIEDAD' | 'INGRESO_COMPRA' | 'SALIDA_VENTA' | 'CUARENTENA' | 'RETORNO_CUARENTENA'
export type EstadoMovimiento = 'PENDIENTE' | 'CONFIRMADO' | 'ANULADO'

export interface Movimiento {
  id: string
  tipo: TipoMovimiento
  estado: EstadoMovimiento
  fechaMovimiento: string
  motivo?: string
  origenPropiedadId?: string
  origenPotreroId?: string
  origenLoteId?: string
  destinoPropiedadId?: string
  destinoPotreroId?: string
  destinoLoteId?: string
  usuarioCrea?: string
  usuarioConfirma?: string
  usuarioAnula?: string
  fechaConfirmacion?: string
  fechaAnulacion?: string
  motivoAnulacion?: string
  version: number
}

export interface MovimientoDetalle {
  id: string
  animalId: string
  estadoAntes?: string
  estadoDespues?: string
}

export interface CreateMovimientoInput {
  tipo: TipoMovimiento
  fechaMovimiento?: string
  motivo?: string
  origenPropiedadId?: string
  origenPotreroId?: string
  origenLoteId?: string
  destinoPropiedadId?: string
  destinoPotreroId?: string
  destinoLoteId?: string
  animalIds: string[]
}

export async function listMovimientos(filters: { estado?: EstadoMovimiento | ''; tipo?: TipoMovimiento | ''; page: number; size: number }) {
  return (await http.get<ApiResponse<Page<Movimiento>>>('/api/v1/movimientos', {
    params: { estado: filters.estado || undefined, tipo: filters.tipo || undefined, page: filters.page, size: filters.size },
  })).data.data
}

export async function getMovimiento(id: string) { return (await http.get<ApiResponse<Movimiento>>(`/api/v1/movimientos/${id}`)).data.data }
export async function listDetalles(id: string) { return (await http.get<ApiResponse<MovimientoDetalle[]>>(`/api/v1/movimientos/${id}/animales`)).data.data }

export async function createMovimiento(input: CreateMovimientoInput) {
  return (await http.post<ApiResponse<Movimiento>>('/api/v1/movimientos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function confirmarMovimiento(id: string, version: number) {
  return (await http.post<ApiResponse<Movimiento>>(`/api/v1/movimientos/${id}/confirmar`, { version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function anularMovimiento(id: string, motivo: string, version: number) {
  return (await http.post<ApiResponse<Movimiento>>(`/api/v1/movimientos/${id}/anular`, { motivo, version }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
