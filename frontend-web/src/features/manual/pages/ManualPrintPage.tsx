import { useEffect } from 'react'
import { formatDate } from '@/shared/utils/date'
import { manual, manualChapters } from '@/features/manual/manualRegistry'
import { ManualViewer } from '@/features/manual/components/ManualViewer'
import '@/features/manual/manual.css'

/**
 * Destino de impresión del manual completo: lo carga Electron en una ventana oculta
 * (ver electron/src/manual-export.ts) y le aplica `webContents.printToPDF()`. No se navega
 * acá desde la UI normal — la pantalla del manual solo dispara la exportación por IPC.
 *
 * `document.title` lleva la versión para que el proceso principal arme el nombre del archivo
 * sin tener que importar manual.json (ese archivo vive en el bundle del renderer, no en Electron).
 */
export function ManualPrintPage() {
  useEffect(() => {
    document.title = `Manual de usuario de Ganadero v${manual.version}`
  }, [])

  return (
    <div className="manual-print-page">
      <section className="manual-print-cover">
        <p className="manual-print-eyebrow">GANADERO</p>
        <h1>{manual.title}</h1>
        <dl className="manual-print-cover-meta">
          <div><dt>Versión del manual</dt><dd>{manual.version}</dd></div>
          <div><dt>Compatible con Ganadero</dt><dd>{manual.systemVersion}</dd></div>
          <div><dt>Última actualización</dt><dd>{formatDate(manual.updatedAt)}</dd></div>
        </dl>
      </section>

      <section className="manual-print-index">
        <h2>Índice</h2>
        <ol>
          {manual.chapters.map((chapter) => (
            <li key={chapter.id}><a href={`#${chapter.id}`}>{chapter.title}</a></li>
          ))}
        </ol>
      </section>

      {manualChapters.map((chapter) => (
        <div key={chapter.id} id={chapter.id} className="manual-print-chapter">
          <ManualViewer chapter={chapter} />
        </div>
      ))}
    </div>
  )
}
