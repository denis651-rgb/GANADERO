import { NavLink } from 'react-router-dom'
import { Beef, House, MapPinned, RefreshCw } from 'lucide-react'
import { cn } from '@/shared/utils/cn'

const items = [
  { path: '/', label: 'Inicio', icon: House, end: true },
  { path: '/animales', label: 'Animales', icon: Beef },
  { path: '/potreros', label: 'Campo', icon: MapPinned },
  { path: '/sincronizacion', label: 'Sincronizar', icon: RefreshCw },
]

export function MobileNav() {
  return (
    <nav className="mobile-nav" aria-label="Navegación móvil">
      {items.map((item) => {
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
    </nav>
  )
}
