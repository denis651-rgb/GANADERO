import { useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Check, RefreshCw, Trash2, Upload } from 'lucide-react'
import { db } from '@/offline/db'
import {
  pullChanges,
  resolveConflictAcceptServer,
  resolveConflictKeepLocal,
  synchronizePendingFiles,
  synchronizePendingOperations,
} from '@/sync/sync.service'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
import { useAuth } from '@/auth/auth-context'

export function SyncPage() {
  const { sessionExpired } = useAuth()
  const operations = useLiveQuery(() => db.operacionesPendientes.orderBy('createdAt').reverse().toArray(), [], [])
  const pendingFiles = useLiveQuery(() => db.archivosPendientes.orderBy('createdAt').reverse().toArray(), [], [])
  const identifiersCount = useLiveQuery(() => db.identificadores.count(), [], 0)
  const lastSync = useLiveQuery(() => db.estadoSincronizacion.get('lastSyncAt'))
  const online = useOnlineStatus()
  const [syncing, setSyncing] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [syncError, setSyncError] = useState<string | null>(null)

  async function sync() {
    setMessage(null)
    setSyncError(null)
    setSyncing(true)
    try {
      const [operationsResult, filesResult] = await Promise.all([synchronizePendingOperations(), synchronizePendingFiles()])
      const { permisosActualizados } = await pullChanges()
      const failed = operationsResult.filter((item) => !item.success).length + filesResult.filter((item) => !item.success).length
      const avisoPermisos = permisosActualizados ? ' Se actualizaron los permisos del usuario.' : ''
      setMessage(failed ? `${failed} operación(es) requieren revisión.${avisoPermisos}` : `Sincronización finalizada correctamente.${avisoPermisos}`)
    } catch (reason) {
      setSyncError(reason instanceof Error ? reason.message : 'No se pudo completar la sincronización.')
    } finally {
      setSyncing(false)
    }
  }

  async function retryFile(localId: string) {
    const pending = await db.archivosPendientes.where('localId').equals(localId).first()
    if (!pending?.id) return
    await db.archivosPendientes.update(pending.id, { status: 'PENDING', progress: 0, lastError: undefined, nextRetryAt: undefined, updatedAt: new Date().toISOString() })
  }

  async function retryOperation(operationId: string) {
    const pending = await db.operacionesPendientes.where('operationId').equals(operationId).first()
    if (!pending?.id) return
    await db.operacionesPendientes.update(pending.id, { status: 'PENDING', lastError: undefined, nextRetryAt: undefined, updatedAt: new Date().toISOString() })
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
        actions={<Button onClick={() => void sync()} loading={syncing} disabled={!online || sessionExpired}><RefreshCw size={18} />Sincronizar</Button>}
      />
      {sessionExpired && <Alert tone="danger">La sesión expiró. Reautentícate para sincronizar; tus operaciones pendientes se conservan.</Alert>}
      {!online && <Alert tone="info">Conecta el dispositivo a internet para enviar las operaciones pendientes.</Alert>}
      {syncError && <Alert tone="danger">{syncError}</Alert>}
      {message && <Alert tone="info">{message}</Alert>}
      <Alert tone="info">El escáner de QR resuelve códigos sin conexión usando {identifiersCount} identificador(es) locales sincronizados.</Alert>
      <Card>
        <div className="section-heading"><h3>Cola local</h3><Button variant="ghost" onClick={() => void clearSynced()}><Trash2 size={17} />Limpiar sincronizadas</Button></div>
        {!operations?.length ? (
          <EmptyState title="No hay operaciones locales" description="Las operaciones realizadas sin conexión aparecerán aquí." />
        ) : (
          <div className="operation-list">
            {operations.map((operation) => (
              <article key={operation.operationId} className="operation-item">
                <div><strong>{operation.tipo}</strong><span>{operation.entidad}{operation.entidadId ? ` · ${operation.entidadId}` : ''} · {operation.attempts} intento(s)</span></div>
                <div className="operation-meta">
                  <span className={`status-badge status-${operation.status.toLowerCase()}`}>{operation.status}</span>
                  <small>{new Date(operation.createdAt).toLocaleString('es-BO')}</small>
                  {['RETRYABLE', 'CONFLICT', 'REJECTED'].includes(operation.status) && (
                    <Button variant="ghost" onClick={() => void retryOperation(operation.operationId)}><RefreshCw size={14} />Reintentar</Button>
                  )}
                </div>
                {operation.lastError && <p className="operation-error">{operation.lastError}</p>}
                {operation.nextRetryAt && <p className="operation-meta"><small>Reintento desde {new Date(operation.nextRetryAt).toLocaleString('es-BO')}</small></p>}
                {operation.status === 'CONFLICT' && (
                  <div className="operation-conflict">
                    <p><small>El servidor tiene una versión distinta (v{operation.versionServidor ?? 0}) de esta entidad{operation.conflictos?.length ? `; campos: ${operation.conflictos.join(', ')}` : ''}. Resuelve eligiendo qué versión conservar.</small></p>
                    {operation.datosServidor != null && (
                      <details className="operation-conflict-details">
                        <summary>Ver datos del servidor</summary>
                        <pre>{JSON.stringify(operation.datosServidor, null, 2)}</pre>
                      </details>
                    )}
                    <div className="operation-actions">
                      <Button variant="ghost" onClick={() => void resolveConflictAcceptServer(operation.operationId)}><Check size={14} />Aceptar servidor</Button>
                      <Button variant="ghost" onClick={() => void resolveConflictKeepLocal(operation.operationId)}><Upload size={14} />Conservar la local</Button>
                    </div>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </Card>
      <Card>
        <div className="section-heading"><h3>Archivos pendientes</h3></div>
        {!pendingFiles?.length ? (
          <EmptyState title="No hay archivos locales" description="Las fotos tomadas sin conexión aparecerán aquí hasta subirse." />
        ) : (
          <div className="operation-list">
            {pendingFiles.map((file) => (
              <article key={file.localId} className="operation-item">
                <div><strong>{file.fileName}</strong><span>{file.entityType} {file.entityId} · {(file.file.size / 1024).toFixed(1)} KB</span></div>
                <div className="operation-meta">
                  <span className={`status-badge status-${file.status.toLowerCase()}`}>{file.status}</span>
                  {file.status === 'PROCESSING' && <small>{file.progress}%</small>}
                  <small>{new Date(file.createdAt).toLocaleString('es-BO')}</small>
                  {file.status === 'RETRYABLE' && (
                    <Button variant="ghost" onClick={() => void retryFile(file.localId)}><RefreshCw size={14} />Reintentar</Button>
                  )}
                </div>
                {file.lastError && <p className="operation-error">{file.lastError}</p>}
              </article>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
