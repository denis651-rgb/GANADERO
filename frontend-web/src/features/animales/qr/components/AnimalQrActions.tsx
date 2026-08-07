import { useState } from 'react'
import { Download, Printer, RefreshCw, Trash2 } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import type { Identificador } from '@/features/animales/types'
import { downloadQr, fetchQrImage } from '@/features/animales/qr/qr-api'
import type { QrImageFormat } from '@/features/animales/qr/qr-types'
import { printRoute } from '@/features/animales/qr/print-utils'

interface AnimalQrActionsProps {
  animalId: string
  animalCodigo: string
  identificador: Identificador
  format?: QrImageFormat
  onReplace: () => void
  onRetire: () => void
}

export function AnimalQrActions({ animalId, animalCodigo, identificador, format = 'png', onReplace, onRetire }: AnimalQrActionsProps) {
  const [downloading, setDownloading] = useState(false)
  const [openingSvg, setOpeningSvg] = useState(false)
  const retired = identificador.estado === 'RETIRADO'

  const handleDownload = async () => {
    setDownloading(true)
    try {
      await downloadQr(animalId, identificador.id, `qr-${animalCodigo}-${identificador.valor}`, { format })
    } finally {
      setDownloading(false)
    }
  }

  const handleOpenSvg = async () => {
    setOpeningSvg(true)
    try {
      const blob = await fetchQrImage(animalId, identificador.id, { format: 'svg', size: 512 })
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener')
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } finally {
      setOpeningSvg(false)
    }
  }

  return (
    <div className="qr-actions">
      {!retired && (
        <>
          <Button variant="secondary" className="button-small" onClick={handleDownload} loading={downloading}>
            <Download size={14} /> Descargar
          </Button>
          <Button variant="secondary" className="button-small" onClick={() => void handleOpenSvg()} loading={openingSvg}>
            <Printer size={14} /> SVG
          </Button>
          <a
            className="button button-secondary button-small"
            href={printRoute([{ animalId, identifierId: identificador.id, codigo: animalCodigo }])}
          >
            <Printer size={14} /> Imprimir
          </a>
          <Button variant="secondary" className="button-small" onClick={onReplace}>
            <RefreshCw size={14} /> Reemplazar
          </Button>
          <Button variant="danger" className="button-small" onClick={onRetire}>
            <Trash2 size={14} /> Retirar
          </Button>
        </>
      )}
      {retired && <span className="qr-retired-note">Este QR fue retirado y no puede descargarse ni imprimirse.</span>}
    </div>
  )
}
