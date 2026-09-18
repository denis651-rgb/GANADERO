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

const TIPOS_REPRODUCCION = new Set(['CELO_DETECTADO', 'DIAGNOSTICO_PENDIENTE', 'PARTO_PROXIMO', 'DESTETE_PROXIMO'])
const TIPOS_SANIDAD = new Set([
  'VACUNA_PROXIMA', 'VACUNA_VENCIDA', 'ACTIVIDAD_SANITARIA_PROXIMA', 'ACTIVIDAD_SANITARIA_VENCIDA',
  'REVISION_SANITARIA_INGRESO', 'RETIRO_CARNE_VIGENTE', 'RETIRO_LECHE_VIGENTE', 'CUARENTENA_POR_FINALIZAR',
  'CASO_CLINICO_CRITICO', 'RECORDATORIO_SANIDAD', 'TRATAMIENTO_PROXIMO', 'TRATAMIENTO_ATRASADO',
])

/** A qué pantalla llevar cada tipo de alerta: la del módulo donde se resuelve. Los tipos sin destino propio no llevan botón. */
function accionDeAlerta(tipo: string): Pick<AttentionItem, 'actionHref' | 'actionLabel'> {
  if (TIPOS_REPRODUCCION.has(tipo)) return { actionHref: '/reproduccion', actionLabel: 'Ir a Reproducción' }
  if (TIPOS_SANIDAD.has(tipo)) return { actionHref: '/sanidad', actionLabel: 'Ir a Sanidad' }
  if (tipo === 'MOVIMIENTO_PENDIENTE') return { actionHref: '/movimientos', actionLabel: 'Ir a Movimientos' }
  if (tipo === 'INVENTARIO_BAJO' || tipo === 'SISTEMA_REQUIERE_ATENCION') return { actionHref: '/alertas', actionLabel: 'Ver alertas' }
  return {}
}

/**
 * Combina las alertas del backend (sanidad, reproducción, tratamientos, movimientos… y los potreros
 * inactivos) con el recordatorio de pesaje, que arma la pantalla porque lleva un botón para registrar
 * el pesaje, y ordena todo por severidad para que lo urgente (danger) siempre quede antes que lo
 * meramente informativo, sin importar el orden de origen.
 */
export function buildAttentionItems(resumen: DashboardResumen): AttentionItem[] {
  const items: AttentionItem[] = resumen.alertas.map((alerta) => ({
    key: alerta.tipo,
    severidad: alerta.severidad,
    mensaje: alerta.mensaje,
    detalle: `${alerta.total} ${alerta.total === 1 ? 'registro requiere' : 'registros requieren'} atención.`,
    ...accionDeAlerta(alerta.tipo),
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

/** Mañana hasta las 12, tarde hasta las 19, noche el resto. */
export function saludoPorHora(hora: number): string {
  if (hora < 12) return 'Buenos días'
  if (hora < 19) return 'Buenas tardes'
  return 'Buenas noches'
}
