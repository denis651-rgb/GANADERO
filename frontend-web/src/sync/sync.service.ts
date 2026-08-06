import { db } from '@/offline/db'
import { http } from '@/shared/api/http'
import { normalizeApiError } from '@/shared/api/errors'

export async function synchronizePendingOperations() {
  const pending = await db.operacionesPendientes.where('status').anyOf('PENDING', 'ERROR').sortBy('createdAt')
  const results: Array<{ operationId: string; success: boolean; message?: string }> = []

  for (const operation of pending) {
    if (!operation.id) continue
    await db.operacionesPendientes.update(operation.id, {
      status: 'SYNCING',
      attempts: operation.attempts + 1,
      updatedAt: new Date().toISOString(),
    })
    try {
      await http.request({ method: operation.method, url: operation.url, data: operation.body, headers: { 'Idempotency-Key': operation.operationId } })
      await db.operacionesPendientes.update(operation.id, { status: 'SYNCED', updatedAt: new Date().toISOString(), lastError: undefined })
      results.push({ operationId: operation.operationId, success: true })
    } catch (reason) {
      const error = normalizeApiError(reason)
      const status = error.status === 409 ? 'CONFLICT' : 'ERROR'
      await db.operacionesPendientes.update(operation.id, { status, updatedAt: new Date().toISOString(), lastError: error.message })
      results.push({ operationId: operation.operationId, success: false, message: error.message })
    }
  }

  await db.estadoSincronizacion.put({ key: 'lastSyncAt', value: new Date().toISOString(), updatedAt: new Date().toISOString() })
  return results
}
