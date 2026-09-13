import { Link } from 'react-router'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { ManualChapter } from '@/features/manual/manualRegistry'

interface ManualNavigationProps {
  previous: ManualChapter | undefined
  next: ManualChapter | undefined
}

export function ManualNavigation({ previous, next }: ManualNavigationProps) {
  if (!previous && !next) return null
  return (
    <div className="manual-navigation">
      {previous
        ? <Link className="manual-nav-link" to={`/manual/${previous.id}`}>
            <span className="manual-nav-label"><ChevronLeft size={14} aria-hidden="true" /> Capítulo anterior</span>
            <span className="manual-nav-title">{previous.title}</span>
          </Link>
        : <span />}
      {next &&
        <Link className="manual-nav-link manual-nav-link-next" to={`/manual/${next.id}`}>
          <span className="manual-nav-label">Capítulo siguiente <ChevronRight size={14} aria-hidden="true" /></span>
          <span className="manual-nav-title">{next.title}</span>
        </Link>}
    </div>
  )
}
