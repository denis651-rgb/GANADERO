import { PageHeader } from '@/shared/components/PageHeader'
import { formatDate } from '@/shared/utils/date'
import { manual } from '@/features/manual/manualRegistry'
import { ManualSearch } from '@/features/manual/components/ManualSearch'

export function ManualHeader() {
  return (
    <>
      <PageHeader
        eyebrow="Ayuda"
        title={manual.title}
        description="Guía paso a paso de cada módulo de Ganadero, disponible sin conexión."
      />
      <p className="manual-header-meta">
        Versión {manual.version} · Compatible con Ganadero {manual.systemVersion} · Actualizado el {formatDate(manual.updatedAt)}
      </p>
      <ManualSearch />
    </>
  )
}
