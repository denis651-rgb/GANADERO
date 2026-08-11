import { CloudOff, RefreshCw } from 'lucide-react'
import { PendingOperationsBadge } from '@/offline/components/PendingOperationsBadge'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function Header() {
  const online = useOnlineStatus()

  return (
    <header className="topbar">
      <div className="topbar-brand" aria-label="GANADERO">GANADERO</div>
      <div className="topbar-actions">
        <span className={online ? 'connection-pill online' : 'connection-pill offline'}>
          {online ? <RefreshCw size={15} aria-hidden="true" /> : <CloudOff size={15} aria-hidden="true" />}
          {online ? 'En línea' : 'Sin conexión'}
        </span>
        <PendingOperationsBadge />
      </div>
    </header>
  )
}
