import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { VentasPage } from './VentasPage'

const listVentas = vi.fn()
const registrarVentaLote = vi.fn()
const listAnimals = vi.fn()
const getPesajeHistory = vi.fn()

vi.mock('@/features/ventas/api', () => ({
  listVentas: (...args: unknown[]) => listVentas(...args),
  registrarVenta: vi.fn(),
  registrarVentaLote: (...args: unknown[]) => registrarVentaLote(...args),
}))
vi.mock('@/features/animales/api', () => ({
  listAnimals: (...args: unknown[]) => listAnimals(...args),
}))
vi.mock('@/features/pesajes/api', () => ({
  getPesajeHistory: (...args: unknown[]) => getPesajeHistory(...args),
}))
vi.mock('@/features/lotes/api', () => ({
  listLotes: vi.fn().mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
  listMembresias: vi.fn().mockResolvedValue([]),
}))

const animal1 = { id: 'animal-1', codigo: 'ANI-000001', nombre: 'Luna', estado: 'ACTIVO' }
const animal2 = { id: 'animal-2', codigo: 'ANI-000002', nombre: 'Sol', estado: 'ACTIVO' }

function renderPage() {
  listVentas.mockResolvedValue([])
  listAnimals.mockResolvedValue({ content: [animal1, animal2], page: 0, size: 500, totalElements: 2, totalPages: 1 })
  getPesajeHistory.mockImplementation((id: string) => Promise.resolve([
    { id: `pesaje-${id}`, animalId: id, fecha: '2026-08-01', pesoKg: id === 'animal-1' ? 380 : 400, tipo: 'RUTINA', tipoPeso: 'MEDIDO', estado: 'ACTIVO', version: 0 },
  ]))
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><VentasPage /></QueryClientProvider>)
}

describe('VentasPage — venta por lote', () => {
  it('calcula el monto por animal en modalidad carneado y envía los pesos prellenados', async () => {
    registrarVentaLote.mockResolvedValue([])
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Varios animales / lote' }))

    await screen.findByRole('checkbox', { name: /ANI-000001/ })
    fireEvent.click(screen.getByRole('checkbox', { name: /ANI-000001/ }))
    fireEvent.click(screen.getByRole('checkbox', { name: /ANI-000002/ }))

    // Espera a que el peso se prellene desde el último pesaje activo de cada animal.
    await waitFor(() => expect(screen.getByLabelText('Peso de salida de ANI-000001 · Luna')).toHaveValue(380))
    await waitFor(() => expect(screen.getByLabelText('Peso de salida de ANI-000002 · Sol')).toHaveValue(400))

    fireEvent.click(screen.getByLabelText(/Carneado/))
    fireEvent.change(screen.getByLabelText(/Precio por kilo/), { target: { value: '15' } })
    fireEvent.change(screen.getByLabelText(/^Comprador/), { target: { value: 'Frigorífico Norte' } })

    fireEvent.click(screen.getByRole('button', { name: /Guardar venta de 2 animal/ }))

    await waitFor(() => expect(registrarVentaLote).toHaveBeenCalledWith(expect.objectContaining({
      animalIds: ['animal-1', 'animal-2'],
      comprador: 'Frigorífico Norte',
      modalidad: 'CARNEADO',
      precioKg: 15,
      pesosVentaKg: { 'animal-1': 380, 'animal-2': 400 },
    })))
  })

  it('bloquea el envío en modalidad carneado si falta el peso de un animal', async () => {
    renderPage()
    getPesajeHistory.mockResolvedValue([])

    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Varios animales / lote' }))

    await screen.findByRole('checkbox', { name: /ANI-000001/ })
    fireEvent.click(screen.getByRole('checkbox', { name: /ANI-000001/ }))
    fireEvent.click(screen.getByLabelText(/Carneado/))

    expect(screen.getByRole('button', { name: /Guardar venta de 1 animal/ })).toBeDisabled()
  })
})
