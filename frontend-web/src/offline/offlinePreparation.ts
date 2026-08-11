import { db } from '@/offline/db'
import { pullBootstrap } from '@/sync/sync.service'

const PREPARED_KEY = 'offlineBootstrapCompletedAt'
let activePreparation: Promise<void> | null = null

export interface OfflinePreparationStatus {
  prepared: boolean
  preparedAt?: string
  breeds: number
  categories: number
  properties: number
  paddocks: number
}

export async function getOfflinePreparationStatus(): Promise<OfflinePreparationStatus> {
  const [marker, breeds, categories, properties, paddocks] = await Promise.all([
    db.estadoSincronizacion.get(PREPARED_KEY),
    db.catalogos.where('type').equals('RAZA').count(),
    db.catalogos.where('type').equals('CATEGORIA').count(),
    db.catalogos.where('type').equals('PROPIEDAD').count(),
    db.catalogos.where('type').equals('POTRERO').count(),
  ])
  return { prepared: Boolean(marker?.value), preparedAt: marker?.value, breeds, categories, properties, paddocks }
}

export async function prepareOfflineData(options: { force?: boolean } = {}): Promise<void> {
  if (!navigator.onLine) throw new Error('Necesitas conexión a internet para preparar los datos offline.')
  if (!options.force && (await getOfflinePreparationStatus()).prepared) return
  if (activePreparation) return activePreparation

  activePreparation = (async () => {
    await pullBootstrap()
    const completedAt = new Date().toISOString()
    await db.estadoSincronizacion.put({ key: PREPARED_KEY, value: completedAt, updatedAt: completedAt })
  })().finally(() => {
    activePreparation = null
  })
  return activePreparation
}

