import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, expect, it, vi } from 'vitest'
import { GestacionSelect } from './GestacionSelect'
import { abrirGestacion, listGestaciones } from '../api'

vi.mock('../api', () => ({ listGestaciones: vi.fn(), abrirGestacion: vi.fn(), listDiagnosticos: vi.fn() }))
beforeEach(() => vi.clearAllMocks())
function view() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><form><GestacionSelect animalId="vaca" /></form></QueryClientProvider>)
}
it('solo permite seleccionar la gestación abierta y conserva las cerradas en el historial', async () => {
  vi.mocked(listGestaciones).mockResolvedValue([
    { id: 'cerrada', animalId: 'vaca', estado: 'FINALIZADA_PARTO', fechaConfirmacion: '2025-01-01', fechaCierre: '2025-09-01', antecedentesDesconocidos: true },
    { id: 'abierta', animalId: 'vaca', estado: 'ABIERTA', fechaConfirmacion: '2026-01-01', antecedentesDesconocidos: true },
  ])
  view()
  await screen.findByRole('option', { name: /Confirmada el/ })
  const select = screen.getByLabelText(/Gestación que finaliza/)
  expect(within(select).getAllByRole('option')).toHaveLength(2)
  expect(within(select).queryByRole('option', { name: /Finalizada/ })).not.toBeInTheDocument()
  expect(screen.getByText(/Finalizada por parto/)).toBeInTheDocument()
  fireEvent.change(select, { target: { value: 'abierta' } })
  expect(select).toHaveValue('abierta')
})
it('sin gestación abierta bloquea el desenlace y permite registrar antecedentes explícitos', async () => {
  vi.mocked(listGestaciones).mockResolvedValue([])
  vi.mocked(abrirGestacion).mockResolvedValue({ id: 'nueva', animalId: 'vaca', estado: 'ABIERTA', fechaConfirmacion: '2026-01-01', antecedentesDesconocidos: true })
  view()
  const modo = await screen.findByLabelText('Identificar gestación')
  expect((screen.getByLabelText(/Gestación que finaliza/) as HTMLSelectElement).checkValidity()).toBe(false)
  fireEvent.change(modo, { target: { value: 'desconocidos' } })
  fireEvent.change(screen.getByLabelText('Fecha de confirmación'), { target: { value: '2026-01-01' } })
  fireEvent.change(screen.getByLabelText('Antecedentes y evidencia de confirmación'), { target: { value: 'Comprada gestante, revisión registrada' } })
  fireEvent.click(screen.getByRole('button', { name: 'Guardar gestación' }))
  await waitFor(() => expect(abrirGestacion).toHaveBeenCalledWith({ animalId: 'vaca', fechaConfirmacion: '2026-01-01', fechaInicioEstimada: undefined, observaciones: 'Comprada gestante, revisión registrada' }))
})
