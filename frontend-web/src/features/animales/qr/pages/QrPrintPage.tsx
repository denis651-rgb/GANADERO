import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { Printer } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { Alert } from '@/shared/components/Alert'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'
import { useQrImage } from '@/features/animales/qr/useQrImage'
import { PRINT_FORMATS } from '@/features/animales/qr/qr-types'
import { decodePrintItems } from '@/features/animales/qr/print-utils'
import type { PrintFormat } from '@/features/animales/qr/qr-types'
import { Link } from 'react-router'

function PrintCell({ animalId, identifierId, codigo, small }: { animalId: string; identifierId: string; codigo: string; small?: boolean }) {
  const { blobUrl } = useQrImage(animalId, identifierId, 'png', 256)
  return (
    <div className={`qr-print-cell ${small ? 'small' : ''}`}>
      {blobUrl ? <img src={blobUrl} alt={`QR de ${codigo}`} /> : <div className="qr-preview-loading">Cargando QR…</div>}
      <span className="qr-print-code">{codigo}</span>
      <span className="qr-print-id">{identifierId}</span>
    </div>
  )
}

export function QrPrintPage() {
  const [searchParams] = useSearchParams()
  const items = useMemo(() => decodePrintItems(searchParams.get('q')), [searchParams])
  const [format, setFormat] = useState<PrintFormat>('PEQUENO')

  if (items.length === 0) {
    return (
      <div className="page-stack">
        <PageHeader eyebrow="QR" title="Impresión de códigos QR" description="Prepara etiquetas de códigos QR para identificar animales." />
        <Card><EmptyState title="No hay códigos seleccionados" description="Vuelve al listado de animales, selecciona uno o varios y usa la acción «Imprimir QR»." /></Card>
      </div>
    )
  }

  return (
    <div className="qr-print-page">
      <div className="qr-print-toolbar">
        <div>
          <strong>{items.length} código{items.length === 1 ? '' : 's'} QR</strong>
          <p className="muted">Elige el tamaño de la etiqueta y pulsa imprimir.</p>
        </div>
        <div className="row-actions">
          <Link className="button button-secondary" to="/animales">Volver</Link>
          <Button onClick={() => window.print()}><Printer size={16} />Imprimir</Button>
        </div>
      </div>

      <div className="qr-print-formats">
        {PRINT_FORMATS.map((option) => (
          <button key={option.value} type="button" className={`qr-print-format ${format === option.value ? 'selected' : ''}`} onClick={() => setFormat(option.value)}>
            <strong>{option.label}</strong>
            <small>{option.description}</small>
          </button>
        ))}
      </div>

      <div className={`qr-print-grid ${format.toLowerCase()}`}>
        {items.map((item) => (
          <div className="qr-print-sheet" key={item.identifierId}>
            <PrintCell animalId={item.animalId} identifierId={item.identifierId} codigo={item.codigo} small={format === 'PEQUENO'} />
          </div>
        ))}
      </div>

      {format === 'A4_MULTIPLE' && items.length < 8 && (
        <Alert tone="info">En formato A4 múltiple se recomiendan 8 códigos por hoja. Selecciona más animales o repite los códigos para completar la hoja.</Alert>
      )}
    </div>
  )
}
