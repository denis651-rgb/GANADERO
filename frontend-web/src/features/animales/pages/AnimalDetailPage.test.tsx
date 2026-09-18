import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AnimalDetailPage } from './AnimalDetailPage'
import { getAnimal, getAnimalTimeline } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'

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
const listPropiedades = vi.fn().mockResolvedValue([])
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: (...args: unknown[]) => listPropiedades(...args) }))
const listAllPotreros = vi.fn().mockResolvedValue([])
vi.mock('@/features/potreros/api', () => ({ listAllPotreros: (...args: unknown[]) => listAllPotreros(...args) }))
const listLotes = vi.fn().mockResolvedValue({ content: [], page: 0, size: 500, totalElements: 0, totalPages: 0 })
vi.mock('@/features/lotes/api', () => ({ listLotes: (...args: unknown[]) => listLotes(...args) }))
vi.mock('@/features/compras/api', () => ({ getResumenCompraAnimal: vi.fn().mockResolvedValue(null) }))
vi.mock('@/features/pesajes/api', () => ({ getPesajeHistory: vi.fn().mockResolvedValue([]) }))
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

describe('AnimalDetailPage línea de tiempo: nombres en vez de IDs', () => {
  it('muestra el nombre de la propiedad y del potrero destino, no sus UUID', async () => {
    listPropiedades.mockResolvedValueOnce([{ id: 'prop-1', nombre: 'Hacienda Santa Bárbara', activo: true }])
    listAllPotreros.mockResolvedValueOnce([{ id: 'pot-1', propiedadId: 'prop-1', nombre: 'Potrero 4 - Las Palmas', activo: true }])
    vi.mocked(getAnimalTimeline).mockResolvedValueOnce({
      content: [{
        id: 'ev-1', tipo: 'MOVIMIENTO_REGISTRADO', titulo: 'Movimiento registrado', descripcion: undefined,
        fechaTecnica: '2026-03-01T00:00:00Z', fechaEvento: '2026-03-01T00:00:00Z', moduloOrigen: 'MOVIMIENTOS',
        origenSync: false, metadata: { destinoPropiedadId: 'prop-1', destinoPotreroId: 'pot-1', destinoLoteId: '' },
      }],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderPage()
    expect(await screen.findByText(/Propiedad destino: Hacienda Santa Bárbara/)).toBeInTheDocument()
    expect(screen.getByText(/Potrero destino: Potrero 4 - Las Palmas/)).toBeInTheDocument()
    expect(screen.queryByText(/prop-1/)).not.toBeInTheDocument()
    expect(screen.queryByText(/pot-1/)).not.toBeInTheDocument()
  })

  it('oculta un UUID que no puede resolver a un nombre en lugar de mostrarlo crudo', async () => {
    vi.mocked(getAnimalTimeline).mockResolvedValueOnce({
      content: [{
        id: 'ev-2', tipo: 'SERVICIO_REGISTRADO', titulo: 'Servicio registrado', descripcion: 'Monta natural',
        fechaTecnica: '2026-03-02T00:00:00Z', fechaEvento: '2026-03-02T00:00:00Z', moduloOrigen: 'REPRODUCCION',
        origenSync: false, metadata: { celoId: '11111111-2222-3333-4444-555555555555' },
      }],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderPage()
    expect(await screen.findByText('Monta natural')).toBeInTheDocument()
    expect(screen.queryByText(/11111111-2222/)).not.toBeInTheDocument()
  })

  it('resuelve el macho referenciado a su código y nombre de animal', async () => {
    const machoId = '11111111-1111-1111-1111-111111111111'
    vi.mocked(getAnimalTimeline).mockResolvedValueOnce({
      content: [{
        id: 'ev-3', tipo: 'SERVICIO_REGISTRADO', titulo: 'Servicio registrado', descripcion: undefined,
        fechaTecnica: '2026-03-03T00:00:00Z', fechaEvento: '2026-03-03T00:00:00Z', moduloOrigen: 'REPRODUCCION',
        origenSync: false, metadata: { machoId },
      }],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    vi.mocked(getAnimal).mockImplementation((animalId: string) => Promise.resolve((
      animalId === machoId ? { id: machoId, codigo: 'ANI-000050', nombre: 'Lucero', estado: 'ACTIVO', sexo: 'MACHO', version: 0 }
        : { id: 'a-1', codigo: 'A-001', nombre: 'Luna', estado: 'ACTIVO', sexo: 'HEMBRA', version: 3 }
    ) as AnimalSummary))
    renderPage()
    expect(await screen.findByText(/Padre \(macho\): ANI-000050 · Lucero/)).toBeInTheDocument()
  })

  it('no repite el código del lote cuando el id del lote ya se resolvió a un nombre', async () => {
    listLotes.mockResolvedValueOnce({ content: [{ id: 'lote-9', nombre: 'Lote Norte' }], page: 0, size: 500, totalElements: 1, totalPages: 1 })
    vi.mocked(getAnimalTimeline).mockResolvedValueOnce({
      content: [{
        id: 'ev-4', tipo: 'LOTE_CAMBIADO', titulo: 'Lote cambiado', descripcion: undefined,
        fechaTecnica: '2026-03-04T00:00:00Z', fechaEvento: '2026-03-04T00:00:00Z', moduloOrigen: 'LOTE',
        origenSync: false, metadata: { loteNuevoId: 'lote-9', loteNuevoCodigo: 'L-09' },
      }],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderPage()
    expect(await screen.findByText(/Lote nuevo: Lote Norte/)).toBeInTheDocument()
    expect(screen.queryByText(/L-09/)).not.toBeInTheDocument()
  })
})
