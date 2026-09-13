import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, it, vi } from 'vitest'
import { getMovimiento, listDetalles } from '@/features/movimientos/api'
import { MovimientosPage } from './MovimientosPage'

vi.mock('@/features/movimientos/api', () => ({
  listMovimientos: vi.fn().mockResolvedValue({ content: [], page: 0, size: 20, totalPages: 0, totalElements: 0 }),
  getMovimiento: vi.fn().mockResolvedValue({ id: 'mov-lote', tipo: 'CAMBIO_POTRERO', estado: 'CONFIRMADO', fechaMovimiento: '2026-09-11', version: 0 }),
  listDetalles: vi.fn().mockResolvedValue([{ id: 'd-1', animalId: 'a-1', estadoResultado: 'OK' }, { id: 'd-2', animalId: 'a-2', estadoResultado: 'OK' }]),
  anularMovimiento: vi.fn(), confirmarMovimiento: vi.fn(), createMovimiento: vi.fn(), revertirMovimiento: vi.fn(), validarMovimiento: vi.fn(),
}))
vi.mock('@/features/animales/api', () => ({ listAnimals: vi.fn().mockResolvedValue({ content: [{ id: 'a-1', codigo: 'ANI-1' }, { id: 'a-2', codigo: 'ANI-2' }] }), getAnimal: vi.fn().mockResolvedValue({}) }))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/potreros/api', () => ({ listAllPotreros: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/lotes/api', () => ({ listLotes: vi.fn().mockResolvedValue({ content: [] }) }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

it('abre el movimiento del enlace aunque no esté en la página del listado y muestra sus animales', async () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/movimientos?movimientoId=mov-lote']}><MovimientosPage /></MemoryRouter></QueryClientProvider>)
  const dialog = await screen.findByRole('dialog', { name: 'Cambio de potrero' })
  expect(getMovimiento).toHaveBeenCalledWith('mov-lote')
  await waitFor(() => expect(listDetalles).toHaveBeenCalledWith('mov-lote'))
  expect(await within(dialog).findByText('Animales (2)')).toBeInTheDocument()
  expect(await within(dialog).findByText('ANI-1')).toBeInTheDocument()
  expect(within(dialog).getByText('ANI-2')).toBeInTheDocument()
  fireEvent.keyDown(document, { key: 'Escape' })
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  client.clear()
})
