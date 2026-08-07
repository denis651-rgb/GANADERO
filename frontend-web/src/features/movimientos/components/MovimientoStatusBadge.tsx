import type { EstadoMovimiento } from '@/features/movimientos/api'

const tones: Record<EstadoMovimiento, string> = {
  PENDIENTE: 'status-badge status-badge-pending',
  CONFIRMADO: 'status-badge status-badge-confirmed',
  ANULADO: 'status-badge status-badge-annulled',
  REVERTIDO: 'status-badge status-badge-reverted',
}

export function MovimientoStatusBadge({ estado }: { estado: EstadoMovimiento }) {
  return <span className={tones[estado]}>{estado}</span>
}
