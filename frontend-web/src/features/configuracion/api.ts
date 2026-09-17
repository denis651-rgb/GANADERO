import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface Configuracion {
  zonaHoraria: string
  moneda: string
  unidadPeso: string
  unidadSuperficie: string
  diasAlertaPreparto: number
  diasSinPesaje: number
  diasAlertaDestete: number
  diasDiagnosticoPostServicio: number
  diasGestacionEstimada: number
  comprimirImagenes: boolean
  calidadImagen: number
  nombreUsuario?: string
  pinConfigurado: boolean
  version: number
}

export interface UpdateConfiguracionInput extends Partial<Omit<Configuracion, 'pinConfigurado' | 'version'>> {
  nuevoPin?: string
  quitarPin?: boolean
  version: number
}

export async function getConfiguracion() {
  return (await http.get<ApiResponse<Configuracion>>('/api/v1/configuracion')).data.data
}

export async function updateConfiguracion(input: UpdateConfiguracionInput) {
  return (await http.patch<ApiResponse<Configuracion>>('/api/v1/configuracion', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
