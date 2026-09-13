import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { JornadaPrepararModal } from './JornadaPrepararModal'
import { obtenerElegibilidadJornada, type JornadaSanitaria } from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'

vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listPlanes: vi.fn().mockResolvedValue([{ id: 'plan-1', estado: 'ACTIVO' }]),
    listPlanItems: vi.fn().mockResolvedValue([{ id: 'item-1', activo: true, tipoActividad: 'VACUNACION' }]),
    obtenerElegibilidadJornada: vi.fn().mockResolvedValue({ elegibles: [], noElegibles: [] }),
  }
})

const jornada = { id: 'j-1', tipoJornada: 'VACUNACION' } as JornadaSanitaria
const catalogs = { categories: [] } as unknown as SanidadCatalogs

function renderModal(preseleccionAnimalIds?: string[]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <JornadaPrepararModal jornada={jornada} catalogs={catalogs} preseleccionAnimalIds={preseleccionAnimalIds} onClose={vi.fn()} onSaved={vi.fn()} />
    </QueryClientProvider>,
  )
}

describe('JornadaPrepararModal', () => {
  it('no muestra verificación bloqueada antes de seleccionar una actividad', async () => {
    renderModal()

    expect(await screen.findByRole('option', { name: 'Vacunación' })).toBeInTheDocument()
    expect(screen.queryByText('Verificando animales…')).not.toBeInTheDocument()
    expect(screen.getByText('Selecciona una actividad del plan para verificar los animales elegibles.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar a confirmación' })).toBeDisabled()
  })

  it('preselecciona los animales de la visita anterior que siguen siendo elegibles', async () => {
    vi.mocked(obtenerElegibilidadJornada).mockResolvedValueOnce({
      elegibles: [
        { id: 'a-1', codigo: 'BOV-001', sexo: 'MACHO', estado: 'ACTIVO', edadEstimada: false, elegible: true, motivos: [] },
        { id: 'a-2', codigo: 'BOV-002', sexo: 'HEMBRA', estado: 'ACTIVO', edadEstimada: false, elegible: true, motivos: [] },
      ],
      noElegibles: [],
    })
    renderModal(['a-1', 'a-99'])

    await screen.findByRole('option', { name: 'Vacunación' })
    fireEvent.change(screen.getByLabelText(/Actividad del plan/), { target: { value: 'item-1' } })

    expect(await screen.findByText(/Se preseleccionaron 1 animal/)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Seleccionar BOV-001' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Seleccionar BOV-002' })).not.toBeChecked()
    expect(screen.getByText('1 animal(es) seleccionados.')).toBeInTheDocument()
  })

  it('un clic en un animal preseleccionado lo saca de la selección', async () => {
    vi.mocked(obtenerElegibilidadJornada).mockResolvedValueOnce({
      elegibles: [{ id: 'a-1', codigo: 'BOV-001', sexo: 'MACHO', estado: 'ACTIVO', edadEstimada: false, elegible: true, motivos: [] }],
      noElegibles: [],
    })
    renderModal(['a-1'])

    await screen.findByRole('option', { name: 'Vacunación' })
    fireEvent.change(screen.getByLabelText(/Actividad del plan/), { target: { value: 'item-1' } })
    const checkbox = await screen.findByRole('checkbox', { name: 'Seleccionar BOV-001' })
    expect(checkbox).toBeChecked()

    fireEvent.click(checkbox)
    expect(checkbox).not.toBeChecked()
  })
})
