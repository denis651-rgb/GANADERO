import Dexie, { type EntityTable } from 'dexie'
import type {
  LocalAnimalSummary,
  LocalCatalog,
  LocalLot,
  LocalPaddock,
  PendingFile,
  PendingOperation,
  SyncState,
} from '@/offline/offline.types'

class GanaderoDatabase extends Dexie {
  catalogos!: EntityTable<LocalCatalog, 'id'>
  animalesResumen!: EntityTable<LocalAnimalSummary, 'id'>
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
      lotes: 'id, propertyId, code, status, updatedAt',
      potreros: 'id, propertyId, code, status, updatedAt',
      operacionesPendientes: '++id, &operationId, status, type, entityType, entityId, createdAt',
      archivosPendientes: '++id, &localId, status, entityType, entityId, createdAt',
      estadoSincronizacion: 'key, updatedAt',
    })
  }
}

export const db = new GanaderoDatabase()
