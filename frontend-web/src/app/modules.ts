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
  Settings,
  ShoppingCart,
  Sprout,
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
  { key: 'propiedades', label: 'Mi finca', path: '/propiedades', icon: MapPinned, phase: 1, status: 'LISTO' },
  { key: 'potreros', label: 'Potreros', path: '/potreros', icon: Sprout, phase: 1, status: 'LISTO' },
  { key: 'animales', label: 'Animales', path: '/animales', icon: Beef, phase: 1, status: 'LISTO' },
  { key: 'lotes', label: 'Lotes ganaderos', path: '/lotes', icon: Boxes, phase: 1, status: 'LISTO' },
  { key: 'movimientos', label: 'Movimientos', path: '/movimientos', icon: Route, phase: 1, status: 'LISTO' },
  { key: 'auditoria', label: 'Auditoría', path: '/auditoria', icon: ClipboardList, phase: 1, status: 'LISTO' },
  { key: 'pesajes', label: 'Pesajes', path: '/pesajes', icon: Scale, phase: 2, status: 'LISTO' },
  { key: 'reproduccion', label: 'Reproducción', path: '/reproduccion', icon: Activity, phase: 3, status: 'LISTO' },
  { key: 'sanidad', label: 'Sanidad', path: '/sanidad', icon: HeartPulse, phase: 3, status: 'LISTO' },
  { key: 'alertas', label: 'Alertas', path: '/alertas', icon: Bell, phase: 3, status: 'LISTO' },
  { key: 'alimentacion', label: 'Alimentación', path: '/alimentacion', icon: Settings, phase: 4, status: 'EN_DESARROLLO' },
  { key: 'ventas', label: 'Ventas', path: '/ventas', icon: ShoppingCart, phase: 5, status: 'LISTO' },
  { key: 'reportes', label: 'Reportes', path: '/reportes', icon: FileBarChart, phase: 6, status: 'PROXIMAMENTE' },
]

export const MODULE_STATUS_LABEL: Record<ModuleStatus, string> = {
  LISTO: 'Listo',
  EN_DESARROLLO: 'En desarrollo',
  PROXIMAMENTE: 'Próximamente',
}
