import { useEffect } from 'react'
import { useLocation, useParams } from 'react-router'
import { EmptyState } from '@/shared/components/EmptyState'
import { ManualHeader } from '@/features/manual/components/ManualHeader'
import { ManualSidebar } from '@/features/manual/components/ManualSidebar'
import { ManualViewer } from '@/features/manual/components/ManualViewer'
import { ManualNavigation } from '@/features/manual/components/ManualNavigation'
import { getAdjacentChapters, getManualChapter, manualChapters } from '@/features/manual/manualRegistry'
import '@/features/manual/manual.css'

export function ManualPage() {
  const { chapterId } = useParams()
  const location = useLocation()
  const chapter = getManualChapter(chapterId)

  /** React Router no hace scroll a la ancla al navegar entre capítulos (solo en una carga completa de página). */
  useEffect(() => {
    if (!chapter) return
    if (!location.hash) {
      window.scrollTo({ top: 0 })
      return
    }
    const target = document.getElementById(decodeURIComponent(location.hash.slice(1)))
    target?.scrollIntoView?.({ block: 'start' })
  }, [chapter, location.hash])

  return (
    <div className="page-stack">
      <ManualHeader />
      <div className="manual-layout">
        <ManualSidebar activeChapterId={chapter?.id ?? manualChapters[0].id} />
        <div className="manual-content">
          {chapter
            ? <ManualViewer chapter={chapter} />
            : <EmptyState title="Capítulo no encontrado" description="Elige un capítulo del índice de la izquierda." />}
          {chapter && <ManualNavigation {...getAdjacentChapters(chapter.id)} />}
        </div>
      </div>
    </div>
  )
}
