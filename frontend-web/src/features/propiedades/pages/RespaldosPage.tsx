import { BackupsPanel } from '@/features/configuracion/components/BackupsPanel'
import { PageHeader } from '@/shared/components/PageHeader'

export function RespaldosPage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Respaldos" description="Copias de seguridad de la base de datos de la operación." />
      <BackupsPanel />
    </div>
  )
}
