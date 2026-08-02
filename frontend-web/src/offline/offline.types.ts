export type PendingStatus = 'PENDING' | 'SYNCING' | 'SYNCED' | 'ERROR' | 'CONFLICT'

export interface LocalCatalog {
  id: string
  type: string
  code: string
  name: string
  version: number
  updatedAt: string
}

export interface LocalAnimalSummary {
  id: string
  code: string
  primaryIdentifier?: string
  name?: string
  sex: string
  category?: string
  propertyId: string
  paddockId: string
  lotId?: string
  status: string
  version: number
  updatedAt: string
}

export interface LocalLot {
  id: string
  propertyId: string
  code: string
  name: string
  status: string
  version: number
  updatedAt: string
}

export interface LocalPaddock {
  id: string
  propertyId: string
  code: string
  name: string
  status: string
  version: number
  updatedAt: string
}

export interface PendingOperation {
  id?: number
  operationId: string
  type: string
  entityType: string
  entityId?: string
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  url: string
  body: unknown
  status: PendingStatus
  attempts: number
  createdAt: string
  updatedAt: string
  lastError?: string
}

export interface PendingFile {
  id?: number
  localId: string
  entityType: string
  entityId: string
  file: Blob
  fileName: string
  mimeType: string
  status: PendingStatus
  createdAt: string
  lastError?: string
}

export interface SyncState {
  key: string
  value: string
  updatedAt: string
}
