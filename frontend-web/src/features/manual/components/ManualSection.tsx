import { useState } from 'react'
import { ChevronRight } from 'lucide-react'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { ManualSection as ManualSectionData } from '@/features/manual/sections'
import { ManualAnchorButton } from '@/features/manual/components/ManualAnchorButton'

interface ManualSectionProps {
  section: ManualSectionData
  chapterId: string
  components: Components
  /** true si el hash de la URL apunta a este encabezado o a uno anidado debajo. */
  forceOpen: boolean
}

/**
 * Una operación de un capítulo "forma B" (ver CONVENCIONES.md), plegada por defecto para que el
 * capítulo no sea una sola página larguísima. El cuerpo queda siempre en el DOM (con `hidden`, no
 * desmontado) para que la búsqueda y la impresión sigan viendo el contenido aunque esté plegado.
 */
export function ManualSection({ section, chapterId, components, forceOpen }: ManualSectionProps) {
  // Mientras el usuario no haya tocado el acordeón, sigue a `forceOpen` (por ejemplo, si cambia el
  // hash de la URL a un enlace dentro de esta sección). Un clic fija su propia preferencia.
  const [userOverride, setUserOverride] = useState<boolean | null>(null)
  const open = userOverride ?? forceOpen

  return (
    <section className="manual-section">
      <h2 id={section.id} className="manual-heading manual-section-heading">
        <button
          type="button"
          className="manual-section-toggle"
          aria-expanded={open}
          onClick={() => setUserOverride(!open)}
        >
          <ChevronRight size={16} aria-hidden="true" className="manual-section-chevron" />
          {section.title}
        </button>
        <ManualAnchorButton path={`/manual/${chapterId}#${section.id}`} />
      </h2>
      <div className="manual-section-body" hidden={!open}>
        <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>{section.body}</ReactMarkdown>
      </div>
    </section>
  )
}
