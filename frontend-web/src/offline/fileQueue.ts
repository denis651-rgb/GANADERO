import { db } from '@/offline/db'
import { createUuid } from '@/shared/utils/uuid'

export interface EnqueueFileInput {
  entityType: string
  entityId: string
  file: Blob
  fileName: string
  mimeType: string
  principal: boolean
}

/**
 * Encola un archivo para subirse cuando haya conexión (Tarea 9.5).
 *
 * - Asocia el archivo con el UUID de la entidad (p.ej. el animal).
 * - Previene duplicados: si ya hay un archivo pendiente con el mismo
 *   nombre y tamaño para la misma entidad, no se encola de nuevo.
 * - El `localId` es único en IndexedDB (`&localId`), por lo que el mismo
 *   archivo nunca se registra dos veces.
 */
export async function enqueueFile(input: EnqueueFileInput): Promise<string> {
  const existing = await db.archivosPendientes
    .where('entityId')
    .equals(input.entityId)
    .filter(
      (pending) =>
        pending.fileName === input.fileName &&
        pending.file.size === input.file.size &&
        pending.status !== 'SYNCED',
    )
    .first()

  if (existing) return existing.localId

  const now = new Date().toISOString()
  const localId = createUuid()
  await db.archivosPendientes.add({
    localId,
    entityType: input.entityType,
    entityId: input.entityId,
    file: input.file,
    fileName: input.fileName,
    mimeType: input.mimeType,
    principal: input.principal,
    status: 'PENDING',
    attempts: 0,
    progress: 0,
    createdAt: now,
    updatedAt: now,
  })
  return localId
}
