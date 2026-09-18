import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, it, vi } from 'vitest'
import { LotesPage } from './LotesPage'
import { listLotes } from '@/features/lotes/api'

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

it('muestra la apertura y el cierre tal como se guardaron, sin correrlos un día', async () => {
  // Una fecha pura («2024-01-01») no debe convertirse a medianoche UTC: en Bolivia (UTC-4) eso da el 31/12/2023.
  vi.mocked(listLotes).mockResolvedValueOnce({
    content: [{
      id: 'l-9', propiedadId: 'p-1', codigo: 'LOT-2024-0001', nombre: 'Toros', estado: 'CERRADO',
      fechaApertura: '2024-01-01', fechaCierre: '2024-03-31', version: 1, cantidadActual: 0, cantidadMaxima: 10,
    }],
    page: 0, size: 20, totalElements: 1, totalPages: 1,
  } as Awaited<ReturnType<typeof listLotes>>)
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter><LotesPage /></MemoryRouter></QueryClientProvider>)

  expect((await screen.findAllByText('01/01/2024')).length).toBeGreaterThan(0)
  expect(screen.getAllByText('31/03/2024').length).toBeGreaterThan(0)
  expect(screen.queryByText('31/12/2023')).not.toBeInTheDocument()
  expect(screen.queryByText('30/03/2024')).not.toBeInTheDocument()
})
