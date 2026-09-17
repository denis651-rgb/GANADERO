import { useEffect } from 'react'
import type { BackendStatus } from '@/shared/api/http'
import { useToast } from '@/shared/toast/useToast'

/**
 * El backend embebido puede morir después de haber arrancado bien (crash, o alguien mata el
 * proceso java a mano). Electron reintenta arrancarlo solo (ver electron/src/main.ts) y avisa acá
 * por IPC — sin esto, la app se queda mostrando errores de conexión sin explicar por qué.
 */
export function BackendStatusListener() {
  const { showToast } = useToast()

  useEffect(() => {
    if (!window.ganadero?.onBackendStatus) return
    return window.ganadero.onBackendStatus((status: BackendStatus) => {
      if (status.state === 'reconnecting') {
        showToast(`Se perdió la conexión con el backend local. Reintentando… (${status.attempt}/${status.maxAttempts})`, 'info')
      } else if (status.state === 'restored') {
        showToast('Conexión con el backend restablecida.')
      } else {
        showToast('No se pudo reconectar con el backend local. Cierra Ganadero desde el Administrador de tareas y vuelve a abrirlo.', 'danger')
      }
    })
  }, [showToast])

  return null
}
