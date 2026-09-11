import axios from 'axios'
import { createUuid } from '@/shared/utils/uuid'

declare global {
  interface Window {
    ganadero?: {
      apiBaseUrl?: string
      quit?: () => Promise<void>
      googleCalendar?: {
        status: () => Promise<GoogleOAuthDesktopStatus>
        importClientConfig: (jsonText: string, fileName: string) => Promise<GoogleOAuthDesktopStatus>
        connect: () => Promise<GoogleOAuthDesktopStatus>
        revoke: () => Promise<GoogleOAuthDesktopStatus>
        changeAccount: () => Promise<GoogleOAuthDesktopStatus>
        syncNow: () => Promise<GoogleCalendarSyncResult>
      }
      backups?: BackupsDesktopBridge
    }
  }
}

export type BackupEstado = 'CREANDO' | 'CREADO_LOCALMENTE' | 'COPIANDO_A_CARPETA_EXTERNA'
  | 'COPIADO_A_CARPETA_EXTERNA' | 'ERROR_DE_COPIA' | 'INTEGRIDAD_INVALIDA'
export type BackupIntegridad = 'DESCONOCIDA' | 'VALIDA' | 'INVALIDA'
export type BackupFrecuencia = 'DIARIA' | 'SEMANAL' | 'MENSUAL'

export interface BackupInfo {
  nombreArchivo: string
  fechaCreacion: string
  tamanoBytes?: number
  hashSha256: string
  estado: BackupEstado
  integridad: BackupIntegridad
  ultimoError?: string | null
  fechaCopiaExterna?: string | null
}

export interface BackupSettings {
  version: 1
  automatico: boolean
  frecuencia: BackupFrecuencia
  hora: string
  destinoLocal: string
  destinoExterno: string | null
  retencionDiarios: number
  retencionSemanales: number
  retencionMensuales: number
  ultimoRespaldo: { fecha: string; periodoKey: string } | null
}

export interface BackupManifestInfo {
  formato: string
  versionFormato: number
  versionAplicacion: string
  versionBaseDatos: string | null
  fechaCreacion: string
  zonaHoraria: string
  empresaId: string | null
  archivoInterno: string
  tamanoBytes: number
  hashSha256: string
}

export interface BackupRestoreResult {
  ok: boolean
  mensaje: string
}

export interface BackupsDesktopBridge {
  backendStatus: 'ok' | 'failed'
  getSettings: () => Promise<BackupSettings>
  saveSettings: (settings: BackupSettings) => Promise<BackupSettings>
  selectExternalFolder: () => Promise<string | null>
  createNow: () => Promise<BackupInfo>
  copyExternal: (nombre: string) => Promise<void>
  list: () => Promise<BackupInfo[]>
  verify: (nombre: string) => Promise<BackupInfo>
  deleteBackup: (nombre: string) => Promise<void>
  selectRestoreFile: () => Promise<string | null>
  inspectRestoreFile: (filePath: string) => Promise<{ manifest: BackupManifestInfo }>
  restore: (filePath: string) => Promise<BackupRestoreResult>
  openLocalFolder: () => Promise<void>
  openExternalFolder: () => Promise<void>
}

export interface GoogleOAuthDesktopStatus {
  available: boolean
  connected: boolean
  email?: string
  expiresAt?: string
  message?: string
}

export interface GoogleCalendarSyncResult {
  generated: number
  claimed: number
  completed: number
  failed: number
  skipped?: 'NOT_CONNECTED' | 'AUTOMATIC_DISABLED'
}

export const http = axios.create({
  baseURL: window.ganadero?.apiBaseUrl || import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  config.headers.set('X-Correlation-Id', createUuid())
  return config
})
