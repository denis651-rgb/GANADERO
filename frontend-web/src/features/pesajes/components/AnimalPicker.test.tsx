import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AnimalPicker } from './AnimalPicker'

const listAnimals = vi.fn().mockResolvedValue({ content: [], page: 0, size: 8, totalElements: 0, totalPages: 0 })
vi.mock('@/features/animales/api', () => ({ listAnimals: (...args: unknown[]) => listAnimals(...args) }))

const properties = [{ id: 'prop-1', codigo: 'P-1', nombre: 'Hacienda Santa Bárbara', activo: true }]
const paddocks = [
  { id: 'pot-1', propiedadId: 'prop-1', codigo: 'POT-1', nombre: 'Potrero 4', tieneAgua: true, estado: 'DISPONIBLE', activo: true, version: 0 },
  { id: 'pot-2', propiedadId: 'prop-1', codigo: 'POT-2', nombre: 'Potrero 5', tieneAgua: true, estado: 'DISPONIBLE', activo: true, version: 0 },
]
const lots = [{ id: 'lote-1', propiedadId: 'prop-1', codigo: 'LOT-1', nombre: 'Lote Norte', cantidadActual: 3, estado: 'ACTIVO', fechaApertura: '2026-01-01', version: 0 }]

function renderPicker() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      <AnimalPicker value={null} onChange={vi.fn()} properties={properties as any} paddocks={paddocks as any} lots={lots as any} />
    </QueryClientProvider>,
  )
}

describe('AnimalPicker', () => {
  it('deshabilita Potrero y Lote hasta elegir una Propiedad', () => {
    renderPicker()
    expect(screen.getByLabelText('Potrero')).toBeDisabled()
    expect(screen.getByLabelText('Lote')).toBeDisabled()
  })

  it('busca en toda la propiedad sin necesidad de escribir texto', async () => {
    renderPicker()
    fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: 'prop-1' } })
    await waitFor(() => expect(listAnimals).toHaveBeenCalledWith(expect.objectContaining({
      propiedadId: 'prop-1', potreroId: undefined, loteId: undefined,
    })))
    expect(screen.getByLabelText('Potrero')).not.toBeDisabled()
    expect(screen.getByLabelText('Lote')).not.toBeDisabled()
  })

  it('filtra solo por ese potrero cuando se elige propiedad y potrero', async () => {
    renderPicker()
    fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: 'prop-1' } })
    fireEvent.change(screen.getByLabelText('Potrero'), { target: { value: 'pot-1' } })
    await waitFor(() => expect(listAnimals).toHaveBeenCalledWith(expect.objectContaining({
      propiedadId: 'prop-1', potreroId: 'pot-1', loteId: undefined,
    })))
  })

  it('filtra por propiedad, potrero y lote cuando se eligen los tres', async () => {
    renderPicker()
    fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: 'prop-1' } })
    fireEvent.change(screen.getByLabelText('Potrero'), { target: { value: 'pot-1' } })
    fireEvent.change(screen.getByLabelText('Lote'), { target: { value: 'lote-1' } })
    await waitFor(() => expect(listAnimals).toHaveBeenCalledWith(expect.objectContaining({
      propiedadId: 'prop-1', potreroId: 'pot-1', loteId: 'lote-1',
    })))
  })

  it('limpia Potrero y Lote al cambiar de Propiedad', async () => {
    renderPicker()
    fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: 'prop-1' } })
    fireEvent.change(screen.getByLabelText('Potrero'), { target: { value: 'pot-1' } })
    fireEvent.change(screen.getByLabelText('Propiedad'), { target: { value: '' } })
    expect(screen.getByLabelText('Potrero')).toHaveValue('')
    expect(screen.getByLabelText('Potrero')).toBeDisabled()
  })
})
