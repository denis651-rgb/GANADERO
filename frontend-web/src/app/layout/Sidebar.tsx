import { NavLink } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { appModules } from '@/app/modules'
import { useAuth } from '@/auth/auth-context'
import { cn } from '@/shared/utils/cn'

export function Sidebar() {
  const { user, can, signOut } = useAuth()

  return (
    <aside className="sidebar" aria-label="Navegación principal">
      <div className="brand">
        <img src="/logo.svg" alt="" width="42" height="42" />
        <div>
          <strong>GANADERO</strong>
          <span>Gestión de campo</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {appModules.filter((module) => !module.permission || can(module.permission)).map((module) => {
          const Icon = module.icon
          return (
            <NavLink
              key={module.key}
              to={module.path}
              end={module.path === '/'}
              className={({ isActive }) => cn('nav-item', isActive && 'active')}
            >
              <Icon size={19} aria-hidden="true" />
              <span>{module.label}</span>
              {module.phase > 1 && <small>F{module.phase}</small>}
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="user-summary">
          <span className="avatar">{user?.displayName.slice(0, 1).toUpperCase()}</span>
          <div>
            <strong>{user?.displayName}</strong>
            <span>{user?.companyName}</span>
          </div>
        </div>
        <button type="button" className="icon-button" onClick={() => void signOut()} aria-label="Cerrar sesión">
          <LogOut size={19} />
        </button>
      </div>
    </aside>
  )
}
