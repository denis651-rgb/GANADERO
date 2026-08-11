import type { ReactNode } from 'react'

interface MobileEntityCardProps {
  title: ReactNode
  subtitle?: ReactNode
  status?: ReactNode
  metadata?: ReactNode
  action?: ReactNode
  selection?: ReactNode
}

export function MobileEntityCard({ title, subtitle, status, metadata, action, selection }: MobileEntityCardProps) {
  return (
    <article className="mobile-entity-card">
      <div className="mobile-entity-heading">
        <div className="mobile-entity-title">{selection}{title}</div>
        {status}
      </div>
      {subtitle && <div className="mobile-entity-subtitle">{subtitle}</div>}
      {metadata && <div className="mobile-entity-meta">{metadata}</div>}
      {action && <div className="mobile-entity-actions">{action}</div>}
    </article>
  )
}
