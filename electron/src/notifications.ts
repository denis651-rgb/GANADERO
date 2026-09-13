import { Notification } from 'electron'

interface AlertaPendiente {
  id: string
  titulo: string
  mensaje: string
  severidad: string
  animalId?: string
}

interface ApiEnvelope<T> {
  ok: boolean
  data: T
}

const POLL_INTERVAL_MS = 60_000

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
      for (const alerta of body.data ?? []) {
        if (Notification.isSupported()) {
          const notification = new Notification({
            title: alerta.titulo || 'Ganadero',
            body: alerta.mensaje || '',
            urgency: alerta.severidad === 'CRITICA' || alerta.severidad === 'URGENTE' ? 'critical' : 'normal',
          })
          notification.on('click', focusApp)
          notification.show()
          await fetch(`http://127.0.0.1:${port}/api/v1/alertas/${alerta.id}/marcar-enviada`, { method: 'POST' }).catch(() => undefined)
        } else {
          const error = encodeURIComponent('Notificaciones nativas no soportadas por este sistema operativo.')
          await fetch(`http://127.0.0.1:${port}/api/v1/alertas/${alerta.id}/marcar-error?error=${error}`, { method: 'POST' }).catch(() => undefined)
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
