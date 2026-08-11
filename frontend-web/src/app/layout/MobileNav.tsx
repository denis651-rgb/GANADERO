import { useState } from 'react'
import { NavLink, useLocation } from 'react-router'
import { Beef, House, LayoutGrid, MapPinned, RefreshCw } from 'lucide-react'
import { appModules, MODULE_STATUS_LABEL } from '@/app/modules'
import { useAuth } from '@/auth/auth-context'
import { cn } from '@/shared/utils/cn'
import { Modal } from '@/shared/components/Modal'

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
  const isPinnedRoute = pinned.some((item) => (
    item.end ? location.pathname === item.path : location.pathname.startsWith(item.path)
  ))

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
              <Icon size={21} aria-hidden="true" />
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
          <LayoutGrid size={21} aria-hidden="true" />
          <span>Más</span>
        </button>
      </nav>

      <Modal open={open} title="Todos los módulos" description="Selecciona un módulo de GANADERO." onClose={() => setOpen(false)} variant="drawer">
            <nav className="mobile-drawer-nav" aria-label="Módulos">
              {modules.map((module) => {
                const Icon = module.icon
                return (
                  <NavLink
                    key={module.key}
                    to={module.path}
                    end={module.path === '/'}
                    onClick={() => setOpen(false)}
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
      </Modal>
    </>
  )
}
