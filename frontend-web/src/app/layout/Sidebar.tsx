import { useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { NavLink, useLocation } from 'react-router'
import { ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'
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

interface FlyoutState {
  key: string
  top: number
  left: number
}

export function Sidebar() {
  const { user, can } = useAuth()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(getInitialCollapsed)
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set())
  const [flyout, setFlyout] = useState<FlyoutState | null>(null)
  const closeTimer = useRef<number | null>(null)

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

  function toggleGroup(key: string) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  function cancelFlyoutClose() {
    if (closeTimer.current) {
      window.clearTimeout(closeTimer.current)
      closeTimer.current = null
    }
  }

  function openFlyout(key: string, target: HTMLElement) {
    cancelFlyoutClose()
    const rect = target.getBoundingClientRect()
    setFlyout({ key, top: rect.top, left: rect.right + 8 })
  }

  function scheduleFlyoutClose() {
    cancelFlyoutClose()
    closeTimer.current = window.setTimeout(() => setFlyout(null), 150)
  }

  const flyoutModule = flyout ? appModules.find((module) => module.key === flyout.key) : null

  return (
    <aside className={cn('sidebar', collapsed && 'collapsed')} aria-label="Navegación principal">
      <div className="brand">
        <img src="/icons/logo.png" alt="" width="42" height="42" />
        {!collapsed && (
          <div>
            <strong>GANADERO</strong>
            <span>Gestión de campo</span>
          </div>
        )}
      </div>

      <button
        type="button"
        className="sidebar-toggle"
        onClick={toggleCollapsed}
        aria-label={collapsed ? 'Expandir barra lateral' : 'Encoger barra lateral'}
        title={collapsed ? 'Expandir barra lateral' : 'Encoger barra lateral'}
      >
        {collapsed ? <ChevronsRight size={14} aria-hidden="true" /> : <ChevronsLeft size={14} aria-hidden="true" />}
      </button>

      <nav className="sidebar-nav">
        {appModules.filter((module) => !module.permission || can(module.permission)).map((module) => {
          const Icon = module.icon

          if (module.children) {
            const childActive = module.children.some((child) => location.pathname === child.path)

            if (collapsed) {
              const isOpen = flyout?.key === module.key
              return (
                <div key={module.key} onMouseEnter={(event) => openFlyout(module.key, event.currentTarget)} onMouseLeave={scheduleFlyoutClose}>
                  <button
                    type="button"
                    className={cn('nav-item', childActive && 'active')}
                    title={module.label}
                    aria-expanded={isOpen}
                    onFocus={(event) => openFlyout(module.key, event.currentTarget)}
                    onClick={(event) => (isOpen ? setFlyout(null) : openFlyout(module.key, event.currentTarget))}
                  >
                    <Icon size={19} aria-hidden="true" />
                  </button>
                </div>
              )
            }

            const isOpen = expanded.has(module.key)
            return (
              <div className="nav-group" key={module.key}>
                <button
                  type="button"
                  className={cn('nav-toggle', childActive && 'child-active')}
                  aria-expanded={isOpen}
                  onClick={() => toggleGroup(module.key)}
                >
                  <Icon size={19} aria-hidden="true" />
                  <span>{module.label}</span>
                  <ChevronRight size={16} aria-hidden="true" className="nav-toggle-chev" />
                </button>
                {isOpen && (
                  <ul className="nav-sublist">
                    {module.children.map((child) => (
                      <li key={child.key}>
                        <NavLink to={child.path} end className={({ isActive }) => cn('nav-subitem', isActive && 'active')}>
                          {child.label}
                        </NavLink>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )
          }

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

      {flyout && flyoutModule?.children && createPortal(
        <div
          className="nav-flyout"
          style={{ top: flyout.top, left: flyout.left }}
          onMouseEnter={cancelFlyoutClose}
          onMouseLeave={scheduleFlyoutClose}
        >
          <div className="nav-flyout-title">{flyoutModule.label}</div>
          <ul className="nav-sublist">
            {flyoutModule.children.map((child) => (
              <li key={child.key}>
                <NavLink to={child.path} end className={({ isActive }) => cn('nav-subitem', isActive && 'active')} onClick={() => setFlyout(null)}>
                  {child.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>,
        document.body,
      )}
    </aside>
  )
}
