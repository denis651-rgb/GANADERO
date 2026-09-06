import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MoverLoteWizard } from './MoverLoteWizard'
import type { Lote, Membresia } from '@/features/lotes/api'
import type { PreparacionMovimientoLote } from '@/features/movimientolote/types'

const prepararMovimientoLote = vi.fn()
const confirmarMovimientoLote = vi.fn()
const cancelarPreparacionMovimientoLote = vi.fn().mockResolvedValue(undefined)

vi.mock('@/features/movimientolote/api', () => ({
  prepararMovimientoLote: (...args: unknown[]) => prepararMovimientoLote(...args),
  confirmarMovimientoLote: (...args: unknown[]) => confirmarMovimientoLote(...args),
  cancelarPreparacionMovimientoLote: (...args: unknown[]) => cancelarPreparacionMovimientoLote(...args),
}))
vi.mock('@/features/lotes/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/lotes/api')>('@/features/lotes/api')
  return { ...actual, listLotes: vi.fn().mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }) }
})
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', nombre: 'Finca La Esperanza', activo: true }]),
}))
vi.mock('@/features/potreros/api', () => ({
  listPotreros: vi.fn().mockResolvedValue([
    { id: 'pt-1', propiedadId: 'p-1', nombre: 'Corral', activo: true },
    { id: 'pt-2', propiedadId: 'p-1', nombre: 'Potrero Norte', activo: true },
  ]),
}))

const lote: Lote = {
  id: 'l-1', codigo: 'LOT-001', nombre: 'Toros', propiedadId: 'p-1', estado: 'ACTIVO',
  fechaApertura: '2026-09-01', version: 0, cantidadActual: 2, potreroActualId: 'pt-1',
}

const miembros: Membresia[] = [
  { id: 'm-1', loteId: 'l-1', animalId: 'a-1', animalCodigo: 'ANI-1', animalNombre: 'Toro 1', fechaIngreso: '2026-09-01T00:00:00Z', version: 0 },
  { id: 'm-2', loteId: 'l-1', animalId: 'a-2', animalCodigo: 'ANI-2', animalNombre: 'Toro 2', fechaIngreso: '2026-09-01T00:00:00Z', version: 0 },
]

function preparacion(overrides: Partial<PreparacionMovimientoLote> = {}): PreparacionMovimientoLote {
  return {
    id: 'prep-1', loteOrigenId: 'l-1', propiedadOrigenId: 'p-1', potreroOrigenId: 'pt-1',
    modalidad: 'LOTE_COMPLETO', destinoPropiedadId: 'p-1', destinoPotreroId: 'pt-2', accionLote: 'MANTENER_LOTE',
    fechaEfectiva: '2026-09-05T12:00:00Z', estado: 'VIGENTE', fechaCaptura: '2026-09-05T12:00:00Z',
    fechaExpiracion: '2026-09-05T12:30:00Z', version: 0, totalEncontrados: 2, totalElegibles: 2, totalExcluidos: 0,
    miembros: [
      { animalId: 'a-1', codigo: 'ANI-1', nombre: 'Toro 1', animalVersion: 0, elegible: true, seleccionadoPorDefecto: true, restricciones: [] },
      { animalId: 'a-2', codigo: 'ANI-2', nombre: 'Toro 2', animalVersion: 0, elegible: true, seleccionadoPorDefecto: true, restricciones: [] },
    ],
    ...overrides,
  }
}

function renderWizard(onSuccess = vi.fn(), onClose = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MoverLoteWizard lote={lote} miembrosActivos={miembros} onClose={onClose} onSuccess={onSuccess} /></QueryClientProvider>)
  return { onSuccess, onClose }
}

describe('MoverLoteWizard', () => {
  it('preselecciona todos los integrantes y permite avanzar', async () => {
    renderWizard()
    expect(await screen.findByText('Toro 1')).toBeInTheDocument()
    expect(screen.getByText('2 seleccionado(s)')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    expect(await screen.findByLabelText(/Propiedad de destino/)).toBeInTheDocument()
  })

  it('advierte que el lote quedará dividido si se mantiene la identidad con selección parcial', async () => {
    renderWizard()
    await screen.findByText('Toro 1')
    fireEvent.click(screen.getByLabelText('Seleccionar ANI-2'))
    expect(screen.getByText('1 seleccionado(s)')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    await screen.findByLabelText(/Propiedad de destino/)
    expect(screen.getByText(/quedará dividido/)).toBeInTheDocument()
  })

  it('bloquea confirmar Mantener lote en un movimiento parcial antes de llamar al backend', async () => {
    renderWizard()
    await screen.findByText('Toro 1')
    fireEvent.click(screen.getByLabelText('Seleccionar ANI-2'))
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    await screen.findByLabelText(/Propiedad de destino/)
    fireEvent.change(screen.getByLabelText(/Potrero de destino/), { target: { value: 'pt-2' } })
    fireEvent.click(screen.getByRole('button', { name: /Validar/ }))
    expect(await screen.findByText(/No puedes mantener el mismo lote en un movimiento parcial/)).toBeInTheDocument()
    expect(prepararMovimientoLote).not.toHaveBeenCalled()
  })

  it('completa el flujo de preparación y confirmación con todos los integrantes', async () => {
    prepararMovimientoLote.mockResolvedValue(preparacion())
    confirmarMovimientoLote.mockResolvedValue({
      movimientoId: 'mov-1', loteOrigenId: 'l-1', loteDestinoId: 'l-1', animalesMovidos: 2,
      animalesPermanecenEnOrigen: 0, loteOrigenVacio: true, identidadTransferida: true, tipoMovimiento: 'CAMBIO_POTRERO',
    })
    let resolveConfirmar: (value: unknown) => void = () => {}
    confirmarMovimientoLote.mockReturnValueOnce(new Promise((resolve) => { resolveConfirmar = resolve }))
    const { onSuccess } = renderWizard()

    await screen.findByText('Toro 1')
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    await screen.findByLabelText(/Propiedad de destino/)
    fireEvent.change(screen.getByLabelText(/Potrero de destino/), { target: { value: 'pt-2' } })
    fireEvent.click(screen.getByRole('button', { name: /Validar/ }))

    await waitFor(() => expect(prepararMovimientoLote).toHaveBeenCalledWith('l-1', expect.objectContaining({
      destinoPropiedadId: 'p-1', destinoPotreroId: 'pt-2', accionLote: 'MANTENER_LOTE', modalidadDeclarada: 'LOTE_COMPLETO',
    })))

    expect(await screen.findByText('Elegibles (2)')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Continuar/ }))

    const confirmBtn = await screen.findByRole('button', { name: /Confirmar movimiento/ })
    fireEvent.click(confirmBtn)
    await waitFor(() => expect(confirmBtn).toBeDisabled())
    fireEvent.click(confirmBtn)
    expect(confirmarMovimientoLote).toHaveBeenCalledOnce()
    resolveConfirmar({
      movimientoId: 'mov-1', loteOrigenId: 'l-1', loteDestinoId: 'l-1', animalesMovidos: 2,
      animalesPermanecenEnOrigen: 0, loteOrigenVacio: true, identidadTransferida: true, tipoMovimiento: 'CAMBIO_POTRERO',
    })
    await waitFor(() => expect(confirmarMovimientoLote).toHaveBeenCalledWith('prep-1', expect.objectContaining({
      version: 0, animalIds: expect.arrayContaining(['a-1', 'a-2']),
    })))
    await waitFor(() => expect(onSuccess).toHaveBeenCalledOnce())
  })

  it('muestra los excluidos con su motivo y no permite seleccionarlos', async () => {
    prepararMovimientoLote.mockResolvedValue(preparacion({
      totalElegibles: 1, totalExcluidos: 1,
      miembros: [
        { animalId: 'a-1', codigo: 'ANI-1', nombre: 'Toro 1', animalVersion: 0, elegible: true, seleccionadoPorDefecto: true, restricciones: [] },
        { animalId: 'a-2', codigo: 'ANI-2', nombre: 'Toro 2', animalVersion: 0, elegible: false, motivoExclusion: 'El animal está en cuarentena activa.', seleccionadoPorDefecto: false, restricciones: [] },
      ],
    }))
    renderWizard()
    await screen.findByText('Toro 1')
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    await screen.findByLabelText(/Propiedad de destino/)
    fireEvent.change(screen.getByLabelText(/Potrero de destino/), { target: { value: 'pt-2' } })
    fireEvent.click(screen.getByRole('button', { name: /Validar/ }))

    expect(await screen.findByText('Excluidos (1)')).toBeInTheDocument()
    expect(screen.getByText('El animal está en cuarentena activa.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Mover a ANI-2')).not.toBeInTheDocument()
  })

  it('exige autorizar advertencias sanitarias antes de continuar', async () => {
    prepararMovimientoLote.mockResolvedValue(preparacion({
      miembros: [
        { animalId: 'a-1', codigo: 'ANI-1', nombre: 'Toro 1', animalVersion: 0, elegible: true, seleccionadoPorDefecto: true,
          restricciones: [{ tipo: 'TRATAMIENTO_ACTIVO', severidad: 'ADVERTENCIA', mensaje: 'Tratamiento activo.' }] },
        { animalId: 'a-2', codigo: 'ANI-2', nombre: 'Toro 2', animalVersion: 0, elegible: true, seleccionadoPorDefecto: true, restricciones: [] },
      ],
    }))
    renderWizard()
    await screen.findByText('Toro 1')
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    await screen.findByLabelText(/Propiedad de destino/)
    fireEvent.change(screen.getByLabelText(/Potrero de destino/), { target: { value: 'pt-2' } })
    fireEvent.click(screen.getByRole('button', { name: /Validar/ }))

    await screen.findByText('Elegibles (2)')
    const continuar = screen.getByRole('button', { name: /Continuar/ })
    expect(continuar).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Motivo de autorización para ANI-1: TRATAMIENTO_ACTIVO'), { target: { value: 'Autorizado por el encargado' } })
    expect(continuar).not.toBeDisabled()
  })
})
