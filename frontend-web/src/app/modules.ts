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
  MailPlus,
  MapPinned,
  PackageSearch,
  RefreshCw,
  Route,
  Scale,
  Settings,
  ShieldCheck,
  ShoppingCart,
  Sprout,
  UserRound,
  Users,
  WalletCards,
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
  { key: 'perfil', label: 'Mi perfil', path: '/perfil', icon: UserRound, phase: 1, status: 'LISTO' },
  { key: 'empresa', label: 'Empresa', path: '/empresa', icon: Building2, phase: 1, permission: 'EMPRESA_VER', status: 'LISTO' },
  { key: 'usuarios', label: 'Usuarios', path: '/usuarios', icon: Users, phase: 1, permission: 'USUARIO_VER', status: 'LISTO' },
  { key: 'invitaciones', label: 'Invitaciones', path: '/invitaciones', icon: MailPlus, phase: 1, permission: 'USUARIO_VER', status: 'LISTO' },
  { key: 'roles', label: 'Roles y permisos', path: '/roles', icon: ShieldCheck, phase: 1, permission: 'ROL_VER', status: 'LISTO' },
  { key: 'propiedades', label: 'Propiedades', path: '/propiedades', icon: MapPinned, phase: 1, permission: 'PROPIEDAD_VER', status: 'LISTO' },
  { key: 'potreros', label: 'Potreros', path: '/potreros', icon: Sprout, phase: 1, permission: 'POTRERO_VER', status: 'LISTO' },
  { key: 'animales', label: 'Animales', path: '/animales', icon: Beef, phase: 1, permission: 'ANIMAL_VER', status: 'LISTO' },
  { key: 'lotes', label: 'Lotes ganaderos', path: '/lotes', icon: Boxes, phase: 1, permission: 'LOTE_VER', status: 'LISTO' },
  { key: 'movimientos', label: 'Movimientos', path: '/movimientos', icon: Route, phase: 1, permission: 'MOVIMIENTO_VER', status: 'LISTO' },
  { key: 'auditoria', label: 'Auditoría', path: '/auditoria', icon: ClipboardList, phase: 1, permission: 'AUDITORIA_VER', status: 'LISTO' },
  { key: 'pesajes', label: 'Pesajes', path: '/pesajes', icon: Scale, phase: 2, permission: 'PESAJE_VER', status: 'LISTO' },
  { key: 'sincronizacion', label: 'Sincronización', path: '/sincronizacion', icon: RefreshCw, phase: 2, status: 'LISTO' },
  { key: 'reproduccion', label: 'Reproducción', path: '/reproduccion', icon: Activity, phase: 3, permission: 'REPRODUCCION_VER', status: 'LISTO' },
  { key: 'sanidad', label: 'Sanidad', path: '/sanidad', icon: HeartPulse, phase: 3, permission: 'SANIDAD_VER', status: 'LISTO' },
  { key: 'alertas', label: 'Alertas', path: '/alertas', icon: Bell, phase: 3, permission: 'ALERTA_VER', status: 'LISTO' },
  { key: 'inventario', label: 'Inventario', path: '/inventario', icon: PackageSearch, phase: 4, permission: 'INVENTARIO_VER', status: 'EN_DESARROLLO' },
  { key: 'alimentacion', label: 'Alimentación', path: '/alimentacion', icon: Settings, phase: 4, permission: 'ALIMENTACION_VER', status: 'EN_DESARROLLO' },
  { key: 'comercial', label: 'Compras y ventas', path: '/comercial', icon: ShoppingCart, phase: 5, permission: 'COMERCIAL_VER', status: 'PROXIMAMENTE' },
  { key: 'finanzas', label: 'Finanzas', path: '/finanzas', icon: WalletCards, phase: 5, permission: 'FINANZAS_VER', status: 'PROXIMAMENTE' },
  { key: 'reportes', label: 'Reportes', path: '/reportes', icon: FileBarChart, phase: 6, permission: 'REPORTE_VER', status: 'PROXIMAMENTE' },
]

export const MODULE_STATUS_LABEL: Record<ModuleStatus, string> = {
  LISTO: 'Listo',
  EN_DESARROLLO: 'En desarrollo',
  PROXIMAMENTE: 'Próximamente',
}
