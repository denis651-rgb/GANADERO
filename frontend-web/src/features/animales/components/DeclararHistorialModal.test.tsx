import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DeclararHistorialModal } from './DeclararHistorialModal'

const registrarAplicacionDeclarada = vi.fn()
const listActivePlanItems = vi.fn().mockResolvedValue([])

vi.mock('@/features/sanidad/api', () => ({
  TIPO_ACTIVIDAD_LABELS: { VACUNACION: 'Vacunación', DESPARASITACION: 'Desparasitación' },
  registrarAplicacionDeclarada: (...args: unknown[]) => registrarAplicacionDeclarada(...args),
  listActivePlanItems: (...args: unknown[]) => listActivePlanItems(...args),
}))

function renderModal(onSuccess = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><DeclararHistorialModal animalId="animal-1" animalLabel="ANI-000001 · Luna" onClose={vi.fn()} onSuccess={onSuccess} /></QueryClientProvider>)
}

describe('DeclararHistorialModal', () => {
  it('declara una actividad sin vincularla a un ítem del plan', async () => {
    registrarAplicacionDeclarada.mockReset().mockResolvedValue({ id: 'ap-1' })
    const onSuccess = vi.fn()
    renderModal(onSuccess)

    fireEvent.change(screen.getByLabelText(/^Actividad/), { target: { value: 'VACUNACION' } })
    fireEvent.change(screen.getByLabelText(/^Fecha/), { target: { value: '2026-09-01' } })
    fireEvent.change(screen.getByLabelText('Producto o medicamento'), { target: { value: 'Vacuna aftosa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(registrarAplicacionDeclarada).toHaveBeenCalledWith(expect.objectContaining({
      animalId: 'animal-1', tipoActividad: 'VACUNACION', planItemId: undefined, fechaAplicacion: '2026-09-01', productoTexto: 'Vacuna aftosa',
    })))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
  })

  it('vincula la declaración a un ítem del plan sanitario cuando se elige uno', async () => {
    registrarAplicacionDeclarada.mockReset().mockResolvedValue({ id: 'ap-2' })
    listActivePlanItems.mockResolvedValueOnce([
      { id: 'item-vacuna-1', tipoActividad: 'VACUNACION', nombre: 'Fiebre aftosa', activo: true },
      { id: 'item-desparasitacion-1', tipoActividad: 'DESPARASITACION', nombre: 'Desparasitación de ingreso', activo: true },
    ])
    renderModal()

    fireEvent.change(screen.getByLabelText(/^Actividad/), { target: { value: 'VACUNACION' } })
    await screen.findByRole('option', { name: 'Fiebre aftosa' })
    expect(screen.queryByRole('option', { name: 'Desparasitación de ingreso' })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Ítem del plan sanitario'), { target: { value: 'item-vacuna-1' } })
    fireEvent.change(screen.getByLabelText(/^Fecha/), { target: { value: '2026-09-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(registrarAplicacionDeclarada).toHaveBeenCalledWith(expect.objectContaining({
      planItemId: 'item-vacuna-1',
    })))
  })
})
