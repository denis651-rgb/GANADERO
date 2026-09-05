import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type {
  ConfirmarMovimientoLoteInput,
  PrepararMovimientoLoteInput,
  PreparacionMovimientoLote,
  ResultadoMovimientoLote,
} from '@/features/movimientolote/types'

export async function prepararMovimientoLote(loteId: string, input: PrepararMovimientoLoteInput) {
  return (await http.post<ApiResponse<PreparacionMovimientoLote>>(
    `/api/v1/lotes/${loteId}/movimiento-lote/preparar`, input,
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function obtenerPreparacionMovimientoLote(id: string) {
  return (await http.get<ApiResponse<PreparacionMovimientoLote>>(`/api/v1/movimiento-lote/preparaciones/${id}`)).data.data
}

export async function confirmarMovimientoLote(id: string, input: ConfirmarMovimientoLoteInput) {
  return (await http.post<ApiResponse<ResultadoMovimientoLote>>(
    `/api/v1/movimiento-lote/preparaciones/${id}/confirmar`, input,
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function cancelarPreparacionMovimientoLote(id: string) {
  return (await http.post<ApiResponse<void>>(
    `/api/v1/movimiento-lote/preparaciones/${id}/cancelar`, {},
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}
