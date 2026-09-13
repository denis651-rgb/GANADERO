import AdmZip from 'adm-zip'
import { app, dialog } from 'electron'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import type { BackendManager } from './backend'
import { getLogsDir } from './logging'

export interface DiagnosticoResult {
  cancelado: boolean
  path?: string
}

function informacionSistema(backend: BackendManager): string {
  const info = {
    fecha: new Date().toISOString(),
    appVersion: app.getVersion(),
    versiones: { electron: process.versions.electron, chrome: process.versions.chrome, node: process.versions.node },
    plataforma: `${process.platform} ${os.release()} (${process.arch})`,
    backendPuerto: backend.port,
    rutas: { db: backend.dbPath, media: backend.mediaPath, backups: backend.backupsPath, logs: getLogsDir() },
  }
  return JSON.stringify(info, null, 2)
}

/**
 * Empaqueta logs/ (Electron + backend, ver logging.ts/backend.ts) y datos básicos del entorno en
 * un .zip que el usuario guarda donde quiera — pensado para adjuntar a un reporte de soporte sin
 * pedirle que navegue manualmente hasta la carpeta de logs.
 */
export async function exportarDiagnostico(backend: BackendManager): Promise<DiagnosticoResult> {
  const resultado = await dialog.showSaveDialog({
    title: 'Guardar información de diagnóstico',
    defaultPath: `ganadero-diagnostico-${new Date().toISOString().replace(/[:.]/g, '-')}.zip`,
    filters: [{ name: 'Archivo ZIP', extensions: ['zip'] }],
  })
  if (resultado.canceled || !resultado.filePath) return { cancelado: true }

  const zip = new AdmZip()
  zip.addFile('info-sistema.json', Buffer.from(informacionSistema(backend)))

  const carpetaLogs = getLogsDir()
  try {
    for (const nombre of fs.readdirSync(carpetaLogs)) {
      if (nombre.endsWith('.log')) zip.addLocalFile(path.join(carpetaLogs, nombre), 'logs')
    }
  } catch {
    // instalación recién creada: todavía no existe la carpeta de logs
  }

  zip.writeZip(resultado.filePath)
  return { cancelado: false, path: resultado.filePath }
}
