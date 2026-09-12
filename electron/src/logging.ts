import { app } from 'electron'
import fs from 'node:fs'
import path from 'node:path'

const RETENCION_DIAS = 14

export function getLogsDir(): string {
  return path.join(app.getPath('userData'), 'logs')
}

function archivoDeHoy(): string {
  return path.join(getLogsDir(), `ganadero-${new Date().toISOString().slice(0, 10)}.log`)
}

function formatearLinea(nivel: string, args: unknown[]): string {
  const mensaje = args
    .map((arg) => (arg instanceof Error ? (arg.stack ?? arg.message) : typeof arg === 'string' ? arg : JSON.stringify(arg)))
    .join(' ')
  return `${new Date().toISOString()} ${nivel} ${mensaje}\n`
}

let stream: fs.WriteStream | null = null
let diaDelStream = ''

/** Reabre el archivo si cambió el día (rotación diaria simple, sin dependencias externas). */
function escribir(linea: string): void {
  const hoy = new Date().toISOString().slice(0, 10)
  if (hoy !== diaDelStream) {
    stream?.end()
    diaDelStream = hoy
    fs.mkdirSync(getLogsDir(), { recursive: true })
    stream = fs.createWriteStream(archivoDeHoy(), { flags: 'a' })
  }
  stream?.write(linea)
}

function limpiarLogsViejos(): void {
  let entradas: string[]
  try {
    entradas = fs.readdirSync(getLogsDir())
  } catch {
    return
  }
  const limite = Date.now() - RETENCION_DIAS * 24 * 60 * 60_000
  for (const nombre of entradas) {
    if (!nombre.startsWith('ganadero-') || !nombre.endsWith('.log')) continue
    const ruta = path.join(getLogsDir(), nombre)
    try {
      if (fs.statSync(ruta).mtimeMs < limite) fs.rmSync(ruta, { force: true })
    } catch {
      // el archivo pudo desaparecer entre el readdir y el stat; no es un error real
    }
  }
}

/**
 * Redirige console.log/info/warn/error a un archivo rotativo diario en userData/logs, además de
 * mantener la salida original (útil en desarrollo). Es la única forma de tener logs persistentes
 * del proceso de Electron en el .exe empaquetado, donde no hay terminal visible — hasta ahora todo
 * lo que pasaba por console.error (incluidos errores del backend, ver backend.ts) se perdía.
 */
export function initLogging(): void {
  fs.mkdirSync(getLogsDir(), { recursive: true })
  limpiarLogsViejos()

  const original = { log: console.log, info: console.info, warn: console.warn, error: console.error }
  console.log = (...args: unknown[]) => { original.log(...args); escribir(formatearLinea('INFO', args)) }
  console.info = (...args: unknown[]) => { original.info(...args); escribir(formatearLinea('INFO', args)) }
  console.warn = (...args: unknown[]) => { original.warn(...args); escribir(formatearLinea('WARN', args)) }
  console.error = (...args: unknown[]) => { original.error(...args); escribir(formatearLinea('ERROR', args)) }

  process.on('uncaughtException', (error) => console.error('[uncaughtException]', error))
  process.on('unhandledRejection', (reason) => console.error('[unhandledRejection]', reason))
}
