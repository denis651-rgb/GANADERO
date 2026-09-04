import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, it, vi } from 'vitest'
import { LotesPage } from './LotesPage'

const createLote = vi.fn().mockResolvedValue({ id: 'l-1', codigo: 'LOT-2026-0001' })
vi.mock('@/features/lotes/api', () => ({
  createLote: (...args: unknown[]) => createLote(...args),
  listLotes: vi.fn().mockResolvedValue({ content: [], page: 0, totalPages: 0 }),
}))
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', nombre: 'Finca Cerro Verde', activo: true }]),
}))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

it('permite crear un lote sin mostrar ni enviar un código manual', async () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter><LotesPage /></MemoryRouter></QueryClientProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Nuevo lote' }))
  await screen.findByText('Finca Cerro Verde')
  expect(screen.queryByLabelText(/Código/)).not.toBeInTheDocument()
  expect(screen.queryByText('Se asigna al guardar')).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: 'p-1' } })
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Lote toros Nelore' } })
  fireEvent.change(screen.getByLabelText('Cantidad máxima de animales'), { target: { value: '30' } })
  fireEvent.click(screen.getByRole('button', { name: 'Crear lote' }))
  await waitFor(() => expect(createLote).toHaveBeenCalledOnce())
  expect(createLote.mock.calls[0][0]).toEqual({ propiedadId: 'p-1', nombre: 'Lote toros Nelore', descripcion: undefined, fechaApertura: undefined, cantidadMaxima: 30 })
})
