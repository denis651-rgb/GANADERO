import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PotrerosPage } from './PotrerosPage'

const updatePotrero = vi.fn()
vi.mock('@/features/potreros/api', () => ({
  listPotreros: vi.fn().mockResolvedValue([{ id: 'pot-1', propiedadId: 'p-1', codigo: 'P-01', nombre: 'Norte', estado: 'DISPONIBLE', activo: true, tieneAgua: true, version: 1 }]),
  listTiposPasto: vi.fn().mockResolvedValue([]), createPotrero: vi.fn(),
  updatePotrero: (...args: unknown[]) => updatePotrero(...args),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', nombre: 'La Esperanza', activo: true }]), listSectores: vi.fn().mockResolvedValue([]) }))

describe('PotrerosPage operational protection', () => {
  it('pide confirmación antes de aplicar un estado operativo', async () => {
    updatePotrero.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PotrerosPage /></QueryClientProvider>)
    const select = await screen.findByRole('combobox', { name: 'Estado de Norte' })
    fireEvent.change(select, { target: { value: 'MANTENIMIENTO' } })
    expect(updatePotrero).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar cambio' }))
    await waitFor(() => expect(updatePotrero).toHaveBeenCalledOnce())
  })
})
