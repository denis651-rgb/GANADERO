import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface ConfiguracionTelegram {
  id: string
  empresaId: string
  botToken: string
  chatIdPrincipal: string
  chatIdsJson: string
  activo: boolean
  notificarSanidad: boolean
  notificarReproduccion: boolean
  horaNotificacion: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface EstadoIntegracionTelegram {
  activo: boolean
  telegramConfigurado: boolean
  firebaseConectado: boolean
  alertasPendientesSincronizacion: number
  horaNotificacion: string
  notificarSanidad: boolean
  notificarReproduccion: boolean
}

export interface GuardarTelegramInput {
  botToken: string
  chatIdPrincipal: string
  chatIdsJson?: string
  activo: boolean
  notificarSanidad: boolean
  notificarReproduccion: boolean
  horaNotificacion: string
}

export interface EnviarPruebaInput {
  botToken?: string
  chatId?: string
}

export interface ResultadoEnvioTelegram {
  exito: boolean
  mensaje: string
  httpStatus: number
}

export async function getConfiguracionTelegram(): Promise<ConfiguracionTelegram> {
  return (await http.get<ApiResponse<ConfiguracionTelegram>>('/api/v1/integraciones/telegram')).data.data
}

export async function getEstadoIntegracionTelegram(): Promise<EstadoIntegracionTelegram> {
  return (await http.get<ApiResponse<EstadoIntegracionTelegram>>('/api/v1/integraciones/telegram/estado')).data.data
}

export async function guardarConfiguracionTelegram(input: GuardarTelegramInput): Promise<ConfiguracionTelegram> {
  return (await http.put<ApiResponse<ConfiguracionTelegram>>('/api/v1/integraciones/telegram', input)).data.data
}

export async function probarNotificacionTelegram(input: EnviarPruebaInput): Promise<ResultadoEnvioTelegram> {
  return (await http.post<ApiResponse<ResultadoEnvioTelegram>>('/api/v1/integraciones/telegram/test', input)).data.data
}

export async function sincronizarAlertasFirebase(): Promise<number> {
  return (await http.post<ApiResponse<number>>('/api/v1/integraciones/telegram/sincronizar')).data.data
}
