import { useEffect, useState } from 'react'
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
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
import { useAuth } from '@/auth/auth-context'
import type { PendingOperation } from '@/offline/offline.types'
import { getOfflinePreparationStatus, prepareOfflineData, type OfflinePreparationStatus } from '@/offline/offlinePreparation'

const statusLabel: Record<string, string> = {
  PENDING: 'Pendiente',
  PROCESSING: 'Procesando',
  SYNCED: 'Sincronizada',
  CONFLICT: 'Conflicto',
  REJECTED: 'Rechazada',
  RETRYABLE: 'Reintentable',
}

export function SyncPage() {
  const { sessionExpired } = useAuth()
  const operations = useLiveQuery(() => db.operacionesPendientes.orderBy('createdAt').reverse().toArray(), [], [])
  const pendingFiles = useLiveQuery(() => db.archivosPendientes.orderBy('createdAt').reverse().toArray(), [], [])
  const identifiersCount = useLiveQuery(() => db.identificadores.count(), [], 0)
  const lastSync = useLiveQuery(() => db.estadoSincronizacion.get('lastSyncAt'))
  const online = useOnlineStatus()
  const [syncing, setSyncing] = useState(false)
  const [confirmClear, setConfirmClear] = useState(false)
  const [conflictTarget, setConflictTarget] = useState<{ operation: PendingOperation; resolution: 'server' | 'local' } | null>(null)
  const [resolvingConflict, setResolvingConflict] = useState(false)
  const [conflictError, setConflictError] = useState<unknown>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [syncError, setSyncError] = useState<string | null>(null)
  const [preparingOffline, setPreparingOffline] = useState(false)
  const [preparationStatus, setPreparationStatus] = useState<OfflinePreparationStatus | null>(null)

  useEffect(() => {
    void refreshPreparationStatus()
  }, [])

  async function refreshPreparationStatus() {
    setPreparationStatus(await getOfflinePreparationStatus())
  }

  async function prepareDevice() {
    setSyncError(null)
    setMessage(null)
    setPreparingOffline(true)
    try {
      await prepareOfflineData({ force: true })
      await refreshPreparationStatus()
      setMessage('Dispositivo preparado para trabajar sin conexión.')
    } catch (reason) {
      setSyncError(reason instanceof Error ? reason.message : 'No se pudieron preparar los datos offline.')
    } finally {
      setPreparingOffline(false)
    }
  }

  async function sync() {
    setMessage(null)
    setSyncError(null)
    setSyncing(true)
    try {
      await prepareOfflineData()
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

  async function confirmConflictResolution() {
    if (!conflictTarget || resolvingConflict) return
    setResolvingConflict(true)
    setConflictError(null)
    try {
      if (conflictTarget.resolution === 'server') {
        await resolveConflictAcceptServer(conflictTarget.operation.operationId)
      } else {
        await resolveConflictKeepLocal(conflictTarget.operation.operationId)
      }
      setConflictTarget(null)
    } catch (reason) {
      setConflictError(reason)
    } finally {
      setResolvingConflict(false)
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Offline-first"
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
        <div className="section-heading"><div><h3>Preparación offline</h3><p className="muted">Descarga razas, categorías, propiedades, potreros, animales e identificadores en este dispositivo.</p></div><Button variant="secondary" loading={preparingOffline} disabled={!online || sessionExpired} onClick={() => void prepareDevice()}>Preparar datos offline</Button></div>
        {preparationStatus && <div className="offline-data-grid"><div><span>Razas</span><strong>{preparationStatus.breeds}</strong></div><div><span>Categorías</span><strong>{preparationStatus.categories}</strong></div><div><span>Propiedades</span><strong>{preparationStatus.properties}</strong></div><div><span>Potreros</span><strong>{preparationStatus.paddocks}</strong></div></div>}
        <Button variant="ghost" onClick={() => void refreshPreparationStatus()}>Verificar datos guardados</Button>
      </Card>
      <Card>
        <div className="section-heading"><h3>Cola local</h3><Button variant="ghost" onClick={() => setConfirmClear(true)}><Trash2 size={17} />Limpiar sincronizadas</Button></div>
        {!operations?.length ? (
          <EmptyState title="No hay operaciones locales" description="Las operaciones realizadas sin conexión aparecerán aquí." />
        ) : (
          <div className="operation-list">
            {operations.map((operation) => (
              <article key={operation.operationId} className="operation-item">
                <div><strong>{operation.tipo}</strong><span>{operation.entidad}{operation.entidadId ? ` · ${operation.entidadId}` : ''} · {operation.attempts} intento(s)</span></div>
                <div className="operation-meta">
                  <span className={`status-badge status-${operation.status.toLowerCase()}`}>{statusLabel[operation.status] ?? operation.status}</span>
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
                      <Button variant="ghost" onClick={() => { setConflictError(null); setConflictTarget({ operation, resolution: 'server' }) }}><Check size={14} />Usar versión servidor</Button>
                      <Button variant="ghost" onClick={() => { setConflictError(null); setConflictTarget({ operation, resolution: 'local' }) }}><Upload size={14} />Conservar versión local</Button>
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
                  <span className={`status-badge status-${file.status.toLowerCase()}`}>{statusLabel[file.status] ?? file.status}</span>
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

      <ConfirmDialog
        open={confirmClear}
        title="Limpiar sincronizadas"
        confirmLabel="Limpiar sincronizadas"
        confirmIcon={<Trash2 size={17} />}
        onClose={() => setConfirmClear(false)}
        onConfirm={() => { setConfirmClear(false); void clearSynced() }}
      >
        <p className="muted">¿Eliminar las operaciones ya sincronizadas de la cola local? Esta acción no se puede deshacer, pero no afecta a los datos ya enviados al servidor.</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={Boolean(conflictTarget)}
        title={conflictTarget?.resolution === 'local' ? 'Conservar versión local' : 'Usar versión del servidor'}
        confirmLabel={conflictTarget?.resolution === 'local' ? 'Conservar versión local' : 'Usar versión del servidor'}
        variant={conflictTarget?.resolution === 'server' ? 'danger' : 'warning'}
        loading={resolvingConflict}
        error={conflictError}
        onClose={() => { setConflictError(null); setConflictTarget(null) }}
        onConfirm={() => void confirmConflictResolution()}
      >
        {conflictTarget && <div className="page-stack">
          <p className="muted">{conflictTarget.resolution === 'local'
            ? 'Se intentará conservar la información registrada en este dispositivo según el flujo de resolución actual.'
            : 'La información local de esta operación será descartada.'}</p>
          <div className="two-column-grid">
            {conflictTarget.operation.datos != null && <div><h3>Información del dispositivo</h3><pre className="code-block">{JSON.stringify(conflictTarget.operation.datos, null, 2)}</pre></div>}
            {conflictTarget.operation.datosServidor != null && <div><h3>Información del servidor</h3><pre className="code-block">{JSON.stringify(conflictTarget.operation.datosServidor, null, 2)}</pre></div>}
          </div>
          {conflictTarget.resolution === 'local' && <Alert tone="danger">La versión del servidor puede ser reemplazada o descartada según esta resolución.</Alert>}
        </div>}
      </ConfirmDialog>
    </div>
  )
}
