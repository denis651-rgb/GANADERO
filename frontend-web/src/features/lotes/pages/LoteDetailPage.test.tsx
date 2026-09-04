import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { expect, it, vi } from 'vitest'
import { LoteDetailPage } from './LoteDetailPage'
import { listMembresias } from '@/features/lotes/api'

const addAnimales = vi.fn().mockResolvedValue({ ok: true, ingresados: 1 })
const updateLote = vi.fn().mockResolvedValue({})
vi.mock('@/features/lotes/api', () => ({
  getLote: vi.fn().mockResolvedValue({ id: 'l-1', codigo: 'LOT-1', nombre: 'Toros', propiedadId: 'p-1', estado: 'ACTIVO', fechaApertura: '2026-09-03', version: 2, cantidadMaxima: 30, cantidadActual: 29 }),
  listMembresias: vi.fn().mockResolvedValue([]),
  addAnimales: (...args: unknown[]) => addAnimales(...args),
  updateLote: (...args: unknown[]) => updateLote(...args),
  cerrarLote: vi.fn(), retirarAnimales: vi.fn(),
}))
vi.mock('@/features/animales/api', () => ({
  listAnimals: vi.fn().mockResolvedValue({ content: [
    { id: 'a-1', codigo: 'ANI-1', propiedadActualId: 'p-1' },
    { id: 'a-2', codigo: 'ANI-2', propiedadActualId: 'p-1' },
  ] }),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', nombre: 'Finca' }]) }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/lotes/l-1']}><Routes>
    <Route path="/lotes/:id" element={<LoteDetailPage />} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

it('muestra el animal vendido por sus datos históricos sin depender de animales activos', async () => {
  vi.mocked(listMembresias).mockImplementationOnce(async () => [])
    .mockImplementationOnce(async () => [{
      id: 'm-vendido', loteId: 'l-1', animalId: 'vendido', animalCodigo: 'ANI-VENDIDO', animalNombre: 'LB-01',
      fechaIngreso: '2026-09-03T12:00:00Z', fechaSalida: '2026-09-03T18:00:00Z',
      motivoSalida: 'Movimiento SALIDA_VENTA', version: 1,
    }])
  renderPage()
  const tabla = await screen.findByRole('table', { name: 'Historial de animales del lote' })
  expect(within(tabla).getByText('LB-01')).toBeInTheDocument()
  expect(within(tabla).queryByText('ANI-VENDIDO')).not.toBeInTheDocument()
  expect(within(tabla).getByText('Movimiento SALIDA_VENTA')).toBeInTheDocument()
})

it('prioriza el nombre en los integrantes actuales', async () => {
  vi.mocked(listMembresias).mockResolvedValueOnce([{
    id: 'm-activo', loteId: 'l-1', animalId: 'a-1', animalCodigo: 'ANI-1', animalNombre: 'Animal local',
    fechaIngreso: '2026-09-03T12:00:00Z', version: 0,
  }])
  renderPage()
  const tabla = await screen.findByRole('table', { name: 'Animales integrantes del lote' })
  expect(within(tabla).getByText('Animal local')).toBeInTheDocument()
  expect(within(tabla).queryByText('ANI-1')).not.toBeInTheDocument()
})

it('muestra ocupación y bloquea la selección excesiva', async () => {
  renderPage()
  expect(await screen.findByText('29 / 30 animales')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Agregar animales' }))
  const modal = await screen.findByRole('dialog', { name: 'Agregar animales al lote' })
  fireEvent.click(await within(modal).findByLabelText('ANI-1'))
  fireEvent.click(within(modal).getByLabelText('ANI-2'))
  expect(within(modal).getByRole('button', { name: 'Agregar 2 animal(es)' })).toBeDisabled()
  expect(addAnimales).not.toHaveBeenCalled()
  fireEvent.click(within(modal).getByLabelText('ANI-2'))
  fireEvent.click(within(modal).getByRole('button', { name: 'Agregar 1 animal(es)' }))
  await waitFor(() => expect(addAnimales).toHaveBeenCalledWith('l-1', expect.objectContaining({ animalIds: ['a-1'] })))
})

it('edita el máximo enviando la versión actual y limita la reducción', async () => {
  renderPage()
  fireEvent.click(await screen.findByRole('button', { name: 'Configurar cantidad máxima' }))
  const input = screen.getByLabelText('Cantidad máxima de animales')
  expect(input).toHaveAttribute('min', '29')
  expect(input).toHaveValue(30)
  fireEvent.change(input, { target: { value: '40' } })
  fireEvent.click(screen.getByRole('button', { name: 'Guardar máximo' }))
  await waitFor(() => expect(updateLote).toHaveBeenCalledWith('l-1', { cantidadMaxima: 40, version: 2 }))
})
