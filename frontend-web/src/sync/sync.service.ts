import { db } from '@/offline/db'
import { http, SessionExpiredError, getAccessToken } from '@/shared/api/http'
import { isAccessTokenExpired } from '@/auth/session'
import { normalizeApiError } from '@/shared/api/errors'
import { createUuid } from '@/shared/utils/uuid'
import type { LocalAnimalSummary, LocalCatalog, LocalIdentifier, PendingStatus } from '@/offline/offline.types'

const DISPOSITIVO = 'web-app'

async function assertSessionActive() {
  const token = await getAccessToken()
  if (token && isAccessTokenExpired(token)) {
    throw new SessionExpiredError()
  }
}

interface BootstrapResponse {
  dispositivoId: string
  cursor: number
  propiedades?: Array<Record<string, unknown>>
  potreros?: Array<Record<string, unknown>>
  lotes?: Array<Record<string, unknown>>
  razas?: Array<Record<string, unknown>>
  categorias?: Array<Record<string, unknown>>
  tiposPasto?: Array<Record<string, unknown>>
  animales?: Array<Record<string, unknown>>
  identificadores?: Array<Record<string, unknown>>
  usuario?: { userId: string; roles: string[]; permisos: string[] }
}

interface OperacionResultado {
  clienteId: string
  estado: string
  entidadId?: string
  versionServidor?: number
  datosServidor?: unknown
  errorCode?: string
  errorMessage?: string
  conflictos?: string[]
}

interface SyncPushResponse {
  dispositivoId: string
  nuevoCursor: number
  resultados: OperacionResultado[]
}

export interface SyncOperationResult {
  operationId: string
  success: boolean
  status: PendingStatus
  message?: string
}

function stringValue(value: unknown): string | undefined {
  if (value === null || value === undefined) return undefined
  return String(value)
}

function toLocalCatalog(row: Record<string, unknown>, type: string, updatedAt: string): LocalCatalog {
  return {
    id: stringValue(row.id) ?? '',
    type,
    code: stringValue(row.codigo) ?? '',
    name: stringValue(row.nombre) ?? '',
    activo: row.activo === undefined ? true : Boolean(row.activo),
    propiedadId: stringValue(row.propiedadId),
    estado: stringValue(row.estado),
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

function backoffDelay(attempts: number): number {
  const seconds = Math.min(3600, Math.pow(2, attempts) * 60)
  return seconds * 1000
}

function toPendingStatus(estado: string): PendingStatus {
  switch (estado) {
    case 'SYNCED':
    case 'CONFLICT':
    case 'REJECTED':
    case 'RETRYABLE':
      return estado
    default:
      return 'RETRYABLE'
  }
}

export async function synchronizePendingOperations(): Promise<SyncOperationResult[]> {
  await assertSessionActive()
  const pending = await db.operacionesPendientes
    .where('status')
    .anyOf('PENDING', 'RETRYABLE')
    .sortBy('createdAt')
  const results: SyncOperationResult[] = []

  for (const operation of pending) {
    if (!operation.id) continue
    const attempts = operation.attempts + 1
    await db.operacionesPendientes.update(operation.id, {
      status: 'PROCESSING',
      attempts,
      updatedAt: new Date().toISOString(),
    })
    try {
      const response = await http.post<{ data: SyncPushResponse }>('/api/v1/sync/push', {
        dispositivo: { codigo: DISPOSITIVO, nombre: 'Web', plataforma: 'WEB', versionApp: '1.0.0' },
        operaciones: [
          {
            clienteId: operation.operationId,
            tipo: operation.tipo,
            entidad: operation.entidad,
            entidadId: operation.entidadId,
            versionCliente: operation.versionCliente,
            idempotencyKey: operation.idempotencyKey,
            datos: operation.datos,
          },
        ],
      })
      const resultado = response.data.data.resultados[0]
      if (!resultado) throw new Error('Respuesta de sincronización vacía')
      const status = toPendingStatus(resultado.estado)
      const conflicting = status === 'CONFLICT'
      await db.operacionesPendientes.update(operation.id, {
        status,
        entidadId: resultado.entidadId ?? operation.entidadId,
        updatedAt: new Date().toISOString(),
        lastError: resultado.errorMessage,
        datosServidor: conflicting ? resultado.datosServidor : undefined,
        versionServidor: conflicting ? resultado.versionServidor : undefined,
        conflictos: conflicting ? resultado.conflictos : undefined,
        nextRetryAt:
          status === 'RETRYABLE' ? new Date(Date.now() + backoffDelay(attempts)).toISOString() : undefined,
      })
      results.push({ operationId: operation.operationId, success: status === 'SYNCED', status, message: resultado.errorMessage })
    } catch (reason) {
      const error = normalizeApiError(reason)
      const status: PendingStatus = error.status === 409 ? 'CONFLICT' : 'RETRYABLE'
      await db.operacionesPendientes.update(operation.id, {
        status,
        updatedAt: new Date().toISOString(),
        lastError: error.message,
        nextRetryAt:
          status === 'RETRYABLE' ? new Date(Date.now() + backoffDelay(attempts)).toISOString() : undefined,
      })
      results.push({ operationId: operation.operationId, success: false, status, message: error.message })
    }
  }

  return results
}

export async function synchronizePendingFiles() {
  await assertSessionActive()
  const pending = await db.archivosPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'ERROR').sortBy('createdAt')
  const results: Array<{ localId: string; success: boolean; message?: string }> = []

  for (const file of pending) {
    if (!file.id) continue
    const attempts = file.attempts + 1
    await db.archivosPendientes.update(file.id, {
      status: 'PROCESSING',
      attempts,
      updatedAt: new Date().toISOString(),
    })
    try {
      const form = new FormData()
      form.append('file', new File([file.file], file.fileName, { type: file.mimeType }))
      form.append('entidadTipo', file.entityType)
      form.append('entidadId', file.entityId)
      form.append('principal', String(file.principal))
      await http.post('/api/v1/archivos/documentos', form, {
        headers: { 'Content-Type': undefined },
        onUploadProgress: (event) => {
          if (!event.total) return
          const progress = Math.min(100, Math.round((event.loaded / event.total) * 100))
          if (file.id) void db.archivosPendientes.update(file.id, { progress })
        },
      })
      await db.archivosPendientes.delete(file.id)
      results.push({ localId: file.localId, success: true })
    } catch (reason) {
      const error = normalizeApiError(reason)
      await db.archivosPendientes.update(file.id, {
        status: 'RETRYABLE',
        updatedAt: new Date().toISOString(),
        lastError: error.message,
        nextRetryAt: new Date(Date.now() + backoffDelay(attempts)).toISOString(),
      })
      results.push({ localId: file.localId, success: false, message: error.message })
    }
  }

  return results
}

export async function pullBootstrap() {
  await assertSessionActive()
  const updatedAt = new Date().toISOString()
  const response = await http.get<{ data: BootstrapResponse }>('/api/v1/sync/bootstrap', {
    params: { dispositivo: DISPOSITIVO, plataforma: 'WEB', versionApp: '1.0.0' },
  })
  const data = response.data.data

  const catalogos: LocalCatalog[] = [
    ...(data.propiedades ?? []).map((row) => toLocalCatalog(row, 'PROPIEDAD', updatedAt)),
    ...(data.potreros ?? []).map((row) => toLocalCatalog(row, 'POTRERO', updatedAt)),
    ...(data.lotes ?? []).map((row) => toLocalCatalog(row, 'LOTE', updatedAt)),
    ...(data.razas ?? []).map((row) => toLocalCatalog(row, 'RAZA', updatedAt)),
    ...(data.categorias ?? []).map((row) => toLocalCatalog(row, 'CATEGORIA', updatedAt)),
    ...(data.tiposPasto ?? []).map((row) => toLocalCatalog(row, 'TIPO_PASTO', updatedAt)),
  ]

  await db.transaction('rw', db.catalogos, db.animalesResumen, db.identificadores, db.estadoSincronizacion, async () => {
    await db.catalogos.bulkPut(catalogos)
    await db.animalesResumen.bulkPut(
      (data.animales ?? []).map((row) => toLocalAnimalSummary(row, updatedAt)),
    )
    await db.identificadores.bulkPut(
      (data.identificadores ?? []).map((row) => toLocalIdentifier(row, updatedAt)),
    )
    await db.estadoSincronizacion.put({ key: 'lastSyncAt', value: updatedAt, updatedAt })
    await db.estadoSincronizacion.put({ key: 'bootstrapCursor', value: String(data.cursor ?? 0), updatedAt })
  })

  return data
}

function toLocalAnimalSummary(row: Record<string, unknown>, updatedAt: string): LocalAnimalSummary {
  return {
    id: stringValue(row.id) ?? '',
    code: stringValue(row.codigo) ?? '',
    primaryIdentifier: stringValue(row.codigo),
    name: stringValue(row.nombre),
    sex: stringValue(row.sexo) ?? '',
    category: stringValue(row.categoriaActualId),
    propertyId: stringValue(row.propiedadActualId) ?? '',
    paddockId: stringValue(row.potreroActualId) ?? '',
    lotId: stringValue(row.loteActualId),
    status: stringValue(row.estado) ?? '',
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

function toLocalIdentifier(row: Record<string, unknown>, updatedAt: string): LocalIdentifier {
  return {
    id: stringValue(row.id) ?? '',
    animalId: stringValue(row.animalId) ?? '',
    tipo: stringValue(row.tipo) ?? '',
    valor: stringValue(row.valor) ?? '',
    principal: Boolean(row.principal),
    estado: stringValue(row.estado) ?? '',
    payload: stringValue(row.payload),
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

function toLocalCatalogRaw(row: Record<string, unknown>, type: string, updatedAt: string): LocalCatalog {
  return {
    id: stringValue(row.id) ?? '',
    type,
    code: stringValue(row.codigo) ?? '',
    name: stringValue(row.nombre) ?? '',
    activo: row.activo === undefined ? true : Boolean(row.activo),
    propiedadId: stringValue(row.propiedad_id),
    estado: stringValue(row.estado),
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

function toLocalAnimalSummaryRaw(row: Record<string, unknown>, updatedAt: string): LocalAnimalSummary {
  return {
    id: stringValue(row.id) ?? '',
    code: stringValue(row.codigo) ?? '',
    primaryIdentifier: stringValue(row.codigo),
    name: stringValue(row.nombre),
    sex: stringValue(row.sexo) ?? '',
    category: stringValue(row.categoria_actual_id),
    propertyId: stringValue(row.propiedad_actual_id) ?? '',
    paddockId: stringValue(row.potrero_actual_id) ?? '',
    lotId: stringValue(row.lote_actual_id),
    status: stringValue(row.estado) ?? '',
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

function toLocalIdentifierRaw(row: Record<string, unknown>, updatedAt: string): LocalIdentifier {
  return {
    id: stringValue(row.id) ?? '',
    animalId: stringValue(row.animal_id) ?? '',
    tipo: stringValue(row.tipo) ?? '',
    valor: stringValue(row.valor) ?? '',
    principal: Boolean(row.principal),
    estado: stringValue(row.estado) ?? '',
    payload: stringValue(row.payload),
    version: Number(row.version ?? 1),
    updatedAt,
  }
}

interface PullCambio {
  id: number
  tabla: string
  entidadId?: string
  tipoCambio: string
  datos?: Record<string, unknown>
  dispositivoOrigen?: string
  createdAt: string
}

interface PullResponse {
  dispositivoId: string
  cursor: number
  hayMas: boolean
  servertime: string
  cambios: PullCambio[]
}

const CATALOGO_TABLAS: Record<string, string> = {
  'core.razas': 'RAZA',
  'ganado.categorias_animal': 'CATEGORIA',
  'campo.tipos_pasto': 'TIPO_PASTO',
  'core.propiedades': 'PROPIEDAD',
  'campo.potreros': 'POTRERO',
  'ganado.lotes_ganaderos': 'LOTE',
}

export async function pullChanges(): Promise<{ permisosActualizados: boolean; aplicados: number }> {
  await assertSessionActive()
  const stored = await db.estadoSincronizacion.get('bootstrapCursor')
  let cursor = Number(stored?.value ?? 0)
  const updatedAt = new Date().toISOString()
  let permisosActualizados = false
  let aplicados = 0

  for (let fin = false; !fin;) {
    const response = await http.get<{ data: PullResponse }>('/api/v1/sync/pull', {
      params: { dispositivo: DISPOSITIVO, cursor, size: 500 },
    })
    const data = response.data.data
    for (const cambio of data.cambios) {
      aplicados += 1
      await aplicarCambio(cambio, updatedAt)
      if (cambio.tabla === 'seguridad.miembros_empresa') permisosActualizados = true
    }
    cursor = data.cursor
    if (data.cambios.length === 0 || !data.hayMas) fin = true
  }

  if (aplicados > 0) {
    await db.estadoSincronizacion.put({ key: 'bootstrapCursor', value: String(cursor), updatedAt })
    await db.estadoSincronizacion.put({ key: 'lastSyncAt', value: updatedAt, updatedAt })
  }

  if (permisosActualizados) {
    try {
      await pullBootstrap()
    } catch {
      // Sin conexión: los permisos se refrescan en el próximo bootstrap.
    }
  }

  return { permisosActualizados, aplicados }
}

async function aplicarCambio(cambio: PullCambio, updatedAt: string) {
  const entidad = cambio.entidadId
  if (cambio.tipoCambio === 'DELETE') {
    if (!entidad) return
    const tipo = CATALOGO_TABLAS[cambio.tabla]
    if (tipo) await db.catalogos.delete(entidad)
    else if (cambio.tabla === 'ganado.animales') await db.animalesResumen.delete(entidad)
    else if (cambio.tabla === 'ganado.identificadores_animal') await db.identificadores.delete(entidad)
    return
  }
  const datos = cambio.datos
  if (!datos) return
  const tipo = CATALOGO_TABLAS[cambio.tabla]
  if (tipo) {
    await db.catalogos.put(toLocalCatalogRaw(datos, tipo, updatedAt))
    return
  }
  if (cambio.tabla === 'ganado.animales') {
    await db.animalesResumen.put(toLocalAnimalSummaryRaw(datos, updatedAt))
    return
  }
  if (cambio.tabla === 'ganado.identificadores_animal') {
    await db.identificadores.put(toLocalIdentifierRaw(datos, updatedAt))
  }
}

export async function resolveConflictAcceptServer(operationId: string) {
  await db.operacionesPendientes.where('operationId').equals(operationId).delete()
  try {
    await pullBootstrap()
  } catch {
    // Sin conexión: el caché local se refresca en el próximo bootstrap.
  }
}

const TIPOS_CREACION = new Set(['ANIMAL_CREAR', 'PESAJE_REGISTRAR', 'MOVIMIENTO_CREAR'])

export async function resolveConflictKeepLocal(operationId: string) {
  const pending = await db.operacionesPendientes.where('operationId').equals(operationId).first()
  if (!pending?.id) return
  const fresh = createUuid()
  let datos = pending.datos
  if (datos && TIPOS_CREACION.has(pending.tipo)) {
    datos = { ...datos, id: fresh }
    if ('clienteUuid' in datos) datos.clienteUuid = fresh
    if ('idempotencyKey' in datos) datos.idempotencyKey = fresh
  }
  await db.operacionesPendientes.update(pending.id, {
    operationId: fresh,
    idempotencyKey: fresh,
    datos,
    status: 'PENDING',
    attempts: 0,
    lastError: undefined,
    nextRetryAt: undefined,
    datosServidor: undefined,
    versionServidor: undefined,
    conflictos: undefined,
    updatedAt: new Date().toISOString(),
  })
}
