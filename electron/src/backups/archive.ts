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
export const MEDIA_ENTRY_PREFIX = 'media/'
// Formato v2: además de manifest.json + database/ganadero.db, empaqueta media/ (fotos de
// animales) — de ahí el límite de entradas mucho más alto que en v1 (dos entradas fijas).
export const MAX_ENTRIES = 20_000
const MAX_TOTAL_BYTES = 8 * 1024 * 1024 * 1024 // 8 GB: base SQLite + fotos de animales.
const FORMATO_ESPERADO = 'GANADERO_BACKUP'
const VERSION_FORMATO_SOPORTADA = 2

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

/**
 * Resuelve dónde debe escribirse una entrada relativa dentro de destAbs, o lanza si escapa
 * (Zip Slip). Separado de extractMediaEntries para poder probarlo sin pasar por un ZIP real:
 * adm-zip normaliza "../" al escribir un zip propio (ver comentario en el test de Zip Slip más
 * abajo), así que un caso malicioso solo se puede ejercer fabricando la ruta directamente.
 */
export function resolverDestinoMedia(destAbs: string, relativo: string): string {
  const destino = path.resolve(destAbs, relativo)
  if (destino !== destAbs && !destino.startsWith(destAbs + path.sep)) {
    throw new ArchiveValidationError(`Ruta de entrada de media fuera del destino permitido: ${relativo}`)
  }
  return destino
}

/**
 * Extrae las entradas media/** a destDir (sin el prefijo "media/"), preservando subcarpetas.
 * Devuelve la cantidad de archivos extraídos — 0 si el respaldo es de un formato anterior sin
 * fotos, en cuyo caso el llamador no debe tocar la carpeta de media existente.
 */
export function extractMediaEntries(zipPath: string, destDir: string): number {
  const zip = new AdmZip(zipPath)
  const entries = validateZipEntries(zip.getEntries(), path.dirname(zipPath))
  const destAbs = path.resolve(destDir)
  let extraidos = 0
  for (const entry of entries) {
    if (entry.isDirectory || !entry.entryName.startsWith(MEDIA_ENTRY_PREFIX)) continue
    const relativo = entry.entryName.slice(MEDIA_ENTRY_PREFIX.length)
    const destino = resolverDestinoMedia(destAbs, relativo)
    fs.mkdirSync(path.dirname(destino), { recursive: true })
    fs.writeFileSync(destino, zip.readFile(entry) ?? Buffer.alloc(0))
    extraidos += 1
  }
  return extraidos
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
