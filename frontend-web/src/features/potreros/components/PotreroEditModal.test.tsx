import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PotreroEditModal } from './PotreroEditModal'

vi.mock('@/features/propiedades/api', () => ({ listSectores: vi.fn().mockResolvedValue([{ id: 's-1', propiedadId: 'p-1', codigo: 'S-1', nombre: 'Norte', activo: true, version: 1 }]) }))

const potrero = { id: 'pot-1', propiedadId: 'p-1', sectorId: 's-1', codigo: 'P-01', nombre: 'Potrero norte', superficieHa: 12, tipoPastoId: 'g-1', capacidadUa: 8, tieneAgua: true, estado: 'DISPONIBLE' as const, activo: true, version: 6 }

describe('PotreroEditModal', () => {
  it('permite quitar asociaciones y valores opcionales explícitamente', async () => {
    const onSubmit = vi.fn()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PotreroEditModal potrero={potrero} properties={[{ id: 'p-1', codigo: 'PR-1', nombre: 'La Esperanza', activo: true, version: 1 }]} grasses={[{ id: 'g-1', codigo: 'G-1', nombre: 'Brachiaria' }]} online loading={false} error={null} onClose={vi.fn()} onSubmit={onSubmit} onReload={vi.fn()} /></QueryClientProvider>)

    await waitFor(() => expect(screen.getByLabelText('Sector')).not.toBeDisabled())
    fireEvent.change(screen.getByLabelText('Sector'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Tipo de pasto'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Superficie (ha)'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Capacidad (UA)'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar potrero' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ quitarSector: true, quitarTipoPasto: true, quitarSuperficie: true, quitarCapacidad: true, version: 6 }))
  })
})
