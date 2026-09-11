import { app } from 'electron'
import fs from 'node:fs/promises'
import path from 'node:path'

export type Frecuencia = 'DIARIA' | 'SEMANAL' | 'MENSUAL'

export interface UltimoRespaldoInfo {
  fecha: string
  periodoKey: string
}

export interface BackupSettings {
  version: 1
  automatico: boolean
  frecuencia: Frecuencia
  hora: string
  destinoLocal: string
  destinoExterno: string | null
  retencionDiarios: number
  retencionSemanales: number
  retencionMensuales: number
  ultimoRespaldo: UltimoRespaldoInfo | null
}

const HORA_VALIDA = /^([01]\d|2[0-3]):([0-5]\d)$/

function settingsPath(): string {
  return path.join(app.getPath('userData'), 'backup-settings.json')
}

function destinoLocalPredeterminado(): string {
  return path.join(app.getPath('userData'), 'backups')
}

function defaults(): BackupSettings {
  return {
    version: 1,
    automatico: false,
    frecuencia: 'DIARIA',
    hora: '20:00',
    destinoLocal: destinoLocalPredeterminado(),
    destinoExterno: null,
    retencionDiarios: 7,
    retencionSemanales: 4,
    retencionMensuales: 12,
    ultimoRespaldo: null,
  }
}

export async function loadSettings(): Promise<BackupSettings> {
  try {
    const raw = await fs.readFile(settingsPath(), 'utf8')
    const parsed = JSON.parse(raw) as Partial<BackupSettings>
    return { ...defaults(), ...parsed, destinoLocal: destinoLocalPredeterminado() }
  } catch {
    return defaults()
  }
}

/** No es editable por el usuario (el enunciado solo pide selector para la carpeta externa). */
export function esRaizDeUnidad(rutaAbsoluta: string): boolean {
  return path.parse(rutaAbsoluta).root === rutaAbsoluta
}

export function validarSettings(input: BackupSettings): string | null {
  if (!HORA_VALIDA.test(input.hora)) return 'La hora debe tener el formato HH:mm (24 horas).'
  if (input.retencionDiarios < 0 || input.retencionSemanales < 0 || input.retencionMensuales < 0) {
    return 'Los valores de retención no pueden ser negativos.'
  }
  if (!path.isAbsolute(input.destinoLocal)) return 'La carpeta local debe ser una ruta absoluta.'
  if (input.destinoExterno) {
    if (!path.isAbsolute(input.destinoExterno)) return 'La carpeta externa debe ser una ruta absoluta.'
    if (esRaizDeUnidad(path.resolve(input.destinoExterno))) return 'La carpeta externa no puede ser la raíz de una unidad.'
  }
  return null
}

/** Escritura atómica: archivo temporal + fsync + rename, igual que electron/src/google-oauth.ts. */
export async function saveSettings(input: BackupSettings): Promise<BackupSettings> {
  const error = validarSettings(input)
  if (error) throw new Error(error)
  const target = settingsPath()
  const temporary = `${target}.tmp`
  await fs.mkdir(path.dirname(target), { recursive: true })
  const payload = { ...input, destinoLocal: destinoLocalPredeterminado() }
  const handle = await fs.open(temporary, 'w')
  try {
    await handle.writeFile(JSON.stringify(payload, null, 2), 'utf8')
    await handle.sync()
  } finally {
    await handle.close()
  }
  await fs.rename(temporary, target)
  return payload
}

export async function marcarUltimoRespaldo(info: UltimoRespaldoInfo): Promise<BackupSettings> {
  const actual = await loadSettings()
  return saveSettings({ ...actual, ultimoRespaldo: info })
}
