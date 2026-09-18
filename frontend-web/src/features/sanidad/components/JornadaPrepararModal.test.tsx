import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { JornadaPrepararModal } from './JornadaPrepararModal'
import { listPlanItems, obtenerElegibilidadJornada, type JornadaSanitaria, type PlanSanitarioItem } from '@/features/sanidad/api'
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
const catalogsConCategorias = {
  categories: [
    { id: 'c-vaca', nombre: 'Vaca' },
    { id: 'c-vaq', nombre: 'Vaquillona' },
    { id: 'c-ter', nombre: 'Ternero' },
  ],
} as unknown as SanidadCatalogs

function renderModal(preseleccionAnimalIds?: string[], catalogos: SanidadCatalogs = catalogs) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <JornadaPrepararModal jornada={jornada} catalogs={catalogos} preseleccionAnimalIds={preseleccionAnimalIds} onClose={vi.fn()} onSaved={vi.fn()} />
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

  describe('criterios aplicados automáticamente', () => {
    async function criteriosDe(categoriasAplicables: string[] | undefined, categoriaAnimalId?: string) {
      vi.mocked(listPlanItems).mockResolvedValueOnce([
        { id: 'item-1', activo: true, tipoActividad: 'VACUNACION', categoriasAplicables, categoriaAnimalId },
      ] as unknown as PlanSanitarioItem[])
      renderModal(undefined, catalogsConCategorias)
      await screen.findByRole('option', { name: 'Vacunación' })
      fireEvent.change(screen.getByLabelText(/Actividad del plan/), { target: { value: 'item-1' } })
      return screen.findByLabelText('Criterios de elegibilidad')
    }

    it('muestra todas las categorías elegidas en la actividad, no solo la primera', async () => {
      // categoriaAnimalId es el campo antiguo y guarda solo la primera; el servidor filtra con la lista completa.
      const criterios = await criteriosDe(['c-vaca', 'c-vaq', 'c-ter'], 'c-vaca')

      expect(criterios).toHaveTextContent('Categorías: Vaca, Vaquillona, Ternero')
    })

    it('con una sola categoría la muestra en singular', async () => {
      const criterios = await criteriosDe(['c-vaca'], 'c-vaca')

      expect(criterios).toHaveTextContent('Categoría: Vaca')
      expect(criterios).not.toHaveTextContent('Categorías')
    })

    it('sin categorías elegidas indica que aplica a todas', async () => {
      const criterios = await criteriosDe([])

      expect(criterios).toHaveTextContent('Categoría: Todas')
    })

    it('no falla si la actividad no trae la lista de categorías', async () => {
      const criterios = await criteriosDe(undefined)

      expect(criterios).toHaveTextContent('Categoría: Todas')
    })

    it('nombra las categorías que ya no están en el catálogo en vez de ocultarlas', async () => {
      const criterios = await criteriosDe(['c-vaca', 'c-borrada'])

      expect(criterios).toHaveTextContent('Categorías: Vaca, Categoría no disponible')
    })
  })
})
