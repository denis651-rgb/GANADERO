import type { LucideIcon } from 'lucide-react'
import {
  Activity,
  Bell,
  Beef,
  Boxes,
  ClipboardList,
  FileBarChart,
  HeartPulse,
  House,
  MapPinned,
  Route,
  Scale,
  ShoppingCart,
  Sprout,
  Truck,
} from 'lucide-react'

export type ModuleStatus = 'LISTO' | 'EN_DESARROLLO' | 'PROXIMAMENTE'

export interface AppModuleDefinition {
  key: string
  label: string
  path: string
  icon: LucideIcon
  phase: number
  permission?: string
  status: ModuleStatus
}

export const appModules: AppModuleDefinition[] = [
  { key: 'dashboard', label: 'Panel principal', path: '/', icon: House, phase: 0, status: 'LISTO' },
  { key: 'propiedades', label: 'Mi finca', path: '/propiedades', icon: MapPinned, phase: 0, status: 'LISTO' },
  { key: 'potreros', label: 'Potreros', path: '/potreros', icon: Sprout, phase: 0, status: 'LISTO' },
  { key: 'animales', label: 'Animales', path: '/animales', icon: Beef, phase: 0, status: 'LISTO' },
  { key: 'compras', label: 'Compras', path: '/compras', icon: Truck, phase: 0, status: 'LISTO' },
  { key: 'lotes', label: 'Lotes ganaderos', path: '/lotes', icon: Boxes, phase: 0, status: 'LISTO' },
  { key: 'movimientos', label: 'Movimientos', path: '/movimientos', icon: Route, phase: 0, status: 'LISTO' },
  { key: 'auditoria', label: 'Auditoría', path: '/auditoria', icon: ClipboardList, phase: 0, status: 'LISTO' },
  { key: 'pesajes', label: 'Pesajes', path: '/pesajes', icon: Scale, phase: 0, status: 'LISTO' },
  { key: 'reproduccion', label: 'Reproducción', path: '/reproduccion', icon: Activity, phase: 0, status: 'LISTO' },
  { key: 'sanidad', label: 'Sanidad', path: '/sanidad', icon: HeartPulse, phase: 0, status: 'LISTO' },
  { key: 'alertas', label: 'Alertas', path: '/alertas', icon: Bell, phase: 0, status: 'LISTO' },
  { key: 'ventas', label: 'Ventas', path: '/ventas', icon: ShoppingCart, phase: 0, status: 'LISTO' },
  { key: 'reportes', label: 'Reportes', path: '/reportes', icon: FileBarChart, phase: 0, status: 'LISTO' },
]

export const MODULE_STATUS_LABEL: Record<ModuleStatus, string> = {
  LISTO: 'Listo',
  EN_DESARROLLO: 'En desarrollo',
  PROXIMAMENTE: 'Próximamente',
}
