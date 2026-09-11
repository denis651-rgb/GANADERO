import AdmZip from 'adm-zip'
import { createHash } from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  ArchiveValidationError,
  DATABASE_ENTRY,
  MANIFEST_ENTRY,
  extractDatabaseEntry,
  readManifestFromArchive,
  validateZipEntries,
  verifyExtractedHash,
  type RespaldoManifest,
} from './archive'

function sha256(buffer: Buffer): string {
  return createHash('sha256').update(buffer).digest('hex')
}

let dir: string

beforeEach(() => {
  dir = fs.mkdtempSync(path.join(os.tmpdir(), 'ganadero-archive-test-'))
})

afterEach(() => {
  fs.rmSync(dir, { recursive: true, force: true })
})

function manifestValido(dbContent: Buffer): RespaldoManifest {
  return {
    formato: 'GANADERO_BACKUP', versionFormato: 1, versionAplicacion: '0.0.1-TEST', versionBaseDatos: '1',
    fechaCreacion: new Date().toISOString(), zonaHoraria: 'America/La_Paz', empresaId: null,
    archivoInterno: DATABASE_ENTRY, tamanoBytes: dbContent.length, hashSha256: sha256(dbContent),
  }
}

describe('readManifestFromArchive / extractDatabaseEntry / verifyExtractedHash', () => {
  it('lee el manifiesto y confirma el hash de un respaldo válido', () => {
    const dbContent = Buffer.from('contenido-de-prueba')
    const manifest = manifestValido(dbContent)
    const zip = new AdmZip()
    zip.addFile(MANIFEST_ENTRY, Buffer.from(JSON.stringify(manifest)))
    zip.addFile(DATABASE_ENTRY, dbContent)
    const zipPath = path.join(dir, 'valido.ganadero-backup')
    zip.writeZip(zipPath)

    const leido = readManifestFromArchive(zipPath)
    expect(leido.hashSha256).toBe(manifest.hashSha256)

    const destino = path.join(dir, 'extraido.db')
    extractDatabaseEntry(zipPath, destino)
    expect(verifyExtractedHash(destino, leido)).toBe(true)
  })

  it('rechaza un formato desconocido', () => {
    const dbContent = Buffer.from('x')
    const manifest = { ...manifestValido(dbContent), formato: 'OTRA_COSA' }
    const zip = new AdmZip()
    zip.addFile(MANIFEST_ENTRY, Buffer.from(JSON.stringify(manifest)))
    zip.addFile(DATABASE_ENTRY, dbContent)
    const zipPath = path.join(dir, 'formato-invalido.ganadero-backup')
    zip.writeZip(zipPath)

    expect(() => readManifestFromArchive(zipPath)).toThrow(ArchiveValidationError)
  })

  it('rechaza una versión de formato más nueva que la soportada', () => {
    const dbContent = Buffer.from('x')
    const manifest = { ...manifestValido(dbContent), versionFormato: 99 }
    const zip = new AdmZip()
    zip.addFile(MANIFEST_ENTRY, Buffer.from(JSON.stringify(manifest)))
    zip.addFile(DATABASE_ENTRY, dbContent)
    const zipPath = path.join(dir, 'version-nueva.ganadero-backup')
    zip.writeZip(zipPath)

    expect(() => readManifestFromArchive(zipPath)).toThrow(ArchiveValidationError)
  })

  it('detecta un hash que no coincide (archivo corrupto o manipulado)', () => {
    const dbContent = Buffer.from('contenido-original')
    const manifest = manifestValido(dbContent)
    const zip = new AdmZip()
    zip.addFile(MANIFEST_ENTRY, Buffer.from(JSON.stringify(manifest)))
    zip.addFile(DATABASE_ENTRY, Buffer.from('contenido-modificado-despues-del-hash'))
    const zipPath = path.join(dir, 'corrupto.ganadero-backup')
    zip.writeZip(zipPath)

    const destino = path.join(dir, 'extraido.db')
    extractDatabaseEntry(zipPath, destino)
    expect(verifyExtractedHash(destino, manifest)).toBe(false)
  })

  it('rechaza una entrada que intenta escapar del directorio de extracción (Zip Slip)', () => {
    // adm-zip normaliza "../" al escribir un zip propio, así que la validación se prueba en
    // aislamiento con una entrada fabricada — defiende contra un .ganadero-backup armado a mano
    // o con otra herramienta que sí conserve rutas de escape en el directorio central del ZIP.
    const entradaMaliciosa = { entryName: '../../evil.txt', header: { size: 4, attr: 0 } } as AdmZip.IZipEntry
    expect(() => validateZipEntries([entradaMaliciosa], dir)).toThrow(ArchiveValidationError)
  })

  it('rechaza una entrada marcada como enlace simbólico', () => {
    const modoSymlink = 0xa000 << 16
    const entradaSymlink = { entryName: 'database/ganadero.db', header: { size: 4, attr: modoSymlink } } as AdmZip.IZipEntry
    expect(() => validateZipEntries([entradaSymlink], dir)).toThrow(ArchiveValidationError)
  })

  it('rechaza un zip con demasiadas entradas', () => {
    const zip = new AdmZip()
    for (let i = 0; i < 20; i += 1) zip.addFile(`entrada-${i}.txt`, Buffer.from('x'))
    const zipPath = path.join(dir, 'demasiadas-entradas.ganadero-backup')
    zip.writeZip(zipPath)

    expect(() => readManifestFromArchive(zipPath)).toThrow(ArchiveValidationError)
  })
})
