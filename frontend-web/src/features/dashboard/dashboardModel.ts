import type { DashboardAlerta, DashboardResumen } from '@/features/dashboard/api'

export interface DashboardModel {
  resumen: DashboardResumen
  tieneDatos: boolean
  attentionItems: AttentionItem[]
}

export type AttentionSeveridad = DashboardAlerta['severidad']

export interface AttentionItem {
  key: string
  severidad: AttentionSeveridad
  mensaje: string
  detalle: string
  actionHref?: string
  actionLabel?: string
}

const SEVERIDAD_RANK: Record<AttentionSeveridad, number> = { danger: 0, warning: 1, info: 2 }

/**
 * Combina las alertas del backend con el recordatorio de pesaje (que no viene
 * como alerta) y ordena todo por severidad para que lo urgente (danger) siempre
 * quede antes que lo meramente informativo, sin importar el orden de origen.
 */
export function buildAttentionItems(resumen: DashboardResumen): AttentionItem[] {
  const items: AttentionItem[] = resumen.alertas.map((alerta) => ({
    key: alerta.tipo,
    severidad: alerta.severidad,
    mensaje: alerta.mensaje,
    detalle: `${alerta.total} registro(s) requieren atención.`,
  }))

  if (resumen.animalesSinPesaje > 0) {
    items.push({
      key: 'SIN_PESAJE_RECIENTE',
      severidad: 'warning',
      mensaje: `${resumen.animalesSinPesaje} animales sin pesaje reciente`,
      detalle: 'Registra controles para mantener actualizado el seguimiento productivo.',
      actionHref: '/pesajes',
      actionLabel: 'Registrar pesaje',
    })
  }

  return items.sort((a, b) => SEVERIDAD_RANK[a.severidad] - SEVERIDAD_RANK[b.severidad])
}

export function buildDashboardModel(resumen: DashboardResumen): DashboardModel {
  const tieneDatos =
    resumen.totalAnimales > 0 ||
    resumen.lotesActivos > 0 ||
    resumen.potrerosActivos > 0 ||
    resumen.pesajesRecientes.length > 0

  return { resumen, tieneDatos, attentionItems: buildAttentionItems(resumen) }
}

export function formatPesoKg(value?: number): string {
  if (value == null) return '—'
  return `${value.toLocaleString('es-BO', { maximumFractionDigits: 1 })} kg`
}
