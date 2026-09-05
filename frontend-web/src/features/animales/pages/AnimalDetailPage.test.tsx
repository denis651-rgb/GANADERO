import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AnimalDetailPage } from './AnimalDetailPage'
import { getAnimal } from '@/features/animales/api'

const changeAnimalState = vi.fn()
vi.mock('@/features/animales/api', () => ({
  changeAnimalState: (...args: unknown[]) => changeAnimalState(...args),
  getAnimal: vi.fn().mockResolvedValue({ id: 'a-1', codigo: 'A-001', nombre: 'Luna', estado: 'ACTIVO', sexo: 'HEMBRA', version: 3 }),
  getAnimalTimeline: vi.fn().mockResolvedValue({ content: [], page: 0, totalPages: 0 }),
  getHistorialCategorias: vi.fn().mockResolvedValue([]),
  listCategorias: vi.fn().mockResolvedValue([]), listRazas: vi.fn().mockResolvedValue([]),
}))
const listAlerts = vi.fn().mockResolvedValue([])
vi.mock('@/features/alertas/api', () => ({ listAlerts: (...args: unknown[]) => listAlerts(...args) }))
const listControlesNeonatales = vi.fn().mockResolvedValue([])
const listControlesEctoparasitarios = vi.fn().mockResolvedValue([])
const listExamenesReproductivos = vi.fn().mockResolvedValue([])
const listTratamientos = vi.fn().mockResolvedValue([])
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listControlesNeonatales: (...args: unknown[]) => listControlesNeonatales(...args),
    listControlesEctoparasitarios: (...args: unknown[]) => listControlesEctoparasitarios(...args),
    listExamenesReproductivos: (...args: unknown[]) => listExamenesReproductivos(...args),
    listTratamientos: (...args: unknown[]) => listTratamientos(...args),
  }
})
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/potreros/api', () => ({ listPotreros: vi.fn().mockResolvedValue([]) }))
vi.mock('@/features/animales/components/GenealogiaTab', () => ({ GenealogiaTab: () => null }))
vi.mock('@/features/animales/components/IdentificadoresTab', () => ({ IdentificadoresTab: () => null }))
vi.mock('@/features/animales/components/FotosTab', () => ({ FotosTab: () => null }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/animales/a-1']}><Routes><Route path="/animales/:id" element={<AnimalDetailPage />} /></Routes></MemoryRouter></QueryClientProvider>)
}

describe('AnimalDetailPage state protection', () => {
  it.each([5600, 0, undefined])('muestra el precio individual %s sin confundir cero con ausencia', async (precioAdquisicion) => {
    const base = await getAnimal('a-1')
    vi.mocked(getAnimal).mockResolvedValueOnce({ ...base, precioAdquisicion })
    renderPage()
    const label = await screen.findByText('Precio de compra')
    expect(label.nextElementSibling).toHaveTextContent(precioAdquisicion == null
      ? 'Sin registro'
      : `${precioAdquisicion.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Bs`)
  })

  it('distingue peso estimado al ingreso de peso al nacer y no inventa el nacimiento', async () => {
    const base = await getAnimal('a-1')
    vi.mocked(getAnimal).mockResolvedValueOnce({
      ...base, origen: 'COMPRADO', pesoIngresoKg: 150, pesoIngresoEstimado: true,
      fechaNacimiento: undefined, fechaNacimientoEstimada: false, fechaIngreso: '2026-09-03',
    })
    renderPage()
    expect(await screen.findByText('150 kg (estimado)')).toBeInTheDocument()
    expect(screen.getByText('Peso al nacer').nextElementSibling).toHaveTextContent('Desconocido')
    expect(screen.getByText('Nacimiento').nextElementSibling).toHaveTextContent('Edad desconocida')
    expect(screen.getByText(/La ausencia de alertas no confirma vacunas/)).toBeInTheDocument()
  })

  it('muestra el calendario sanitario con las alertas activas del animal', async () => {
    listAlerts.mockReset().mockResolvedValue([
      { id: 'al-1', animalId: 'a-1', tipo: 'VACUNA_VENCIDA', titulo: 'Vacuna vencida', mensaje: 'La vacunación de Luna está vencida.', severidad: 'URGENTE', fechaProgramada: '2026-01-01T00:00:00Z', origenTipo: 'PROYECCION_SANITARIA', estado: 'PENDIENTE', metadata: {} },
      { id: 'al-2', animalId: 'a-1', tipo: 'CELO_DETECTADO', titulo: 'Celo detectado', mensaje: 'No debería aparecer aquí.', severidad: 'INFO', fechaProgramada: '2026-01-01T00:00:00Z', origenTipo: 'X', estado: 'PENDIENTE', metadata: {} },
    ])
    renderPage()

    expect(await screen.findByText('Vacuna vencida')).toBeInTheDocument()
    expect(listAlerts).toHaveBeenCalledWith({ animalId: 'a-1' })
    expect(screen.queryByText('Celo detectado')).not.toBeInTheDocument()
  })

  it('no cambia un estado crítico hasta confirmarlo y cancelar no llama la API', async () => {
    listAlerts.mockReset().mockResolvedValue([])
    changeAnimalState.mockReset().mockResolvedValue({})
    renderPage()
    await screen.findByText('A-001 · Luna')
    fireEvent.change(screen.getByRole('combobox', { name: '' }), { target: { value: 'MUERTO' } })
    fireEvent.change(screen.getByPlaceholderText('Motivo del cambio…'), { target: { value: 'Registro confirmado' } })
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar estado' }))

    expect(await screen.findByRole('dialog', { name: 'Confirmar cambio de estado' })).toBeInTheDocument()
    expect(changeAnimalState).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(changeAnimalState).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Actualizar estado' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Confirmar cambio' }))
    await waitFor(() => expect(changeAnimalState).toHaveBeenCalledOnce())
  })

  it('deja la ficha en modo consulta y enlaza a los controles sanitarios del animal', async () => {
    listAlerts.mockReset().mockResolvedValue([])
    listControlesNeonatales.mockReset().mockResolvedValue([])
    renderPage()

    const link = await screen.findByRole('link', { name: 'Ver historial sanitario' })
    expect(link).toHaveAttribute('href', '/sanidad?seccion=controles&animalId=a-1')
    expect(screen.queryByRole('button', { name: 'Registrar control neonatal' })).not.toBeInTheDocument()
  })

  it('implementa roving tabindex y navegación completa por teclado en tabs', async () => {
    renderPage()
    const timeline = await screen.findByRole('tab', { name: 'Línea de tiempo' })
    const identifiers = screen.getByRole('tab', { name: 'Identificadores' })
    const photos = screen.getByRole('tab', { name: 'Fotografías' })
    const genealogy = screen.getByRole('tab', { name: 'Genealogía' })
    expect(timeline).toHaveAttribute('aria-selected', 'true')
    expect(timeline).toHaveAttribute('tabindex', '0')
    expect(identifiers).toHaveAttribute('tabindex', '-1')

    timeline.focus()
    fireEvent.keyDown(timeline, { key: 'ArrowRight' })
    expect(identifiers).toHaveFocus()
    expect(identifiers).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(identifiers, { key: 'End' })
    expect(genealogy).toHaveFocus()
    fireEvent.keyDown(genealogy, { key: 'Home' })
    expect(timeline).toHaveFocus()
    fireEvent.keyDown(timeline, { key: 'ArrowLeft' })
    expect(genealogy).toHaveFocus()
    fireEvent.keyDown(genealogy, { key: 'ArrowLeft' })
    expect(photos).toHaveFocus()
  })
})
