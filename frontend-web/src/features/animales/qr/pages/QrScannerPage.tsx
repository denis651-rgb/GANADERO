import { QrScanner } from '@/features/animales/qr/components/QrScanner'
import { PageHeader } from '@/shared/components/PageHeader'
import { Card } from '@/shared/components/Card'

export function QrScannerPage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="QR" title="Escanear código QR" description="Verifica la autenticidad de un código QR de un animal. Funciona sin conexión usando datos sincronizados." />
      <Card>
        <QrScanner />
      </Card>
    </div>
  )
}
