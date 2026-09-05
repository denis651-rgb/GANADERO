import { useState } from 'react'
import { NavLink } from 'react-router'
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { appModules, MODULE_STATUS_LABEL } from '@/app/modules'
import { useAuth } from '@/auth/auth-context'
import { cn } from '@/shared/utils/cn'

const COLLAPSED_STORAGE_KEY = 'ganadero:sidebar-collapsed'

function getInitialCollapsed() {
  try {
    return localStorage.getItem(COLLAPSED_STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

export function Sidebar() {
  const { user, can } = useAuth()
  const [collapsed, setCollapsed] = useState(getInitialCollapsed)

  function toggleCollapsed() {
    setCollapsed((prev) => {
      const next = !prev
      try {
        localStorage.setItem(COLLAPSED_STORAGE_KEY, next ? '1' : '0')
      } catch {
        // almacenamiento no disponible: la preferencia simplemente no persiste
      }
      return next
    })
  }

  return (
    <aside className={cn('sidebar', collapsed && 'collapsed')} aria-label="Navegación principal">
      <div className="brand">
        <img src="/logo.svg" alt="" width="42" height="42" />
        {!collapsed && (
          <div>
            <strong>GANADERO</strong>
            <span>Gestión de campo</span>
          </div>
        )}
        <button
          type="button"
          className="icon-button sidebar-toggle"
          onClick={toggleCollapsed}
          aria-label={collapsed ? 'Expandir barra lateral' : 'Encoger barra lateral'}
          title={collapsed ? 'Expandir barra lateral' : 'Encoger barra lateral'}
        >
          {collapsed ? <PanelLeftOpen size={18} aria-hidden="true" /> : <PanelLeftClose size={18} aria-hidden="true" />}
        </button>
      </div>

      <nav className="sidebar-nav">
        {appModules.filter((module) => !module.permission || can(module.permission)).map((module) => {
          const Icon = module.icon
          return (
            <NavLink
              key={module.key}
              to={module.path}
              end={module.path === '/'}
              title={collapsed ? module.label : undefined}
              className={({ isActive }) => cn('nav-item', isActive && 'active')}
            >
              <Icon size={19} aria-hidden="true" />
              <span>{module.label}</span>
              {module.status !== 'LISTO' && <small className={`module-status module-status-${module.status.toLowerCase()}`}>{MODULE_STATUS_LABEL[module.status]}</small>}
              {module.status === 'LISTO' && module.phase > 1 && <small>F{module.phase}</small>}
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="user-summary">
          <span className="avatar">{user.displayName.slice(0, 1).toUpperCase()}</span>
          <div>
            <strong>{user.displayName}</strong>
          </div>
        </div>
      </div>
    </aside>
  )
}
