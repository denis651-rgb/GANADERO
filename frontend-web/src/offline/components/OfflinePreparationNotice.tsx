import { useEffect, useRef, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
import { db } from '@/offline/db'
import { prepareOfflineData } from '@/offline/offlinePreparation'

export function OfflinePreparationNotice() {
  const online = useOnlineStatus()
  const marker = useLiveQuery(() => db.estadoSincronizacion.get('offlineBootstrapCompletedAt'))
  const [preparing, setPreparing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const attempted = useRef(false)

  function startPreparation() {
    attempted.current = true
    setPreparing(true)
    setError(null)
    void prepareOfflineData({ force: true })
      .catch((reason) => setError(reason instanceof Error ? reason.message : 'No se pudieron preparar los datos offline.'))
      .finally(() => setPreparing(false))
  }

  useEffect(() => {
    if (!online || marker || attempted.current) return
    startPreparation()
  }, [marker, online])

  if (marker) return null
  if (!online) {
    return <Alert tone="info" title="Este dispositivo todavía no está preparado para trabajar sin conexión">Conéctalo a internet y abre nuevamente GANADERO para descargar razas, categorías, propiedades y potreros.</Alert>
  }
  if (!error) return null
  return <Alert tone="danger" title="No se completó la preparación offline"><span>{error}</span> <Button variant="ghost" loading={preparing} onClick={startPreparation}>Reintentar</Button></Alert>
}
