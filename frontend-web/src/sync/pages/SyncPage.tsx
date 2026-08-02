import { useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { RefreshCw, Trash2 } from 'lucide-react'
import { db } from '@/offline/db'
import { synchronizePendingOperations } from '@/sync/sync.service'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function SyncPage() {
  const operations = useLiveQuery(() => db.operacionesPendientes.orderBy('createdAt').reverse().toArray(), [], [])
  const lastSync = useLiveQuery(() => db.estadoSincronizacion.get('lastSyncAt'))
  const online = useOnlineStatus()
  const [syncing, setSyncing] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  async function sync() {
    setMessage(null)
    setSyncing(true)
    const results = await synchronizePendingOperations()
    const failed = results.filter((item) => !item.success).length
    setMessage(failed ? `${failed} operación(es) requieren revisión.` : 'Sincronización finalizada correctamente.')
    setSyncing(false)
  }

  async function clearSynced() {
    await db.operacionesPendientes.where('status').equals('SYNCED').delete()
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Fase 2 preparada"
        title="Sincronización"
        description={`Última sincronización: ${lastSync?.value ? new Date(lastSync.value).toLocaleString('es-BO') : 'todavía no realizada'}`}
        actions={<Button onClick={() => void sync()} loading={syncing} disabled={!online}><RefreshCw size={18} />Sincronizar</Button>}
      />
      {!online && <Alert tone="info">Conecta el dispositivo a internet para enviar las operaciones pendientes.</Alert>}
      {message && <Alert tone="info">{message}</Alert>}
      <Card>
        <div className="section-heading"><h3>Cola local</h3><Button variant="ghost" onClick={() => void clearSynced()}><Trash2 size={17} />Limpiar sincronizadas</Button></div>
        {!operations?.length ? (
          <EmptyState title="No hay operaciones locales" description="Las operaciones realizadas sin conexión aparecerán aquí." />
        ) : (
          <div className="operation-list">
            {operations.map((operation) => (
              <article key={operation.operationId} className="operation-item">
                <div><strong>{operation.type}</strong><span>{operation.method} {operation.url}</span></div>
                <div className="operation-meta"><span className={`status-badge status-${operation.status.toLowerCase()}`}>{operation.status}</span><small>{new Date(operation.createdAt).toLocaleString('es-BO')}</small></div>
                {operation.lastError && <p className="operation-error">{operation.lastError}</p>}
              </article>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
