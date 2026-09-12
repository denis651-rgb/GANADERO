import AdmZip from 'adm-zip'
import { createHash } from 'node:crypto'
import fs from 'node:fs/promises'
import fsSync from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { BackendManager } from '../backend'
import { DATABASE_ENTRY, MANIFEST_ENTRY, MEDIA_ENTRY_PREFIX, type RespaldoManifest } from './archive'
import { restaurarRespaldo, type RestoreDependencies } from './restore'

function sha256(buffer: Buffer): string {
  return createHash('sha256').update(buffer).digest('hex')
}

function crearBackup(zipPath: string, dbContent: Buffer, media: Record<string, string> = {}): void {
  const manifest: RespaldoManifest = {
    formato: 'GANADERO_BACKUP', versionFormato: 2, versionAplicacion: '0.0.1-TEST', versionBaseDatos: '1',
    fechaCreacion: new Date().toISOString(), zonaHoraria: 'America/La_Paz', empresaId: null,
    archivoInterno: DATABASE_ENTRY, tamanoBytes: dbContent.length, hashSha256: sha256(dbContent),
  }
  const zip = new AdmZip()
  zip.addFile(MANIFEST_ENTRY, Buffer.from(JSON.stringify(manifest)))
  zip.addFile(DATABASE_ENTRY, dbContent)
  for (const [nombre, contenido] of Object.entries(media)) zip.addFile(`${MEDIA_ENTRY_PREFIX}${nombre}`, Buffer.from(contenido))
  zip.writeZip(zipPath)
}

/** Simula BackendManager: solo lo que restore.ts realmente usa (dbPath, mediaPath, stop, start). */
function fakeBackend(dbPath: string, mediaPath: string, arrancaOk: () => boolean): BackendManager {
  return {
    dbPath,
    mediaPath,
    stop: async () => {},
    start: async () => {
      if (!arrancaOk()) throw new Error('boom')
      return 8080
    },
  } as unknown as BackendManager
}

let dir: string
let dbActual: string
let mediaActual: string

beforeEach(() => {
  dir = fsSync.mkdtempSync(path.join(os.tmpdir(), 'ganadero-restore-test-'))
  dbActual = path.join(dir, 'ganadero.db')
  mediaActual = path.join(dir, 'media')
})

afterEach(async () => {
  await fs.rm(dir, { recursive: true, force: true })
})

describe('restaurarRespaldo', () => {
  it('reemplaza la base y la media, dejando la anterior renombrada como recovery', async () => {
    await fs.writeFile(dbActual, 'contenido-viejo')
    await fs.mkdir(path.join(mediaActual, 'animales'), { recursive: true })
    await fs.writeFile(path.join(mediaActual, 'animales', 'vieja.jpg'), 'foto-vieja')

    const zipPath = path.join(dir, 'nuevo.ganadero-backup')
    crearBackup(zipPath, Buffer.from('contenido-nuevo'), { 'animales/nueva.jpg': 'foto-nueva' })

    const backend = fakeBackend(dbActual, mediaActual, () => true)
    const deps: RestoreDependencies = { backend, crearRespaldoPreventivo: async () => {} }

    const resultado = await restaurarRespaldo(deps, zipPath)

    expect(resultado.ok).toBe(true)
    expect(await fs.readFile(dbActual, 'utf8')).toBe('contenido-nuevo')
    expect(await fs.readFile(path.join(mediaActual, 'animales', 'nueva.jpg'), 'utf8')).toBe('foto-nueva')

    const entradas = await fs.readdir(dir)
    expect(entradas.some((n) => n.startsWith('ganadero.recovery-'))).toBe(true)
    expect(entradas.some((n) => n.startsWith('media.recovery-'))).toBe(true)
  })

  it('no toca la media existente cuando el respaldo es de un formato anterior sin media', async () => {
    await fs.writeFile(dbActual, 'contenido-viejo')
    await fs.mkdir(mediaActual, { recursive: true })
    await fs.writeFile(path.join(mediaActual, 'sigue-aqui.jpg'), 'foto-existente')

    const zipPath = path.join(dir, 'sin-media.ganadero-backup')
    crearBackup(zipPath, Buffer.from('contenido-nuevo'))

    const backend = fakeBackend(dbActual, mediaActual, () => true)
    const deps: RestoreDependencies = { backend, crearRespaldoPreventivo: async () => {} }

    const resultado = await restaurarRespaldo(deps, zipPath)

    expect(resultado.ok).toBe(true)
    expect(await fs.readFile(path.join(mediaActual, 'sigue-aqui.jpg'), 'utf8')).toBe('foto-existente')
    const entradas = await fs.readdir(dir)
    expect(entradas.some((n) => n.startsWith('media.recovery-'))).toBe(false)
  })

  it('revierte base y media si el backend no logra arrancar con los datos restaurados', async () => {
    await fs.writeFile(dbActual, 'contenido-viejo')
    await fs.mkdir(mediaActual, { recursive: true })
    await fs.writeFile(path.join(mediaActual, 'vieja.jpg'), 'foto-vieja')

    const zipPath = path.join(dir, 'nuevo.ganadero-backup')
    crearBackup(zipPath, Buffer.from('contenido-nuevo'), { 'nueva.jpg': 'foto-nueva' })

    const backend = fakeBackend(dbActual, mediaActual, () => false)
    const deps: RestoreDependencies = { backend, crearRespaldoPreventivo: async () => {} }

    const resultado = await restaurarRespaldo(deps, zipPath)

    expect(resultado.ok).toBe(false)
    expect(await fs.readFile(dbActual, 'utf8')).toBe('contenido-viejo')
    expect(await fs.readFile(path.join(mediaActual, 'vieja.jpg'), 'utf8')).toBe('foto-vieja')
    await expect(fs.access(path.join(mediaActual, 'nueva.jpg'))).rejects.toThrow()
  })
})
