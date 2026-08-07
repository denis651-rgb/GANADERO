import type { Identificador } from '@/features/animales/types'
import { AnimalQrPreview } from '@/features/animales/qr/components/AnimalQrPreview'
import { AnimalQrActions } from '@/features/animales/qr/components/AnimalQrActions'
import { QrStatusBadge } from '@/features/animales/qr/components/QrStatusBadge'

interface AnimalQrCardProps {
  animalId: string
  animalCodigo: string
  identificador: Identificador
  onReplace: () => void
  onRetire: () => void
  showPayload?: boolean
}

function formatFecha(fecha?: string) {
  if (!fecha) return '—'
  const date = new Date(fecha)
  return isNaN(date.getTime()) ? fecha : date.toLocaleDateString('es-BO')
}

export function AnimalQrCard({ animalId, animalCodigo, identificador, onReplace, onRetire, showPayload }: AnimalQrCardProps) {
  const retired = identificador.estado === 'RETIRADO'

  return (
    <div className="animal-qr-card">
      <div className="animal-qr-header">
        <div>
          <h3>Código QR del animal</h3>
          <p className="muted">Animal {animalCodigo} · Identificador {identificador.valor}</p>
        </div>
        <QrStatusBadge identificador={identificador} />
      </div>

      <div className="animal-qr-body">
        <AnimalQrPreview animalId={animalId} identificadorId={identificador.id} size={512} retired={retired} />
        <div className="animal-qr-meta">
          <dl>
            <dt>Identificador</dt>
            <dd>{identificador.valor}</dd>
            <dt>Principal</dt>
            <dd>{identificador.principal ? 'Sí' : 'No'}</dd>
            <dt>Asignado</dt>
            <dd>{formatFecha(identificador.fechaAsignacion)}</dd>
            {identificador.fechaRetiro && (
              <>
                <dt>Retirado</dt>
                <dd>{formatFecha(identificador.fechaRetiro)}</dd>
              </>
            )}
            {identificador.motivoRetiro && (
              <>
                <dt>Motivo de retiro</dt>
                <dd>{identificador.motivoRetiro}</dd>
              </>
            )}
            <dt>Versión</dt>
            <dd>v{identificador.version}</dd>
          </dl>
        </div>
      </div>

      <AnimalQrActions
        animalId={animalId}
        animalCodigo={animalCodigo}
        identificador={identificador}
        onReplace={onReplace}
        onRetire={onRetire}
      />

      {showPayload && identificador.payload && (
        <details className="qr-payload-details">
          <summary>Ver contenido técnico del QR (sin datos sensibles)</summary>
          <pre className="qr-payload">{identificador.payload}</pre>
        </details>
      )}
    </div>
  )
}
