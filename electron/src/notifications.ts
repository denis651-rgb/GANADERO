import { Notification } from 'electron'

export interface AlertaPendiente {
  id: string
  tipo: string
  titulo: string
  mensaje: string
  severidad: string
  animalId?: string
  fechaVencimiento?: string | null
}

interface ApiEnvelope<T> {
  ok: boolean
  data: T
}

const POLL_INTERVAL_MS = 60_000

/**
 * Agrupa por tipo de alerta + fecha de vencimiento, no por título/mensaje: alertas como
 * VACUNA_PROXIMA arman el mensaje con el código de cada animal (ver MotorAlertasService,
 * plantilla VACUNA_PROXIMA), así que 30 animales con la misma vacunación programada tienen 30
 * mensajes distintos pero el mismo tipo y la misma fecha de vencimiento — esa es la señal real
 * de "una sola actividad para muchos animales", no el texto.
 */
export function agruparPorTipoYVencimiento(alertas: AlertaPendiente[]): AlertaPendiente[][] {
  const grupos = new Map<string, AlertaPendiente[]>()
  for (const alerta of alertas) {
    const clave = `${alerta.tipo}|||${alerta.fechaVencimiento ?? ''}`
    const grupo = grupos.get(clave)
    if (grupo) grupo.push(alerta)
    else grupos.set(clave, [alerta])
  }
  return Array.from(grupos.values())
}

/** Consulta las alertas pendientes de notificar y dispara notificaciones nativas del OS. */
export function startNotificationPolling(getPort: () => number, focusApp: () => void): () => void {
  let stopped = false

  async function tick() {
    const port = getPort()
    if (!port) return
    try {
      const response = await fetch(`http://127.0.0.1:${port}/api/v1/alertas/pendientes-notificar?limite=20`)
      if (!response.ok) return
      const body = (await response.json()) as ApiEnvelope<AlertaPendiente[]>
      // N animales con la misma actividad (mismo tipo y vencimiento) generan N alertas que
      // llegan juntas en un mismo tick: agruparlas evita mostrar N notificaciones/sonidos
      // seguidos por una sola actividad o movimiento.
      for (const grupo of agruparPorTipoYVencimiento(body.data ?? [])) {
        const primera = grupo[0]
        if (Notification.isSupported()) {
          const notification = new Notification({
            title: primera.titulo || 'Ganadero',
            body: grupo.length > 1
              ? `${grupo.length} animales — ${primera.mensaje || ''}`.trim()
              : (primera.mensaje || ''),
            urgency: grupo.some((a) => a.severidad === 'CRITICA' || a.severidad === 'URGENTE') ? 'critical' : 'normal',
          })
          notification.on('click', focusApp)
          notification.show()
          await Promise.all(grupo.map((alerta) =>
            fetch(`http://127.0.0.1:${port}/api/v1/alertas/${alerta.id}/marcar-enviada`, { method: 'POST' }).catch(() => undefined)))
        } else {
          const error = encodeURIComponent('Notificaciones nativas no soportadas por este sistema operativo.')
          await Promise.all(grupo.map((alerta) =>
            fetch(`http://127.0.0.1:${port}/api/v1/alertas/${alerta.id}/marcar-error?error=${error}`, { method: 'POST' }).catch(() => undefined)))
        }
      }
    } catch (error) {
      console.error('[notifications] error consultando alertas pendientes', error)
    }
  }

  const timer = setInterval(() => { void tick() }, POLL_INTERVAL_MS)
  void tick()

  return () => {
    if (stopped) return
    stopped = true
    clearInterval(timer)
  }
}
