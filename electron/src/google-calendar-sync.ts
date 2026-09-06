import { randomUUID } from 'node:crypto'
import type { GoogleOAuthManager } from './google-oauth'

const GOOGLE_API = 'https://www.googleapis.com/calendar/v3'
const POLL_MS = 60_000

interface Envelope<T> { ok: boolean; data: T }
interface Config {
  cuentaEmail?: string
  calendarioExternoId?: string
  calendarioNombre: string
  zonaHoraria: string
  sincronizacionAutomatica: boolean
  estado: string
}
interface Job {
  id: string
  ocurrenciaId: string
  operacion: 'CREAR' | 'ACTUALIZAR' | 'CANCELAR' | 'ELIMINAR'
  claveIdempotencia: string
  payload?: string
}
interface EventPayload {
  ocurrenciaId: string
  fechaPrevista: string
  nombreActividad?: string
  tipoActividad?: string
  producto?: string
  dosis?: number
  unidadDosis?: string
  via?: string
  lugarAplicacion?: string
  instrucciones?: string
  propiedad?: string
  potrero?: string
  lote?: string
  animales?: number
  diasAlerta?: number
  horariosAviso?: string[]
  eventoExternoId?: string
  etag?: string
}
interface GoogleEvent { id: string; etag?: string; htmlLink?: string; updated?: string }

export interface GoogleCalendarSyncResult {
  generated: number
  claimed: number
  completed: number
  failed: number
  skipped?: 'NOT_CONNECTED' | 'AUTOMATIC_DISABLED'
}
export interface GoogleCalendarSyncHandle { stop: () => void; syncNow: () => Promise<GoogleCalendarSyncResult> }

export function startGoogleCalendarSync(getPort: () => number, oauth: GoogleOAuthManager): GoogleCalendarSyncHandle {
  const deviceId = `ganadero-desktop-${randomUUID()}`
  let stopped = false
  let activeSync: Promise<GoogleCalendarSyncResult> | null = null

  async function run(manual: boolean): Promise<GoogleCalendarSyncResult> {
    if (stopped) return { generated: 0, claimed: 0, completed: 0, failed: 0 }
    try {
      const status = await oauth.status()
      if (!status.connected) return { generated: 0, claimed: 0, completed: 0, failed: 0, skipped: 'NOT_CONNECTED' }
      let config = await backend<Config>(getPort(), '/api/v1/integraciones/calendario')
      if (!manual && !config.sincronizacionAutomatica) {
        return { generated: 0, claimed: 0, completed: 0, failed: 0, skipped: 'AUTOMATIC_DISABLED' }
      }
      if (!config.calendarioExternoId) config = await ensureCalendar(getPort(), oauth, config, status.email)
      const preparation = await backend<{ eventosGenerados: number }>(getPort(), '/api/v1/integraciones/calendario/electron/sincronizar/preparar', { method: 'POST' })
      const generated = preparation.eventosGenerados
      let claimed = 0
      let completed = 0
      let failed = 0
      do {
        const jobs = await backend<Job[]>(getPort(), '/api/v1/integraciones/calendario/electron/cola/reclamar', {
          method: 'POST', body: JSON.stringify({ dispositivoId: deviceId, limite: 20, manual }),
        })
        claimed += jobs.length
        for (const job of jobs) {
          if (await processJob(getPort(), oauth, config, job)) completed++
          else failed++
        }
        if (!manual || jobs.length < 20) break
      } while (claimed < 200)
      return { generated, claimed, completed, failed }
    } catch (error) {
      if (!manual) {
        console.error('[google-calendar] sincronización no disponible', error)
        return { generated: 0, claimed: 0, completed: 0, failed: 1 }
      }
      throw error
    }
  }

  function begin(manual: boolean): Promise<GoogleCalendarSyncResult> {
    const task = run(manual)
    activeSync = task
    void task.finally(() => { if (activeSync === task) activeSync = null })
    return task
  }

  function tick(): Promise<GoogleCalendarSyncResult> {
    return activeSync ?? begin(false)
  }

  async function syncNow(): Promise<GoogleCalendarSyncResult> {
    if (activeSync) await activeSync
    return begin(true)
  }

  const timer = setInterval(() => { void tick() }, POLL_MS)
  void tick()
  return { stop: () => { stopped = true; clearInterval(timer) }, syncNow }
}

async function ensureCalendar(port: number, oauth: GoogleOAuthManager, config: Config, email?: string): Promise<Config> {
  const calendar = await google<{ id: string }>(oauth, '/calendars', {
    method: 'POST', body: JSON.stringify({ summary: config.calendarioNombre, timeZone: config.zonaHoraria }),
  })
  return backend<Config>(port, '/api/v1/integraciones/calendario/electron/conexion/confirmar', {
    method: 'POST', body: JSON.stringify({ cuentaEmail: email, calendarioExternoId: calendar.id }),
  })
}

async function processJob(port: number, oauth: GoogleOAuthManager, config: Config, job: Job): Promise<boolean> {
  try {
    if (!config.calendarioExternoId) throw new Error('No existe un calendario externo configurado.')
    const payload = JSON.parse(job.payload || '{}') as EventPayload
    const eventId = payload.eventoExternoId || job.ocurrenciaId.replaceAll('-', '').toLowerCase()
    let result: GoogleEvent
    if (job.operacion === 'CANCELAR' || job.operacion === 'ELIMINAR') {
      await google<void>(oauth, `/calendars/${encodeURIComponent(config.calendarioExternoId)}/events/${eventId}`, { method: 'DELETE' }, [404, 410])
      result = { id: eventId, updated: new Date().toISOString() }
    } else {
      const request = eventRequest(payload, config.zonaHoraria, eventId, job.claveIdempotencia)
      if (job.operacion === 'ACTUALIZAR' || payload.eventoExternoId) {
        result = await google<GoogleEvent>(oauth,
          `/calendars/${encodeURIComponent(config.calendarioExternoId)}/events/${eventId}`,
          { method: 'PATCH', headers: payload.etag ? { 'If-Match': payload.etag } : undefined, body: JSON.stringify(request) })
      } else {
        try {
          result = await google<GoogleEvent>(oauth,
            `/calendars/${encodeURIComponent(config.calendarioExternoId)}/events?sendUpdates=none`,
            { method: 'POST', body: JSON.stringify(request) })
        } catch (error) {
          if (!(error instanceof GoogleApiError) || error.status !== 409) throw error
          result = await google<GoogleEvent>(oauth,
            `/calendars/${encodeURIComponent(config.calendarioExternoId)}/events/${eventId}`)
        }
      }
    }
    await backend(port, `/api/v1/integraciones/calendario/electron/cola/${job.id}/completar`, {
      method: 'POST', body: JSON.stringify({ eventoExternoId: result.id, etag: result.etag,
        enlaceExterno: result.htmlLink, fechaActualizacionExterna: result.updated }),
    })
    return true
  } catch (error) {
    const status = error instanceof GoogleApiError ? error.status : 0
    const reintentable = status === 0 || status === 408 || status === 429 || status >= 500
    await backend(port, `/api/v1/integraciones/calendario/electron/cola/${job.id}/fallar`, {
      method: 'POST', body: JSON.stringify({ codigo: status ? `GOOGLE_${status}` : 'SIN_CONEXION',
        mensaje: error instanceof Error ? error.message : String(error), reintentable }),
    }).catch(reportError => console.error('[google-calendar] no se pudo reportar el fallo', reportError))
    return false
  }
}

function eventRequest(p: EventPayload, timeZone: string, eventId: string, idempotencyKey: string) {
  const start = new Date(p.fechaPrevista)
  const end = new Date(start.getTime() + 60 * 60_000)
  const details = [
    p.tipoActividad && `Tipo: ${p.tipoActividad}`,
    p.producto && `Medicamento/producto: ${p.producto}`,
    p.dosis && `Dosis: ${p.dosis} ${p.unidadDosis ?? ''}`.trim(),
    p.via && `Vía: ${p.via}`,
    p.lugarAplicacion && `Lugar de aplicación: ${p.lugarAplicacion}`,
    p.animales != null && `Animales: ${p.animales}`,
    p.instrucciones && `Instrucciones: ${p.instrucciones}`,
  ].filter(Boolean).join('\n')
  return {
    id: eventId,
    summary: p.nombreActividad || 'Actividad sanitaria',
    description: details,
    location: [p.propiedad, p.potrero, p.lote].filter(Boolean).join(' · '),
    start: { dateTime: start.toISOString(), timeZone },
    end: { dateTime: end.toISOString(), timeZone },
    reminders: { useDefault: false, overrides: reminderMinutes(p, start, timeZone).map(minutes => ({ method: 'popup', minutes })) },
    extendedProperties: { private: { ganaderoOcurrenciaId: p.ocurrenciaId, ganaderoIdempotencia: idempotencyKey } },
  }
}

function reminderMinutes(p: EventPayload, start: Date, timeZone: string): number[] {
  const days = Math.max(0, p.diasAlerta ?? 1)
  const hours = p.horariosAviso?.length ? p.horariosAviso : ['06:00', '07:00', '08:00']
  const eventParts = partsInZone(start, timeZone)
  const reminders = hours.map(value => {
    const [hour, minute] = value.split(':').map(Number)
    const reminderUtc = zonedToUtc(eventParts.year, eventParts.month, eventParts.day - days, hour, minute, timeZone)
    return Math.max(0, Math.round((start.getTime() - reminderUtc.getTime()) / 60_000))
  }).filter(value => value <= 40_320)
  return [...new Set(reminders)].sort((a, b) => b - a).slice(0, 5)
}

function partsInZone(date: Date, timeZone: string): Record<'year'|'month'|'day', number> {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: 'numeric', day: 'numeric' }).formatToParts(date)
  const get = (type: string) => Number(parts.find(part => part.type === type)?.value)
  return { year: get('year'), month: get('month'), day: get('day') }
}

function zonedToUtc(year: number, month: number, day: number, hour: number, minute: number, timeZone: string): Date {
  const guess = new Date(Date.UTC(year, month - 1, day, hour, minute))
  const shown = new Intl.DateTimeFormat('en-CA', { timeZone, hour12: false, year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric' }).formatToParts(guess)
  const get = (type: string) => Number(shown.find(part => part.type === type)?.value)
  const represented = Date.UTC(get('year'), get('month') - 1, get('day'), get('hour') % 24, get('minute'))
  return new Date(guess.getTime() + (guess.getTime() - represented))
}

class GoogleApiError extends Error { constructor(readonly status: number, message: string) { super(message) } }

async function google<T>(oauth: GoogleOAuthManager, path: string, init: RequestInit = {}, accepted: number[] = []): Promise<T> {
  const token = await oauth.accessToken()
  const response = await fetch(`${GOOGLE_API}${path}`, { ...init, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...init.headers } })
  if (accepted.includes(response.status)) return undefined as T
  if (!response.ok) {
    const text = await response.text()
    throw new GoogleApiError(response.status, `Google Calendar respondió ${response.status}: ${text.slice(0, 800)}`)
  }
  return response.status === 204 ? undefined as T : await response.json() as T
}

async function backend<T>(port: number, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`http://127.0.0.1:${port}${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...init?.headers } })
  if (!response.ok) throw new Error(`Backend local respondió ${response.status} en ${path}.`)
  return ((await response.json()) as Envelope<T>).data
}
