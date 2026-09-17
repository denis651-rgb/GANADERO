import { useState } from 'react'
import { Download } from 'lucide-react'
import { PageHeader } from '@/shared/components/PageHeader'
import { Button } from '@/shared/components/Button'
import { formatDate } from '@/shared/utils/date'
import { manual } from '@/features/manual/manualRegistry'
import { ManualSearch } from '@/features/manual/components/ManualSearch'
import { useToast } from '@/shared/toast/useToast'

/** Solo existe en la app de escritorio — no hay forma de generar el PDF desde el navegador suelto. */
function useManualPdfExport() {
  const { showToast } = useToast()
  const [exporting, setExporting] = useState(false)
  const exportPdf = window.ganadero?.manual?.exportPdf

  async function handleClick() {
    if (!exportPdf) return
    setExporting(true)
    try {
      const result = await exportPdf()
      if (result.ok) showToast('PDF del manual guardado correctamente.')
      else if (!result.cancelled) showToast(result.message, 'danger')
    } catch {
      showToast('No se pudo generar el PDF del manual.', 'danger')
    } finally {
      setExporting(false)
    }
  }

  return { canExport: Boolean(exportPdf), exporting, handleClick }
}

export function ManualHeader() {
  const { canExport, exporting, handleClick } = useManualPdfExport()

  return (
    <>
      <PageHeader
        eyebrow="Ayuda"
        title={manual.title}
        description="Guía paso a paso de cada módulo de Ganadero, disponible sin conexión."
        actions={canExport
          ? <Button variant="secondary" onClick={handleClick} loading={exporting}>
              <Download size={16} aria-hidden="true" />Descargar PDF
            </Button>
          : undefined}
      />
      <p className="manual-header-meta">
        Versión {manual.version} · Compatible con Ganadero {manual.systemVersion} · Actualizado el {formatDate(manual.updatedAt)}
      </p>
      <ManualSearch />
    </>
  )
}
