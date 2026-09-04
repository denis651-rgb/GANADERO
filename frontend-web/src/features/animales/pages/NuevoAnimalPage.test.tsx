import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { NuevoAnimalPage } from './NuevoAnimalPage'

const catalogId = '00000000-0000-0000-0000-000000000001'
const createAnimal = vi.fn().mockResolvedValue({ id: 'a-1' })
vi.mock('@/features/animales/api', () => ({
  createAnimal: (...args: unknown[]) => createAnimal(...args),
  listRazas: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', nombre: 'Nelore' }]),
  listCategorias: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', nombre: 'Novillo' }]),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', nombre: 'Finca', activo: true }]) }))
vi.mock('@/features/potreros/api', () => ({ listPotreros: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', propiedadId: '00000000-0000-0000-0000-000000000001', nombre: 'Corral', activo: true }]) }))

describe('NuevoAnimalPage', () => {
  it.each(['true', 'false'])('guarda peso de ingreso con tipo estimado=%s sin inventar nacimiento', async (tipo) => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><MemoryRouter><NuevoAnimalPage /></MemoryRouter></QueryClientProvider>)
    await screen.findByText('Nelore')
    for (const label of ['Raza', 'Categoría', 'Propiedad', 'Potrero']) {
      fireEvent.change(screen.getByLabelText(label), { target: { value: catalogId } })
    }
    fireEvent.change(screen.getByLabelText('Origen'), { target: { value: 'COMPRADO' } })
    fireEvent.change(screen.getByLabelText('Peso al ingreso (kg)'), { target: { value: '150' } })
    fireEvent.change(screen.getByLabelText('Tipo de peso al ingreso'), { target: { value: tipo } })
    expect(screen.getByLabelText('Fecha de nacimiento')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Guardar animal' }))
    await waitFor(() => expect(createAnimal).toHaveBeenCalledWith(expect.objectContaining({
      pesoIngresoKg: 150, pesoIngresoEstimado: tipo === 'true', fechaNacimiento: undefined,
      fechaNacimientoEstimada: false, origen: 'COMPRADO',
    })))
  })
})
