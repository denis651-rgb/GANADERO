import { useState } from 'react'
import { Link } from 'react-router'
import { List } from 'lucide-react'
import { manualChapters } from '@/features/manual/manualRegistry'
import { Button } from '@/shared/components/Button'
import { Modal } from '@/shared/components/Modal'

interface ManualSidebarProps {
  activeChapterId: string
}

function ChapterLinks({ activeChapterId, onNavigate }: { activeChapterId: string; onNavigate?: () => void }) {
  return manualChapters.map((chapter) => (
    <Link
      key={chapter.id}
      className="manual-sidebar-link"
      to={`/manual/${chapter.id}`}
      aria-current={chapter.id === activeChapterId ? 'page' : undefined}
      onClick={onNavigate}
    >
      {chapter.title}
    </Link>
  ))
}

export function ManualSidebar({ activeChapterId }: ManualSidebarProps) {
  const [open, setOpen] = useState(false)

  return (
    <div className="manual-sidebar-column">
      <nav className="manual-sidebar" aria-label="Capítulos del manual">
        <ChapterLinks activeChapterId={activeChapterId} />
      </nav>

      <Button type="button" variant="secondary" className="manual-index-trigger" onClick={() => setOpen(true)} aria-haspopup="dialog">
        <List size={16} aria-hidden="true" />
        Índice del manual
      </Button>
      <Modal open={open} title="Índice del manual" onClose={() => setOpen(false)} variant="drawer">
        <nav className="mobile-drawer-nav manual-drawer-nav" aria-label="Capítulos del manual">
          <ChapterLinks activeChapterId={activeChapterId} onNavigate={() => setOpen(false)} />
        </nav>
      </Modal>
    </div>
  )
}
