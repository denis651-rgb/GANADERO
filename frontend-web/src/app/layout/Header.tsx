import { CloudOff, RefreshCw } from 'lucide-react'
import { useLocation } from 'react-router'
import { appModules } from '@/app/modules'
import { PendingOperationsBadge } from '@/offline/components/PendingOperationsBadge'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function Header() {
  const location = useLocation()
  const online = useOnlineStatus()
  const current = appModules.find((item) => item.path === location.pathname)
    ?? appModules.find((item) => item.path !== '/' && location.pathname.startsWith(item.path))

  return (
    <header className="topbar">
      <div>
        <span className="eyebrow">GANADERO</span>
        <div className="topbar-title">{current?.label ?? 'Panel principal'}</div>
      </div>
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
