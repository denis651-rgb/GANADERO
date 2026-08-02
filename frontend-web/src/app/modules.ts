import type { LucideIcon } from 'lucide-react'
import {
  Activity,
  Bell,
  Beef,
  Boxes,
  Building2,
  ClipboardList,
  FileBarChart,
  HeartPulse,
  House,
  MapPinned,
  PackageSearch,
  RefreshCw,
  Route,
  Scale,
  Settings,
  ShieldCheck,
  ShoppingCart,
  Sprout,
  Users,
  WalletCards,
} from 'lucide-react'

export interface AppModuleDefinition {
  key: string
  label: string
  path: string
  icon: LucideIcon
  phase: number
  permission?: string
}

export const appModules: AppModuleDefinition[] = [
  { key: 'dashboard', label: 'Panel principal', path: '/', icon: House, phase: 0 },
  { key: 'empresa', label: 'Empresa', path: '/empresa', icon: Building2, phase: 1, permission: 'EMPRESA_VER' },
  { key: 'usuarios', label: 'Usuarios', path: '/usuarios', icon: Users, phase: 1, permission: 'USUARIO_VER' },
  { key: 'roles', label: 'Roles y permisos', path: '/roles', icon: ShieldCheck, phase: 1, permission: 'ROL_VER' },
  { key: 'propiedades', label: 'Propiedades', path: '/propiedades', icon: MapPinned, phase: 1, permission: 'PROPIEDAD_VER' },
  { key: 'potreros', label: 'Potreros', path: '/potreros', icon: Sprout, phase: 1, permission: 'POTRERO_VER' },
  { key: 'animales', label: 'Animales', path: '/animales', icon: Beef, phase: 1, permission: 'ANIMAL_VER' },
  { key: 'lotes', label: 'Lotes ganaderos', path: '/lotes', icon: Boxes, phase: 1, permission: 'LOTE_VER' },
  { key: 'movimientos', label: 'Movimientos', path: '/movimientos', icon: Route, phase: 1, permission: 'MOVIMIENTO_VER' },
  { key: 'auditoria', label: 'Auditoría', path: '/auditoria', icon: ClipboardList, phase: 1, permission: 'AUDITORIA_VER' },
  { key: 'pesajes', label: 'Pesajes', path: '/pesajes', icon: Scale, phase: 2, permission: 'PESAJE_VER' },
  { key: 'reproduccion', label: 'Reproducción', path: '/reproduccion', icon: Activity, phase: 3, permission: 'REPRODUCCION_VER' },
  { key: 'sanidad', label: 'Sanidad', path: '/sanidad', icon: HeartPulse, phase: 3, permission: 'SANIDAD_VER' },
  { key: 'inventario', label: 'Inventario', path: '/inventario', icon: PackageSearch, phase: 4, permission: 'INVENTARIO_VER' },
  { key: 'alimentacion', label: 'Alimentación', path: '/alimentacion', icon: Settings, phase: 4, permission: 'ALIMENTACION_VER' },
  { key: 'comercial', label: 'Compras y ventas', path: '/comercial', icon: ShoppingCart, phase: 5, permission: 'COMERCIAL_VER' },
  { key: 'finanzas', label: 'Finanzas', path: '/finanzas', icon: WalletCards, phase: 5, permission: 'FINANZAS_VER' },
  { key: 'alertas', label: 'Alertas', path: '/alertas', icon: Bell, phase: 3, permission: 'ALERTA_VER' },
  { key: 'reportes', label: 'Reportes', path: '/reportes', icon: FileBarChart, phase: 6, permission: 'REPORTE_VER' },
  { key: 'sincronizacion', label: 'Sincronización', path: '/sincronizacion', icon: RefreshCw, phase: 2 },
]
