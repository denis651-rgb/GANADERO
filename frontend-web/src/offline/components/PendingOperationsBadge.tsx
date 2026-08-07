import { useLiveQuery } from 'dexie-react-hooks'
import { Link } from 'react-router'
import { RefreshCw } from 'lucide-react'
import { db } from '@/offline/db'

export function PendingOperationsBadge() {
  const count = useLiveQuery(() => db.operacionesPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'CONFLICT', 'REJECTED', 'ERROR').count(), [], 0)
  return (
    <Link className="pending-pill" to="/sincronizacion" aria-label={`${count ?? 0} operaciones pendientes`}>
      <RefreshCw size={15} />
      <span>{count ?? 0}</span>
    </Link>
  )
}
