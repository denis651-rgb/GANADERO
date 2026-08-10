import type { CSSProperties } from 'react'
import { cn } from '@/shared/utils/cn'

export function Skeleton({ className, style }: { className?: string; style?: CSSProperties }) {
  return <div className={cn('skeleton', className)} style={style} aria-hidden="true" />
}

export function TableSkeleton({ rows = 6, columns = 6 }: { rows?: number; columns?: number }) {
  return (
    <div className="table-wrapper" role="status" aria-live="polite">
      <span className="sr-only">Cargando contenido…</span>
      <table className="skeleton-table" aria-hidden="true">
        <thead>
          <tr>{Array.from({ length: columns }, (_, i) => <th key={i}><Skeleton className="skeleton-cell" style={{ width: '70%' }} /></th>)}</tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }, (_, r) => (
            <tr key={r}>{Array.from({ length: columns }, (_, c) => <td key={c}><Skeleton className="skeleton-cell" style={{ width: `${92 - ((r * 13 + c * 17) % 48)}%` }} /></td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
