import { db } from '@/offline/db'
import type { PendingOperation } from '@/offline/offline.types'
import { payloadHash } from '@/offline/payloadHash'
import { createUuid } from '@/shared/utils/uuid'

export interface QueueSyncOperationInput {
  tipo: string
  entidad: string
  entidadId?: string
  versionCliente?: number
  idempotencyKey?: string
  datos?: Record<string, unknown>
}

export async function queueSyncOperation(input: QueueSyncOperationInput): Promise<number | undefined> {
  const now = new Date().toISOString()
  const operationId = createUuid()
  const operation: PendingOperation = {
    operationId,
    tipo: input.tipo,
    entidad: input.entidad,
    entidadId: input.entidadId,
    versionCliente: input.versionCliente,
    idempotencyKey: input.idempotencyKey ?? operationId,
    payloadHash: await payloadHash(input.datos),
    datos: input.datos,
    status: 'PENDING',
    attempts: 0,
    createdAt: now,
    updatedAt: now,
  }
  return db.operacionesPendientes.add(operation)
}
