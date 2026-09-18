import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EnfermedadesPanel } from './EnfermedadesPanel'

const listEnfermedades = vi.fn().mockResolvedValue([])
const crearEnfermedad = vi.fn().mockResolvedValue({ id: 'e-1', codigo: 'ENF-001', nombre: 'Fiebre aftosa', activo: true })
vi.mock('@/features/sanidad/api', () => ({
  listEnfermedades: (...args: unknown[]) => listEnfermedades(...args),
  crearEnfermedad: (...args: unknown[]) => crearEnfermedad(...args),
  cambiarEstadoEnfermedad: vi.fn(),
}))
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))

function renderPanel() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><EnfermedadesPanel /></QueryClientProvider>)
}

describe('EnfermedadesPanel', () => {
  it('no muestra un campo de código: se genera automáticamente en el backend', async () => {
    renderPanel()
    fireEvent.click(await screen.findByRole('button', { name: 'Nueva enfermedad' }))
    const modal = await screen.findByRole('dialog', { name: 'Nueva enfermedad' })
    expect(screen.queryByLabelText(/^Código/)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/^Nombre/), { target: { value: 'Fiebre aftosa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear enfermedad' }))

    await waitFor(() => expect(crearEnfermedad).toHaveBeenCalledWith({
      nombre: 'Fiebre aftosa', descripcion: undefined, esNotificable: false,
    }))
    expect(modal).not.toHaveTextContent('Código')
  })
})
