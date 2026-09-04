import type { GanaderoAlert } from '@/features/alertas/api'
import type { Seccion } from '@/features/sanidad/pages/SanidadPage'

export type ResumenAlertSeveridad = 'danger' | 'warning'

export interface ResumenAlertItem {
  id: string
  severidad: ResumenAlertSeveridad
  mensaje: string
  detalle: string
  seccion: Seccion
  seccionLabel: string
}

interface FuenteAlerta {
  alertas: GanaderoAlert[]
  severidad: ResumenAlertSeveridad
  seccion: Seccion
  seccionLabel: string
}

const SEVERIDAD_RANK: Record<ResumenAlertSeveridad, number> = { danger: 0, warning: 1 }

/** Una alerta ya resuelta, cancelada o atendida dejó de requerir atención en este panel. */
function requiereAtencion(alerta: GanaderoAlert): boolean {
  return !['RESUELTA', 'CANCELADA', 'ATENDIDA'].includes(alerta.estado)
}

/**
 * Combina listas de alertas de distintos tipos/orígenes en un único listado de "atención
 * requerida", ordenado por severidad (danger antes que warning) para que lo urgente
 * siempre quede primero sin importar el orden en que llegaron las fuentes.
 */
export function buildResumenAlertItems(fuentes: FuenteAlerta[]): ResumenAlertItem[] {
  const items = fuentes.flatMap(({ alertas, severidad, seccion, seccionLabel }) =>
    alertas.filter(requiereAtencion).map((alerta) => ({
      id: alerta.id,
      severidad,
      mensaje: alerta.titulo,
      detalle: alerta.mensaje,
      seccion,
      seccionLabel,
    })),
  )
  return items.sort((a, b) => SEVERIDAD_RANK[a.severidad] - SEVERIDAD_RANK[b.severidad])
}
