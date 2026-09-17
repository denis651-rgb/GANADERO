import { dialog, shell } from 'electron'
import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import path from 'node:path'
import type { BackendManager } from '../backend'
import { computeSha256, extractDatabaseEntry, readManifestFromArchive, verifyExtractedHash } from './archive'
import { conLockDeOperacion } from './lock'
import type { RespaldoResumen } from './retention'
import { inspeccionarRespaldo, restaurarRespaldo, type RestoreInfo, type RestoreResult } from './restore'
import { loadSettings, saveSettings, type BackupSettings } from './settings'

export interface RespaldoInfo extends RespaldoResumen {
  hashSha256: string
  ultimoError?: string | null
  fechaCopiaExterna?: string | null
}

async function leerRespuesta<T>(response: Response): Promise<T> {
  const body = (await response.json()) as { ok: boolean; data: T }
  return body.data
}

/**
 * Orquesta el módulo de respaldos desde Electron: la creación/listado/verificación reales viven
 * en el backend (HTTP local); Electron aporta selección de carpetas, copia a la carpeta externa
 * sincronizada, programación y el apagado/reinicio del backend durante una restauración.
 *
 * `list()`/`verify()` intentan primero el backend y caen a un escaneo directo de la carpeta local
 * (vía archive.ts) si no responde — es lo que permite que la ventana de recuperación funcione sin
 * backend disponible.
 */
export class BackupManager {
  constructor(private readonly backend: BackendManager) {}

  private baseUrl(): string {
    return `http://127.0.0.1:${this.backend.port}`
  }

  async getSettings(): Promise<BackupSettings> {
    return loadSettings()
  }

  async saveSettings(input: BackupSettings): Promise<BackupSettings> {
    return saveSettings(input)
  }

  async selectExternalFolder(): Promise<string | null> {
    const result = await dialog.showOpenDialog({ properties: ['openDirectory', 'createDirectory'] })
    if (result.canceled || result.filePaths.length === 0) return null
    return result.filePaths[0]
  }

  async createNow(): Promise<RespaldoInfo> {
    return conLockDeOperacion(() => this.crearSinLock())
  }

  /** Usado también por restore.ts para el respaldo preventivo — ya corre dentro del lock de restauración. */
  async crearSinLock(): Promise<RespaldoInfo> {
    const response = await fetch(`${this.baseUrl()}/api/v1/respaldos`, { method: 'POST' })
    if (!response.ok) throw new Error(await mensajeError(response, 'No se pudo crear el respaldo.'))
    const creado = await leerRespuesta<RespaldoInfo>(response)
    const settings = await loadSettings()
    if (settings.destinoExterno) {
      try {
        await this.copyToExternal(creado.nombreArchivo)
      } catch (error) {
        console.error('[backups] la copia a la carpeta externa falló:', error)
      }
    }
    return creado
  }

  async copyToExternal(nombreArchivo: string): Promise<void> {
    const settings = await loadSettings()
    if (!settings.destinoExterno) throw new Error('No hay una carpeta externa configurada.')
    const origen = path.join(settings.destinoLocal, nombreArchivo)
    const destinoFinal = path.join(settings.destinoExterno, nombreArchivo)
    const destinoTemporal = `${destinoFinal}.part`

    await this.reportarEstadoExterno(nombreArchivo, 'COPIANDO_A_CARPETA_EXTERNA')
    try {
      await fs.mkdir(settings.destinoExterno, { recursive: true })
      if (await existe(destinoFinal)) throw new Error('Ya existe un archivo con ese nombre en la carpeta externa.')
      await fs.copyFile(origen, destinoTemporal)
      const [origenStat, destinoStat] = await Promise.all([fs.stat(origen), fs.stat(destinoTemporal)])
      if (origenStat.size !== destinoStat.size) throw new Error('El tamaño copiado no coincide con el original.')
      if (computeSha256(origen) !== computeSha256(destinoTemporal)) throw new Error('El hash copiado no coincide con el original.')
      await fs.rename(destinoTemporal, destinoFinal)
      await this.reportarEstadoExterno(nombreArchivo, 'COPIADO_A_CARPETA_EXTERNA')
    } catch (error) {
      await fs.rm(destinoTemporal, { force: true })
      await this.reportarEstadoExterno(nombreArchivo, 'ERROR_DE_COPIA', (error as Error).message)
      throw error
    }
  }

  private async reportarEstadoExterno(nombreArchivo: string, estado: string, error?: string): Promise<void> {
    try {
      await fetch(`${this.baseUrl()}/api/v1/respaldos/${encodeURIComponent(nombreArchivo)}/estado-externo`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ estado, error: error ?? null }),
      })
    } catch (reportError) {
      console.error('[backups] no se pudo informar el estado de copia externa al backend:', reportError)
    }
  }

  async list(): Promise<RespaldoInfo[]> {
    try {
      const response = await fetch(`${this.baseUrl()}/api/v1/respaldos`)
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      return await leerRespuesta<RespaldoInfo[]>(response)
    } catch {
      return this.listarDesdeCarpetaLocal()
    }
  }

  /** Modo recuperación (sin backend): lee cada .ganadero-backup directamente de la carpeta local. */
  private async listarDesdeCarpetaLocal(): Promise<RespaldoInfo[]> {
    const settings = await loadSettings()
    let archivos: string[]
    try {
      archivos = await fs.readdir(settings.destinoLocal)
    } catch {
      return []
    }
    const resultado: RespaldoInfo[] = []
    for (const nombre of archivos.filter((n) => n.endsWith('.ganadero-backup'))) {
      try {
        const manifest = readManifestFromArchive(path.join(settings.destinoLocal, nombre))
        resultado.push({
          nombreArchivo: nombre, fechaCreacion: manifest.fechaCreacion, estado: 'CREADO_LOCALMENTE',
          integridad: 'DESCONOCIDA', hashSha256: manifest.hashSha256,
        })
      } catch {
        // Un archivo dañado o ilegible se omite del listado en vez de bloquear toda la pantalla de recuperación.
      }
    }
    return resultado.sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion))
  }

  /**
   * A diferencia de list() (que cae a lectura local ante cualquier error, incluida una respuesta
   * de error del backend, porque es de solo lectura y así funciona la ventana de recuperación sin
   * backend), aquí solo se cae al cálculo local cuando el backend es inalcanzable (fetch lanza,
   * p. ej. el proceso no arrancó). Si el backend respondió pero con un error real (archivo
   * borrado del disco, etc.), ese error se debe propagar: el cálculo local no persiste su
   * resultado en la tabla `respaldos`, así que enmascararlo como éxito dejaba al respaldo mostrando
   * "Sin verificar" de nuevo en el próximo refresco, pese al aviso de éxito que veía el usuario.
   */
  async verify(nombreArchivo: string): Promise<RespaldoInfo> {
    let response: Response
    try {
      response = await fetch(`${this.baseUrl()}/api/v1/respaldos/${encodeURIComponent(nombreArchivo)}/verificar`, { method: 'POST' })
    } catch {
      return this.verificarDesdeCarpetaLocal(nombreArchivo)
    }
    if (!response.ok) throw new Error(await mensajeError(response, 'No se pudo verificar el respaldo.'))
    return await leerRespuesta<RespaldoInfo>(response)
  }

  private async verificarDesdeCarpetaLocal(nombreArchivo: string): Promise<RespaldoInfo> {
    const settings = await loadSettings()
    const archivo = path.join(settings.destinoLocal, nombreArchivo)
    const manifest = readManifestFromArchive(archivo)
    const temporal = path.join(settings.destinoLocal, `.verificacion-${crypto.randomUUID()}.db`)
    try {
      extractDatabaseEntry(archivo, temporal)
      const integro = verifyExtractedHash(temporal, manifest)
      return {
        nombreArchivo, fechaCreacion: manifest.fechaCreacion, hashSha256: manifest.hashSha256,
        estado: integro ? 'CREADO_LOCALMENTE' : 'INTEGRIDAD_INVALIDA', integridad: integro ? 'VALIDA' : 'INVALIDA',
      }
    } finally {
      await fs.rm(temporal, { force: true })
    }
  }

  async deleteBackup(nombreArchivo: string): Promise<void> {
    const response = await fetch(`${this.baseUrl()}/api/v1/respaldos/${encodeURIComponent(nombreArchivo)}`, { method: 'DELETE' })
    if (!response.ok) throw new Error(await mensajeError(response, 'No se pudo eliminar el respaldo.'))
  }

  async selectRestoreFile(): Promise<string | null> {
    const result = await dialog.showOpenDialog({
      properties: ['openFile'],
      filters: [{ name: 'Respaldo de Ganadero', extensions: ['ganadero-backup'] }],
    })
    if (result.canceled || result.filePaths.length === 0) return null
    return result.filePaths[0]
  }

  async inspectRestoreFile(archivoPath: string): Promise<RestoreInfo> {
    return inspeccionarRespaldo(archivoPath)
  }

  async restore(archivoPath: string): Promise<RestoreResult> {
    return restaurarRespaldo({ backend: this.backend, crearRespaldoPreventivo: () => this.crearSinLock() }, archivoPath)
  }

  async openLocalFolder(): Promise<void> {
    const settings = await loadSettings()
    await fs.mkdir(settings.destinoLocal, { recursive: true })
    await shell.openPath(settings.destinoLocal)
  }

  async openExternalFolder(): Promise<void> {
    const settings = await loadSettings()
    if (!settings.destinoExterno) throw new Error('No hay una carpeta externa configurada.')
    await shell.openPath(settings.destinoExterno)
  }
}

async function existe(rutaArchivo: string): Promise<boolean> {
  try {
    await fs.access(rutaArchivo)
    return true
  } catch {
    return false
  }
}

async function mensajeError(response: Response, fallback: string): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    return body?.message ?? fallback
  } catch {
    return fallback
  }
}
