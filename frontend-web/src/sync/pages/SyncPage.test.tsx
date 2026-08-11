import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { SyncPage } from './SyncPage'

const acceptServer = vi.fn()
const keepLocal = vi.fn()
const operation = {
  operationId: 'op-1', tipo: 'ANIMAL_EDITAR', entidad: 'ANIMAL', entidadId: 'a-1', idempotencyKey: 'key',
  status: 'CONFLICT', attempts: 1, createdAt: '2026-08-10T10:00:00Z', updatedAt: '2026-08-10T10:00:00Z',
  datos: { nombre: 'Local' }, datosServidor: { nombre: 'Servidor' }, versionServidor: 4,
}

vi.mock('dexie-react-hooks', () => ({ useLiveQuery: (callback: () => unknown, _deps: unknown[], fallback: unknown) => {
  const source = callback.toString()
  if (source.includes('operacionesPendientes')) return [operation]
  if (source.includes('archivosPendientes')) return []
  if (source.includes('identificadores')) return 0
  return fallback
} }))
vi.mock('@/offline/db', () => ({ db: { operacionesPendientes: {}, archivosPendientes: {}, identificadores: {}, estadoSincronizacion: {} } }))
vi.mock('@/sync/sync.service', () => ({
  resolveConflictAcceptServer: (...args: unknown[]) => acceptServer(...args),
  resolveConflictKeepLocal: (...args: unknown[]) => keepLocal(...args),
  pullChanges: vi.fn(), synchronizePendingFiles: vi.fn(), synchronizePendingOperations: vi.fn(),
}))
vi.mock('@/shared/hooks/useOnlineStatus', () => ({ useOnlineStatus: () => true }))
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ sessionExpired: false }) }))

describe('SyncPage conflict protection', () => {
  it('cancela o confirma cada estrategia sin ejecutarla inmediatamente', async () => {
    acceptServer.mockReset().mockResolvedValue(undefined)
    keepLocal.mockReset().mockResolvedValue(undefined)
    render(<SyncPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Conservar versión local' }))
    expect(keepLocal).not.toHaveBeenCalled()
    expect(screen.getByText('Información del dispositivo')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(keepLocal).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Usar versión servidor' }))
    expect(acceptServer).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Usar versión del servidor' }))
    await waitFor(() => expect(acceptServer).toHaveBeenCalledOnce())
  })
})
