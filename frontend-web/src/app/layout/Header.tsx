import { useQuery } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { Link } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { Breadcrumbs } from '@/app/layout/Breadcrumbs'
import { getAlertCount } from '@/features/alertas/api'

export function Header() {
  const { can } = useAuth()
  const allowed = can('ALERTA_VER')
  const count = useQuery({
    queryKey: ['alert-count'],
    queryFn: getAlertCount,
    enabled: allowed,
    refetchInterval: 60_000,
  })

  return (
    <header className="topbar">
      <Breadcrumbs />
      <div className="topbar-actions">
        {allowed && (
          <Link to="/alertas" className="notification-bell" aria-label={`${count.data?.total ?? 0} alertas no leídas`}>
            <Bell size={20} />
            {!!count.data?.total && (
              <span>{count.data.total > 99 ? '99+' : count.data.total}</span>
            )}
          </Link>
        )}
      </div>
    </header>
  )
}