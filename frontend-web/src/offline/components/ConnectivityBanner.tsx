import { CloudOff } from 'lucide-react'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function ConnectivityBanner() {
  const online = useOnlineStatus()
  if (online) return null
  return <div className="offline-banner"><CloudOff size={17} />Sin conexión. Los cambios compatibles se guardarán en este dispositivo.</div>
}
