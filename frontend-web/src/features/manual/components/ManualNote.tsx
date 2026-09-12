import type { ReactNode } from 'react'
import { Info } from 'lucide-react'

/** Toda cita `>` del contenido se muestra como una nota/advertencia destacada, no como una cita textual. */
export function ManualNote({ children }: { children?: ReactNode }) {
  return (
    <blockquote className="manual-note">
      <Info size={16} aria-hidden="true" className="manual-note-icon" />
      <div>{children}</div>
    </blockquote>
  )
}
