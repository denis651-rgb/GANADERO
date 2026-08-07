import { useQrImage } from '@/features/animales/qr/useQrImage'
import type { QrImageFormat, QrImageSize } from '@/features/animales/qr/qr-types'

interface AnimalQrPreviewProps {
  animalId: string
  identificadorId: string
  format?: QrImageFormat
  size?: QrImageSize
  retired?: boolean
  className?: string
}

export function AnimalQrPreview({ animalId, identificadorId, format = 'png', size = 512, retired, className }: AnimalQrPreviewProps) {
  const { blobUrl, error, loading } = useQrImage(animalId, identificadorId, format, size)

  if (loading) return <div className="qr-preview qr-preview-loading">Generando imagen QR…</div>
  if (error || !blobUrl) return <div className="qr-preview qr-preview-error">No se pudo generar la imagen QR.</div>

  return (
    <div className={`qr-preview ${retired ? 'qr-preview-retired' : ''} ${className ?? ''}`}>
      <img src={blobUrl} alt={`Código QR del animal`} />
    </div>
  )
}
