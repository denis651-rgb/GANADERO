import { beforeEach, describe, expect, it, vi } from 'vitest'
import { prepareOfflineData } from './offlinePreparation'
import { pullBootstrap } from '@/sync/sync.service'

const { get, put, counts } = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
  counts: { RAZA: 3, CATEGORIA: 4, PROPIEDAD: 2, POTRERO: 5 } as Record<string, number>,
}))

vi.mock('@/offline/db', () => ({
  db: {
    estadoSincronizacion: { get, put },
    catalogos: { where: () => ({ equals: (type: string) => ({ count: async () => counts[type] ?? 0 }) }) },
  },
}))

vi.mock('@/sync/sync.service', () => ({ pullBootstrap: vi.fn() }))

describe('preparación de datos offline', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
  })

  it('ejecuta bootstrap y guarda una marca de preparación', async () => {
    get.mockResolvedValue(undefined)
    vi.mocked(pullBootstrap).mockResolvedValue({} as never)

    await prepareOfflineData()

    expect(pullBootstrap).toHaveBeenCalledOnce()
    expect(put).toHaveBeenCalledWith(expect.objectContaining({ key: 'offlineBootstrapCompletedAt' }))
  })

  it('no repite el bootstrap cuando el dispositivo ya está preparado', async () => {
    get.mockResolvedValue({ key: 'offlineBootstrapCompletedAt', value: '2026-08-10T00:00:00.000Z' })

    await prepareOfflineData()

    expect(pullBootstrap).not.toHaveBeenCalled()
  })

  it('explica que necesita internet para preparar el dispositivo', async () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false })

    await expect(prepareOfflineData()).rejects.toThrow('Necesitas conexión a internet')
    expect(pullBootstrap).not.toHaveBeenCalled()
  })
})
