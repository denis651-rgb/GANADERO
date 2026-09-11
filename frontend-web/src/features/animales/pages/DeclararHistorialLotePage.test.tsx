import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AnimalSummary } from '@/features/animales/types'
import { DeclararHistorialLotePage } from './DeclararHistorialLotePage'

const registrarHistorialDeclaradoLote = vi.fn()

vi.mock('@/features/sanidad/api', () => ({
  TIPO_ACTIVIDAD_LABELS: { VACUNACION: 'Vacunación', DESPARASITACION: 'Desparasitación' },
  registrarHistorialDeclaradoLote: (...args: unknown[]) => registrarHistorialDeclaradoLote(...args),
}))

const animales = [
  { id: 'animal-1', codigo: 'ANI-000001', nombre: 'Luna' },
  { id: 'animal-2', codigo: 'ANI-000002', nombre: 'Sol' },
] as AnimalSummary[]

function renderPage(state?: unknown) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  const router = createMemoryRouter(
    [{ path: '/animales/declarar-historial', element: <DeclararHistorialLotePage /> }],
    { initialEntries: [{ pathname: '/animales/declarar-historial', state }] },
  )
  render(<QueryClientProvider client={client}><RouterProvider router={router} /></QueryClientProvider>)
}

describe('DeclararHistorialLotePage', () => {
  it('registra varias actividades para todos los animales seleccionados', async () => {
    registrarHistorialDeclaradoLote.mockResolvedValue([{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }])
    renderPage({ animales })

    fireEvent.change(screen.getByLabelText(/^Tipo/), { target: { value: 'VACUNACION' } })
    fireEvent.change(screen.getByLabelText(/^Fecha/), { target: { value: '2026-09-01' } })
    fireEvent.change(screen.getByLabelText('Producto o medicamento'), { target: { value: 'Vacuna aftosa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Agregar actividad' }))
    const tipos = screen.getAllByLabelText(/^Tipo/)
    const fechas = screen.getAllByLabelText(/^Fecha/)
    fireEvent.change(tipos[1], { target: { value: 'DESPARASITACION' } })
    fireEvent.change(fechas[1], { target: { value: '2026-09-02' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar declaración' }))

    await waitFor(() => expect(registrarHistorialDeclaradoLote).toHaveBeenCalledWith({
      animalIds: ['animal-1', 'animal-2'],
      actividades: [
        expect.objectContaining({ tipoActividad: 'VACUNACION', fechaAplicacion: '2026-09-01', productoTexto: 'Vacuna aftosa' }),
        expect.objectContaining({ tipoActividad: 'DESPARASITACION', fechaAplicacion: '2026-09-02' }),
      ],
    }))
    expect(await screen.findByText(/4 antecedente\(s\) sanitario\(s\) correctamente/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ir a Animales' })).toBeInTheDocument()
  })

  it('muestra un vacío con salida a Animales si no llegan animales', () => {
    renderPage()
    expect(screen.getByText('Sin animales para declarar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ir a Animales' })).toBeInTheDocument()
  })
})