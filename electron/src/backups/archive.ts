import AdmZip from 'adm-zip'
import { createHash } from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

export interface RespaldoManifest {
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

export const MANIFEST_ENTRY = 'manifest.json'
export const DATABASE_ENTRY = 'database/ganadero.db'
const MAX_ENTRIES = 10
const MAX_TOTAL_BYTES = 5 * 1024 * 1024 * 1024 // 5 GB: generosa para una base SQLite de una finca.
const FORMATO_ESPERADO = 'GANADERO_BACKUP'
const VERSION_FORMATO_SOPORTADA = 1

export class ArchiveValidationError extends Error {}

/**
 * Guardas contra Zip Slip / zip bombs antes de tocar el disco: cuenta y tamaño total de entradas,
 * cada ruta debe resolver DENTRO del directorio de extracción, y se rechaza cualquier symlink
 * (bit de símlink Unix en los atributos externos de la entrada).
 */
export function validateZipEntries(entries: AdmZip.IZipEntry[], destDir: string): AdmZip.IZipEntry[] {
  if (entries.length === 0 || entries.length > MAX_ENTRIES) {
    throw new ArchiveValidationError('El archivo de respaldo tiene una cantidad de entradas inesperada.')
  }
  let total = 0
  const destAbs = path.resolve(destDir)
  for (const entry of entries) {
    total += entry.header.size
    if (total > MAX_TOTAL_BYTES) throw new ArchiveValidationError('El respaldo excede el tamaño máximo permitido.')

    const resolved = path.resolve(destAbs, entry.entryName)
    if (resolved !== destAbs && !resolved.startsWith(destAbs + path.sep)) {
      throw new ArchiveValidationError(`Ruta de entrada fuera del destino permitido: ${entry.entryName}`)
    }
    // Bits altos de header.attr en un ZIP de Unix codifican el modo de archivo; 0xA000 = symlink.
    const modoUnix = (entry.header.attr >>> 16) & 0xffff
    const esSymlink = (modoUnix & 0xa000) === 0xa000
    if (esSymlink) throw new ArchiveValidationError(`El respaldo contiene un enlace simbólico: ${entry.entryName}`)
  }
  return entries
}

export function readManifestFromArchive(zipPath: string): RespaldoManifest {
  const zip = new AdmZip(zipPath)
  validateZipEntries(zip.getEntries(), path.dirname(zipPath))
  const entry = zip.getEntry(MANIFEST_ENTRY)
  if (!entry) throw new ArchiveValidationError('El respaldo no contiene manifest.json.')
  const manifest = JSON.parse(zip.readAsText(entry)) as RespaldoManifest
  if (manifest.formato !== FORMATO_ESPERADO) {
    throw new ArchiveValidationError(`Formato de respaldo no reconocido: ${manifest.formato}`)
  }
  if (manifest.versionFormato > VERSION_FORMATO_SOPORTADA) {
    throw new ArchiveValidationError(`Este respaldo usa una versión de formato más nueva (${manifest.versionFormato}) que la soportada.`)
  }
  return manifest
}

/** Extrae únicamente database/ganadero.db a destPath, validando la entrada primero. */
export function extractDatabaseEntry(zipPath: string, destPath: string): void {
  const zip = new AdmZip(zipPath)
  const destDir = path.dirname(destPath)
  validateZipEntries(zip.getEntries(), destDir)
  const entry = zip.getEntry(DATABASE_ENTRY)
  if (!entry) throw new ArchiveValidationError('El respaldo no contiene database/ganadero.db.')
  fs.writeFileSync(destPath, zip.readFile(entry) ?? Buffer.alloc(0))
}

export function computeSha256(filePath: string): string {
  const hash = createHash('sha256')
  hash.update(fs.readFileSync(filePath))
  return hash.digest('hex')
}

/**
 * Verificación de integridad sin motor SQLite en Electron: el backend ya corrió un PRAGMA
 * integrity_check real al crear el respaldo y firmó el resultado con el hash del manifiesto: si
 * el .db extraído produce el mismo hash, son bytes idénticos a los que pasaron esa verificación.
 */
export function verifyExtractedHash(extractedDbPath: string, manifest: RespaldoManifest): boolean {
  return computeSha256(extractedDbPath).toLowerCase() === manifest.hashSha256.toLowerCase()
}
