import { CategoriasEdadPanel } from '@/features/configuracion/components/CategoriasEdadPanel'
import { PageHeader } from '@/shared/components/PageHeader'

export function CategoriasEdadPage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Categorías por edad" description="Rangos de edad usados para clasificar automáticamente al hato." />
      <CategoriasEdadPanel />
    </div>
  )
}
