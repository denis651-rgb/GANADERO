import { CloudOff } from 'lucide-react'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function ConnectivityBanner() {
  const online = useOnlineStatus()
  const previousOnline = useRef(online)
  const [announcement, setAnnouncement] = useState('')

  useEffect(() => {
    if (previousOnline.current !== online) {
      setAnnouncement(online
        ? 'Conexión restablecida.'
        : 'Sin conexión. Puedes continuar trabajando. Los cambios se sincronizarán cuando vuelva internet.')
      previousOnline.current = online
    }
  }, [online])

  return <>
    {!online && <div className="offline-banner" aria-hidden="true"><CloudOff size={17} aria-hidden="true" />Sin conexión. Los cambios compatibles se guardarán en este dispositivo.</div>}
    <span className="visually-hidden" role="status" aria-live="polite" aria-atomic="true">{announcement}</span>
  </>
}
import { useEffect, useRef, useState } from 'react'
