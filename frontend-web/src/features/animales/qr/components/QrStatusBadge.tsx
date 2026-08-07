import type { Identificador } from '@/features/animales/types'

export function QrStatusBadge({ identificador }: { identificador: Identificador }) {
  if (identificador.estado === 'RETIRADO') {
    return <span className="status-badge status-retired">QR retirado</span>
  }
  return <span className="status-badge status-active">QR activo</span>
}
