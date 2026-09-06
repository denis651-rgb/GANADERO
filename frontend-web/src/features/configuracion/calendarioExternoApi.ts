import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export type EstadoConexionCalendario = 'DESCONECTADO' | 'PENDIENTE_AUTORIZACION' | 'CONECTADO' | 'ERROR' | 'DESHABILITADO'

export interface ConfiguracionCalendarioExterno {
  id: string
  cuentaEmail?: string
  calendarioExternoId?: string
  calendarioNombre: string
  zonaHoraria: string
  sincronizacionAutomatica: boolean
  estado: EstadoConexionCalendario
  ultimoError?: string
  ultimaSincronizacion?: string
  version: number
}

export async function getConfiguracionCalendarioExterno(): Promise<ConfiguracionCalendarioExterno> {
  return (await http.get<ApiResponse<ConfiguracionCalendarioExterno>>('/api/v1/integraciones/calendario')).data.data
}

export interface ConteoColaCalendario { estado: 'PENDIENTE'|'PROCESANDO'|'REINTENTO'|'COMPLETADO'|'ERROR_DEFINITIVO'|'CANCELADO'; cantidad: number }
export interface EstadoSincronizacionCalendario { configuracion: ConfiguracionCalendarioExterno; cola: ConteoColaCalendario[] }

export async function guardarConfiguracionCalendarioExterno(input: Pick<ConfiguracionCalendarioExterno, 'cuentaEmail'|'calendarioNombre'|'zonaHoraria'|'sincronizacionAutomatica'>) {
  return (await http.put<ApiResponse<ConfiguracionCalendarioExterno>>('/api/v1/integraciones/calendario', input)).data.data
}
export async function getEstadoSincronizacionCalendario() {
  return (await http.get<ApiResponse<EstadoSincronizacionCalendario>>('/api/v1/integraciones/calendario/electron/estado')).data.data
}
export async function reintentarSincronizacionCalendario() {
  return (await http.post<ApiResponse<EstadoSincronizacionCalendario>>('/api/v1/integraciones/calendario/reintentar')).data.data
}

export interface EstadoOcurrenciaCalendario {
  ocurrenciaId: string
  actividad: string
  fechaPrevista: string
  propiedad?: string
  potrero?: string
  lote?: string
  animales: number
  estadoExterno?: string
  estadoCola?: string
  enlaceExterno?: string
  ultimaSincronizacion?: string
  error?: string
}
export async function listarEstadoOcurrenciasCalendario() {
  return (await http.get<ApiResponse<EstadoOcurrenciaCalendario[]>>('/api/v1/integraciones/calendario/ocurrencias')).data.data
}
export async function reintentarOcurrenciaCalendario(id: string) {
  await http.post(`/api/v1/integraciones/calendario/ocurrencias/${id}/reintentar`)
}
