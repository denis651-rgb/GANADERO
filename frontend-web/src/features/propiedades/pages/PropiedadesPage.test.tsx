import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PropiedadesPage } from './PropiedadesPage'

const updatePropiedad = vi.fn()
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', codigo: 'PR-1', nombre: 'La Esperanza', activo: true, version: 2 }]),
  listSectores: vi.fn().mockResolvedValue([]), createPropiedad: vi.fn(), createSector: vi.fn(),
  updatePropiedad: (...args: unknown[]) => updatePropiedad(...args),
}))

describe('PropiedadesPage operational protection', () => {
  it('pide confirmación antes de desactivar una propiedad', async () => {
    updatePropiedad.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PropiedadesPage /></QueryClientProvider>)
    fireEvent.click((await screen.findAllByRole('button', { name: 'Desactivar' }))[0])
    expect(updatePropiedad).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Desactivar propiedad' }))
    await waitFor(() => expect(updatePropiedad).toHaveBeenCalledOnce())
  })
})
