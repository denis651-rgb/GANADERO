import type { DashboardResumen } from '@/features/dashboard/api'

export type DashboardScope = 'TODA_EMPRESA' | 'PROPIEDADES_ASIGNADAS'

export interface DashboardLocalData {
  operacionesPendientes: number
  conflictos: number
  archivosPendientes: number
  ultimaSincronizacion?: string
}

export interface DashboardModel {
  resumen: DashboardResumen
  local: DashboardLocalData
  scope: DashboardScope
  tieneDatos: boolean
}

export function buildDashboardModel(
  resumen: DashboardResumen,
  local: DashboardLocalData,
  scope: DashboardScope,
): DashboardModel {
  const tieneDatos =
    resumen.totalAnimales > 0 ||
    resumen.lotesActivos > 0 ||
    resumen.potrerosActivos > 0 ||
    resumen.pesajesRecientes.length > 0 ||
    local.operacionesPendientes > 0 ||
    local.conflictos > 0

  return { resumen, local, scope, tieneDatos }
}

export function formatPesoKg(value?: number): string {
  if (value == null) return '—'
  return `${value.toLocaleString('es-BO', { maximumFractionDigits: 1 })} kg`
}
