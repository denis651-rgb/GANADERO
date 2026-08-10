import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router'
import { Beef, House, LayoutGrid, MapPinned, RefreshCw, X } from 'lucide-react'
import { appModules, MODULE_STATUS_LABEL } from '@/app/modules'
import { useAuth } from '@/auth/auth-context'
import { cn } from '@/shared/utils/cn'

const pinned = [
  { path: '/', label: 'Inicio', icon: House, end: true },
  { path: '/animales', label: 'Animales', icon: Beef },
  { path: '/potreros', label: 'Campo', icon: MapPinned },
  { path: '/sincronizacion', label: 'Sincronizar', icon: RefreshCw },
]

export function MobileNav() {
  const { can } = useAuth()
  const location = useLocation()
  const [open, setOpen] = useState(false)
  const [lastPath, setLastPath] = useState(location.pathname)
  if (lastPath !== location.pathname) {
    setLastPath(location.pathname)
    setOpen(false)
  }
  const isPinnedRoute = pinned.some((item) => (
    item.end ? location.pathname === item.path : location.pathname.startsWith(item.path)
  ))

  useEffect(() => {
    if (!open) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open])

  const modules = appModules.filter((module) => !module.permission || can(module.permission))

  return (
    <>
      <nav className="mobile-nav" aria-label="Navegación móvil">
        {pinned.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) => cn('mobile-nav-item', isActive && 'active')}
            >
              <Icon size={21} />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
        <button
          type="button"
          className={cn('mobile-nav-item', (open || !isPinnedRoute) && 'active')}
          onClick={() => setOpen((value) => !value)}
          aria-expanded={open}
          aria-haspopup="dialog"
        >
          <LayoutGrid size={21} />
          <span>Más</span>
        </button>
      </nav>

      {open && (
        <div className="mobile-drawer-overlay" onClick={() => setOpen(false)}>
          <div
            className="mobile-drawer"
            role="dialog"
            aria-modal="true"
            aria-label="Todos los módulos"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mobile-drawer-header">
              <strong>Todos los módulos</strong>
              <button type="button" className="icon-button" onClick={() => setOpen(false)} aria-label="Cerrar menú">
                <X size={19} />
              </button>
            </div>
            <nav className="mobile-drawer-nav" aria-label="Módulos">
              {modules.map((module) => {
                const Icon = module.icon
                return (
                  <NavLink
                    key={module.key}
                    to={module.path}
                    end={module.path === '/'}
                    className={({ isActive }) => cn('mobile-drawer-item', isActive && 'active')}
                  >
                    <Icon size={19} aria-hidden="true" />
                    <span>{module.label}</span>
                    {module.status !== 'LISTO' && (
                      <small className={`module-status module-status-${module.status.toLowerCase()}`}>{MODULE_STATUS_LABEL[module.status]}</small>
                    )}
                  </NavLink>
                )
              })}
            </nav>
          </div>
        </div>
      )}
    </>
  )
}
