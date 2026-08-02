import { useRegisterSW } from 'virtual:pwa-register/react'
import { Button } from '@/shared/components/Button'

export function PwaUpdatePrompt() {
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    offlineReady: [offlineReady, setOfflineReady],
    updateServiceWorker,
  } = useRegisterSW()

  if (!needRefresh && !offlineReady) return null

  return (
    <div className="pwa-toast" role="status">
      <div>
        <strong>{offlineReady ? 'Aplicación lista sin conexión' : 'Nueva versión disponible'}</strong>
        <span>{offlineReady ? 'Los recursos principales fueron guardados.' : 'Actualiza cuando no tengas formularios pendientes.'}</span>
      </div>
      {needRefresh && <Button onClick={() => void updateServiceWorker(true)}>Actualizar</Button>}
      <Button variant="ghost" onClick={() => { setNeedRefresh(false); setOfflineReady(false) }}>Cerrar</Button>
    </div>
  )
}
