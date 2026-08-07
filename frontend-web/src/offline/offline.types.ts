export type PendingStatus = 'PENDING' | 'PROCESSING' | 'SYNCED' | 'CONFLICT' | 'REJECTED' | 'RETRYABLE'

export interface LocalCatalog {
  id: string
  type: string
  code: string
  name: string
  activo?: boolean
  propiedadId?: string
  estado?: string
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

export interface LocalIdentifier {
  id: string
  animalId: string
  tipo: string
  valor: string
  principal: boolean
  estado: string
  payload?: string
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
  tipo: string
  entidad: string
  entidadId?: string
  versionCliente?: number
  idempotencyKey: string
  payloadHash?: string
  datos?: Record<string, unknown>
  status: PendingStatus
  attempts: number
  createdAt: string
  updatedAt: string
  lastError?: string
  nextRetryAt?: string
  datosServidor?: unknown
  versionServidor?: number
  conflictos?: string[]
}

export interface PendingFile {
  id?: number
  localId: string
  entityType: string
  entityId: string
  file: Blob
  fileName: string
  mimeType: string
  principal: boolean
  status: PendingStatus
  attempts: number
  progress: number
  createdAt: string
  updatedAt: string
  lastError?: string
  nextRetryAt?: string
}

export interface SyncState {
  key: string
  value: string
  updatedAt: string
}
