import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AnimalSummary } from '@/features/animales/types'
import { AnimalMultiPicker } from './AnimalMultiPicker'

const listLotes = vi.fn()
const listMembresias = vi.fn()
const listAnimals = vi.fn()

vi.mock('@/features/lotes/api', () => ({
  listLotes: (...args: unknown[]) => listLotes(...args),
  listMembresias: (...args: unknown[]) => listMembresias(...args),
}))
vi.mock('@/features/animales/api', () => ({
  listAnimals: (...args: unknown[]) => listAnimals(...args),
}))

const animales = [
  { id: 'animal-1', codigo: 'ANI-000001', nombre: 'Luna' },
  { id: 'animal-2', codigo: 'ANI-000002', nombre: 'Sol' },
] as AnimalSummary[]

function renderPicker(seleccionados: Set<string>, onChange: (next: Set<string>) => void) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AnimalMultiPicker animales={animales} cargando={false} seleccionados={seleccionados} onChange={onChange} />
    </QueryClientProvider>,
  )
}

describe('AnimalMultiPicker', () => {
  it('muestra la lista precargada sin búsqueda', () => {
    renderPicker(new Set(), vi.fn())
    expect(screen.getByText('ANI-000001', { exact: false })).toBeInTheDocument()
    expect(screen.getByText('ANI-000002', { exact: false })).toBeInTheDocument()
  })

  it('al escribir 2+ caracteres, delega la búsqueda al backend (código, arete, nombre o raza)', async () => {
    listAnimals.mockResolvedValue({ content: [{ id: 'animal-2', codigo: 'ANI-000002', nombre: 'Sol' }], page: 0, size: 200, totalElements: 1, totalPages: 1 })
    renderPicker(new Set(), vi.fn())

    fireEvent.change(screen.getByLabelText('Buscar animales por código, arete, nombre o raza'), { target: { value: 'Sol' } })

    await waitFor(() => expect(listAnimals).toHaveBeenCalledWith(expect.objectContaining({ search: 'Sol', estado: 'ACTIVO' })))
    expect(await screen.findByText('ANI-000002', { exact: false })).toBeInTheDocument()
    expect(screen.queryByText('ANI-000001', { exact: false })).not.toBeInTheDocument()
  })

  it('agrega o quita un animal al marcar su checkbox', () => {
    const onChange = vi.fn()
    renderPicker(new Set(), onChange)

    fireEvent.click(screen.getByRole('checkbox', { name: /ANI-000001/ }))

    expect(onChange).toHaveBeenCalledWith(new Set(['animal-1']))
  })

  it('agrega todos los animales activos de un lote seleccionado', async () => {
    listLotes.mockResolvedValue({ content: [{ id: 'lote-1', codigo: 'LOTE-01', nombre: 'Potrero Norte', cantidadActual: 2 }], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    listMembresias.mockResolvedValue([
      { id: 'm1', loteId: 'lote-1', animalId: 'animal-1', fechaIngreso: '2026-01-01', version: 0, animalCodigo: 'ANI-000001' },
      { id: 'm2', loteId: 'lote-1', animalId: 'animal-2', fechaIngreso: '2026-01-01', version: 0, animalCodigo: 'ANI-000002' },
    ])
    const onChange = vi.fn()
    renderPicker(new Set(), onChange)

    fireEvent.change(screen.getByLabelText('…o vender un lote completo'), { target: { value: 'Potrero' } })
    fireEvent.mouseDown(await screen.findByText('LOTE-01 · Potrero Norte'))

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(new Set(['animal-1', 'animal-2'])))
  })
})
