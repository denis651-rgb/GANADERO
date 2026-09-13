import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import path from 'node:path'
import type { BackendManager } from '../backend'
import {
  DATABASE_ENTRY, extractDatabaseEntry, extractMediaEntries, readManifestFromArchive, verifyExtractedHash,
  type RespaldoManifest,
} from './archive'
import { conLockDeOperacion } from './lock'

export interface RestoreDependencies {
  backend: BackendManager
  /** Respaldo preventivo de la base ACTUAL, con el backend todavía arriba (paso 9 del flujo). */
  crearRespaldoPreventivo: () => Promise<unknown>
}

export interface RestoreInfo {
  manifest: RespaldoManifest
}

export interface RestoreResult {
  ok: boolean
  mensaje: string
}

const MAX_ARCHIVO_BYTES = 8 * 1024 * 1024 * 1024 // 8 GB: base SQLite + fotos de animales.

/** Pasos 1-7: valida extensión/tamaño, extrae, valida manifiesto y compara hash. No toca la base viva. */
export async function inspeccionarRespaldo(archivoPath: string): Promise<RestoreInfo> {
  if (!archivoPath.toLowerCase().endsWith('.ganadero-backup')) {
    throw new Error('El archivo seleccionado no tiene la extensión .ganadero-backup.')
  }
  const stat = await fs.stat(archivoPath)
  if (stat.size === 0 || stat.size > MAX_ARCHIVO_BYTES) {
    throw new Error('El tamaño del archivo de respaldo es inválido.')
  }
  const manifest = readManifestFromArchive(archivoPath)
  return { manifest }
}

async function existeCarpeta(ruta: string): Promise<boolean> {
  try {
    await fs.access(ruta)
    return true
  } catch {
    return false
  }
}

/** Pasos 9-17: crea un respaldo preventivo, detiene el backend, reemplaza la base (y la media, si el respaldo la trae) y reinicia; revierte todo si el arranque falla. */
export async function restaurarRespaldo(deps: RestoreDependencies, archivoPath: string): Promise<RestoreResult> {
  return conLockDeOperacion(async () => {
    const { manifest } = await inspeccionarRespaldo(archivoPath)

    const dbActual = deps.backend.dbPath
    const dbDir = path.dirname(dbActual)
    const mediaActual = deps.backend.mediaPath
    // Se extrae en la misma carpeta que la base viva para que el fs.rename final (paso 12) sea
    // un movimiento atómico dentro del mismo volumen, no una copia entre discos distintos.
    const carpetaTemporal = path.join(dbDir, `.restore-tmp-${crypto.randomUUID()}`)
    await fs.mkdir(carpetaTemporal, { recursive: true })
    const dbExtraida = path.join(carpetaTemporal, 'ganadero.restaurada.db')
    const mediaExtraida = path.join(carpetaTemporal, 'media')

    try {
      extractDatabaseEntry(archivoPath, dbExtraida)
      if (!verifyExtractedHash(dbExtraida, manifest)) {
        throw new Error('El hash del respaldo extraído no coincide con el manifiesto; el archivo pudo corromperse.')
      }
      // Un respaldo de formato anterior (v1) no trae media/: 0 archivos, y más abajo no se toca
      // la carpeta de media existente en esta instalación.
      const archivosMedia = extractMediaEntries(archivoPath, mediaExtraida)

      // Paso 9: respaldo preventivo de la base actual, con el backend todavía arriba.
      await deps.crearRespaldoPreventivo()

      // Paso 10: detener el backend de forma ordenada (espera a que el proceso termine de verdad).
      await deps.backend.stop()

      // Paso 11: la base actual se renombra como copia de recuperación, nunca se borra.
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
      const dbRecuperacion = path.join(dbDir, `ganadero.recovery-${timestamp}.db`)
      await fs.rename(dbActual, dbRecuperacion)

      // Misma lógica de "renombrar, nunca borrar" para la media, solo si el respaldo la trae.
      const mediaRecuperacion = `${mediaActual}.recovery-${timestamp}`
      let mediaFueReemplazada = false
      if (archivosMedia > 0) {
        if (await existeCarpeta(mediaActual)) await fs.rename(mediaActual, mediaRecuperacion)
        await fs.rename(mediaExtraida, mediaActual)
        mediaFueReemplazada = true
      }

      try {
        // Paso 12: reemplazo atómico (mismo volumen ⇒ rename, no copy+delete).
        await fs.rename(dbExtraida, dbActual)
        // Paso 13: reiniciar — Spring Boot corre Flyway al arrancar; el health check ya lo espera BackendManager.start().
        await deps.backend.start()
        return { ok: true, mensaje: 'Base de datos restaurada correctamente. Ganadero se reinició con los datos del respaldo.' }
      } catch (error) {
        // Paso 17: si el arranque falla, revertir automáticamente base y media a como estaban.
        await fs.rm(dbActual, { force: true })
        await fs.rename(dbRecuperacion, dbActual)
        if (mediaFueReemplazada) {
          await fs.rm(mediaActual, { recursive: true, force: true })
          if (await existeCarpeta(mediaRecuperacion)) await fs.rename(mediaRecuperacion, mediaActual)
        }
        await deps.backend.start()
        throw new Error(
          `La base restaurada no pudo iniciar (${(error as Error).message}); se restauró automáticamente la base anterior. `
          + 'El respaldo que intentaste usar podría ser incompatible con esta instalación.',
        )
      }
    } finally {
      await fs.rm(carpetaTemporal, { recursive: true, force: true })
    }
  }).then(
    (resultado) => resultado,
    (error: Error) => ({ ok: false, mensaje: error.message }),
  )
}

export { DATABASE_ENTRY }
