import Dexie, { type EntityTable } from 'dexie'
import type {
  LocalAnimalSummary,
  LocalCatalog,
  LocalIdentifier,
  LocalLot,
  LocalPaddock,
  PendingFile,
  PendingOperation,
  PendingStatus,
  SyncState,
} from '@/offline/offline.types'

interface LegacyPendingOperation {
  id?: number
  operationId: string
  type: string
  entityType: string
  entityId?: string
  body?: unknown
  status: string
  attempts: number
  createdAt: string
  updatedAt: string
  lastError?: string
  idempotencyKey?: string
}

const LEGACY_TIPO: Record<string, string> = {
  CREATE_ANIMAL: 'ANIMAL_CREAR',
  REGISTER_PESAJE: 'PESAJE_REGISTRAR',
  REGISTER_PESAJE_LOTE: 'PESAJE_LOTE',
}

function legacyStatus(status: string): PendingStatus {
  switch (status) {
    case 'SYNCING':
      return 'PROCESSING'
    case 'ERROR':
      return 'RETRYABLE'
    case 'PENDING':
      return 'PENDING'
    case 'SYNCED':
      return 'SYNCED'
    case 'CONFLICT':
      return 'CONFLICT'
    default:
      return 'RETRYABLE'
  }
}

class GanaderoDatabase extends Dexie {
  catalogos!: EntityTable<LocalCatalog, 'id'>
  animalesResumen!: EntityTable<LocalAnimalSummary, 'id'>
  identificadores!: EntityTable<LocalIdentifier, 'id'>
  lotes!: EntityTable<LocalLot, 'id'>
  potreros!: EntityTable<LocalPaddock, 'id'>
  operacionesPendientes!: EntityTable<PendingOperation, 'id'>
  archivosPendientes!: EntityTable<PendingFile, 'id'>
  estadoSincronizacion!: EntityTable<SyncState, 'key'>

  constructor() {
    super('ganadero-local')
    this.version(1).stores({
      catalogos: 'id, type, code, updatedAt',
      animalesResumen: 'id, code, primaryIdentifier, propertyId, paddockId, lotId, status, updatedAt',
      identificadores: 'id, animalId, tipo, estado, principal, updatedAt',
      lotes: 'id, propertyId, code, status, updatedAt',
      potreros: 'id, propertyId, code, status, updatedAt',
      operacionesPendientes: '++id, &operationId, status, type, entityType, entityId, createdAt',
      archivosPendientes: '++id, &localId, status, entityType, entityId, createdAt',
      estadoSincronizacion: 'key, updatedAt',
    })
    this.version(2).stores({
      catalogos: 'id, type, code, updatedAt',
      animalesResumen: 'id, code, primaryIdentifier, propertyId, paddockId, lotId, status, updatedAt',
      identificadores: 'id, animalId, tipo, estado, principal, updatedAt',
      lotes: 'id, propertyId, code, status, updatedAt',
      potreros: 'id, propertyId, code, status, updatedAt',
      operacionesPendientes: '++id, &operationId, status, tipo, entidad, entidadId, createdAt',
      archivosPendientes: '++id, &localId, status, entityType, entityId, createdAt',
      estadoSincronizacion: 'key, updatedAt',
    }).upgrade(async (tx) => {
      const table = tx.table<PendingOperation & LegacyPendingOperation, number>('operacionesPendientes')
      await table.toCollection().modify((operation) => {
        if (operation.tipo) return
        operation.tipo = LEGACY_TIPO[operation.type] ?? operation.type ?? 'OPERACION'
        operation.entidad = operation.entityType ?? ''
        operation.entidadId = operation.entityId
        operation.idempotencyKey = operation.idempotencyKey ?? operation.operationId
        operation.payloadHash = undefined
        operation.datos = operation.body as Record<string, unknown> | undefined
        operation.status = legacyStatus(operation.status)
        operation.nextRetryAt = undefined
        operation.versionCliente = undefined
      })
    })
  }
}

export const db = new GanaderoDatabase()
