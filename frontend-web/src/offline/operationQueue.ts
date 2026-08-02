import { db } from '@/offline/db'
import type { PendingOperation } from '@/offline/offline.types'
import { createUuid } from '@/shared/utils/uuid'

interface QueueHttpOperationInput {
  type: string
  entityType: string
  entityId?: string
  method: PendingOperation['method']
  url: string
  body: unknown
}

export async function queueHttpOperation(input: QueueHttpOperationInput) {
  const now = new Date().toISOString()
  const operation: PendingOperation = {
    ...input,
    operationId: createUuid(),
    status: 'PENDING',
    attempts: 0,
    createdAt: now,
    updatedAt: now,
  }
  return db.operacionesPendientes.add(operation)
}
