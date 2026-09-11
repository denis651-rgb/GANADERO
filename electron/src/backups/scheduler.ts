import fs from 'node:fs/promises'
import path from 'node:path'
import { calcularSobrantes } from './retention'
import { loadSettings, marcarUltimoRespaldo, type Frecuencia } from './settings'
import type { BackupManager } from './manager'

const POLL_MS = 5 * 60_000
const BOLIVIA_TZ = 'America/La_Paz'
const PART_MAX_AGE_MS = 24 * 60 * 60_000

function partesBolivia(now: Date) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: BOLIVIA_TZ, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).formatToParts(now)
  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? '00'
  return { year: get('year'), month: get('month'), day: get('day'), hour: get('hour') === '24' ? '00' : get('hour'), minute: get('minute') }
}

function fechaBoliviaISO(now: Date): string {
  const { year, month, day } = partesBolivia(now)
  return `${year}-${month}-${day}`
}

function horaBoliviaHHmm(now: Date): string {
  const { hour, minute } = partesBolivia(now)
  return `${hour}:${minute}`
}

/** Mismo cálculo de semana ISO-8601 que retention.ts, anclado a la fecha calendario de Bolivia. */
function semanaIsoBolivia(now: Date): string {
  const [y, m, d] = fechaBoliviaISO(now).split('-').map(Number)
  const utcNoon = new Date(Date.UTC(y, m - 1, d, 12))
  const dia = (utcNoon.getUTCDay() + 6) % 7
  const jueves = new Date(utcNoon)
  jueves.setUTCDate(utcNoon.getUTCDate() - dia + 3)
  const inicioAnio = new Date(Date.UTC(jueves.getUTCFullYear(), 0, 1))
  const semana = Math.floor((jueves.getTime() - inicioAnio.getTime()) / (7 * 86_400_000)) + 1
  return `${jueves.getUTCFullYear()}-W${String(semana).padStart(2, '0')}`
}

function mesBoliviaISO(now: Date): string {
  const { year, month } = partesBolivia(now)
  return `${year}-${month}`
}

/** Claves idempotentes pedidas: BACKUP:DIARIA:2026-09-06, BACKUP:SEMANAL:2026-W36, BACKUP:MENSUAL:2026-09. */
export function periodoKeyActual(frecuencia: Frecuencia, now: Date = new Date()): string {
  if (frecuencia === 'SEMANAL') return `BACKUP:SEMANAL:${semanaIsoBolivia(now)}`
  if (frecuencia === 'MENSUAL') return `BACKUP:MENSUAL:${mesBoliviaISO(now)}`
  return `BACKUP:DIARIA:${fechaBoliviaISO(now)}`
}

export async function limpiarPartsHuerfanos(carpeta: string): Promise<void> {
  let entradas: string[]
  try {
    entradas = await fs.readdir(carpeta)
  } catch {
    return
  }
  const ahora = Date.now()
  for (const nombre of entradas) {
    if (!nombre.endsWith('.part')) continue
    const ruta = path.join(carpeta, nombre)
    try {
      const info = await fs.stat(ruta)
      if (ahora - info.mtimeMs > PART_MAX_AGE_MS) await fs.rm(ruta, { force: true })
    } catch {
      // el archivo pudo desaparecer entre el readdir y el stat; no es un error real
    }
  }
}

export interface BackupSchedulerHandle {
  stop(): void
  runNow(): Promise<void>
}

/**
 * Revisa cada 5 minutos (mismo patrón que google-calendar-sync.ts) si corresponde crear un
 * respaldo programado, con una pasada inmediata al iniciar para cubrir el caso de que la PC
 * haya estado apagada a la hora configurada. Usa `manager`'s lock interno (compartido con la
 * creación/restauración manual) para nunca solaparse con otra operación de respaldo.
 */
export function startBackupScheduler(manager: BackupManager): BackupSchedulerHandle {
  let enCurso: Promise<void> | null = null

  async function tick(): Promise<void> {
    if (enCurso) return enCurso
    enCurso = ejecutar().finally(() => {
      enCurso = null
    })
    return enCurso
  }

  async function ejecutar(): Promise<void> {
    const settings = await loadSettings()
    await limpiarPartsHuerfanos(settings.destinoLocal)
    if (!settings.automatico) return
    const claveActual = periodoKeyActual(settings.frecuencia)
    if (settings.ultimoRespaldo?.periodoKey === claveActual) return
    if (horaBoliviaHHmm(new Date()) < settings.hora) return

    try {
      await manager.createNow()
      await marcarUltimoRespaldo({ fecha: new Date().toISOString(), periodoKey: claveActual })
      await aplicarRetencion(manager)
    } catch (error) {
      // Se reintenta en el próximo tick: la clave de período no se marca hasta que termina OK.
      console.error('[backups] falló la creación programada:', error)
    }
  }

  const interval = setInterval(() => { void tick() }, POLL_MS)
  void tick()

  return { stop: () => clearInterval(interval), runNow: tick }
}

export async function aplicarRetencion(manager: BackupManager): Promise<void> {
  const settings = await loadSettings()
  const lista = await manager.list()
  const sobrantes = calcularSobrantes(lista, settings)
  for (const nombre of sobrantes) {
    try {
      await manager.deleteBackup(nombre)
    } catch (error) {
      console.error(`[backups] no se pudo aplicar retención sobre ${nombre}:`, error)
    }
  }
}
