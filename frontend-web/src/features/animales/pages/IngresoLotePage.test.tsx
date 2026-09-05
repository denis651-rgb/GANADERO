import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { IngresoLotePage } from './IngresoLotePage'
import { calcularNacimientoEstimado } from '@/features/animales/edad'
import { todayInBolivia } from '@/shared/utils/date'

const createAnimalesLote = vi.fn()
const createMovimiento = vi.fn()
const confirmarMovimiento = vi.fn()

vi.mock('@/features/animales/api', () => ({
  createAnimalesLote: (...args: unknown[]) => createAnimalesLote(...args),
  getAnimal: vi.fn(),
  listCategorias: vi.fn().mockResolvedValue([{ id: 'cat-1', codigo: 'VAQUILLA', nombre: 'Vaquillona', sexoAplicable: 'AMBOS', edadMinMeses: 13, edadMaxMeses: 35, activo: true, clasificacionAutomatica: true, ordenEvaluacion: 0 }]),
  listRazas: vi.fn().mockResolvedValue([{ id: 'raza-1', codigo: 'BRAH', nombre: 'Brahman', especie: 'BOVINO' }]),
}))
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([{ id: 'prop-1', nombre: 'La Esperanza', activo: true }]),
}))
vi.mock('@/features/potreros/api', () => ({
  listPotreros: vi.fn().mockResolvedValue([{ id: 'pot-1', propiedadId: 'prop-1', nombre: 'Potrero Norte', activo: true }]),
}))
vi.mock('@/features/movimientos/api', () => ({
  createMovimiento: (...args: unknown[]) => createMovimiento(...args),
  confirmarMovimiento: (...args: unknown[]) => confirmarMovimiento(...args),
}))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<MemoryRouter><QueryClientProvider client={client}><IngresoLotePage /></QueryClientProvider></MemoryRouter>)
}

describe('IngresoLotePage', () => {
  it('agrupa los animales en una tabla y conserva los datos al quitar una fila', async () => {
    renderPage()
    await screen.findByText('Brahman')
    expect(screen.getByRole('button', { name: 'Quitar fila 1' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Agregar fila' }))
    fireEvent.click(screen.getByRole('button', { name: 'Agregar fila' }))
    const tabla = screen.getByRole('table', { name: 'Animales del lote' })
    expect(screen.getAllByRole('table')).toHaveLength(1)
    expect(within(tabla).getAllByRole('row')).toHaveLength(4)
    fireEvent.change(screen.getByLabelText('Nombre del animal 3'), { target: { value: 'LB-03' } })
    fireEvent.click(screen.getByRole('button', { name: 'Quitar fila 2' }))
    expect(within(tabla).getAllByRole('row')).toHaveLength(3)
    expect(screen.getByLabelText('Nombre del animal 2')).toHaveValue('LB-03')
  })

  it('inicia con el día de Bolivia aunque UTC ya sea mañana', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    try {
      vi.setSystemTime(new Date('2026-09-04T01:00:00Z'))
      renderPage()
      expect(screen.getByLabelText(/^Fecha de ingreso/)).toHaveValue('2026-09-03')
      expect(screen.getByLabelText(/^Fecha de ingreso/)).toHaveAttribute('max', '2026-09-03')
      fireEvent.change(screen.getByLabelText('Nacimiento del animal 1'), { target: { value: 'CONOCIDA' } })
      expect(screen.getByLabelText(/^Fecha de nacimiento/)).toHaveAttribute('max', '2026-09-03')
    } finally {
      vi.useRealTimers()
    }
  })

  it('crea el lote con 2 filas y encadena el movimiento de ingreso por compra', async () => {
    createAnimalesLote.mockResolvedValue([
      { id: 'a-1', codigo: 'ANI-000001', version: 0 },
      { id: 'a-2', codigo: 'ANI-000002', version: 0 },
    ])
    createMovimiento.mockResolvedValue({ id: 'mov-1', version: 0 })
    confirmarMovimiento.mockResolvedValue({ id: 'mov-1' })

    renderPage()

    await screen.findByText('Brahman') // espera a que resuelvan los catálogos (raza/categoría/propiedad/potrero)
    fireEvent.change(screen.getByLabelText(/^Raza/), { target: { value: 'raza-1' } })
    fireEvent.change(screen.getByLabelText(/^Propiedad/), { target: { value: 'prop-1' } })
    fireEvent.change(screen.getByLabelText(/^Potrero/), { target: { value: 'pot-1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Agregar fila' }))
    const categorias = screen.getAllByLabelText(/^Categoría/)
    expect(categorias).toHaveLength(2)
    fireEvent.change(categorias[0], { target: { value: 'cat-1' } })
    fireEvent.change(categorias[1], { target: { value: 'cat-1' } })
    fireEvent.change(screen.getByLabelText('Peso al ingreso (kg) del animal 1'), { target: { value: '150' } })
    fireEvent.change(screen.getByLabelText('Peso al ingreso (kg) del animal 2'), { target: { value: '180' } })
    fireEvent.change(screen.getByLabelText('Tipo de peso del animal 2'), { target: { value: 'MEDIDO' } })
    fireEvent.change(screen.getByLabelText('Nacimiento del animal 1'), { target: { value: 'EDAD_APROXIMADA' } })
    fireEvent.change(screen.getByLabelText('Edad aproximada del animal 1'), { target: { value: '18' } })
    const nacimientoEsperado = calcularNacimientoEstimado(todayInBolivia(), 18, 'MESES')
    expect(screen.getByText(/Nacimiento estimado:/)).toHaveTextContent(nacimientoEsperado!)
    fireEvent.change(screen.getByLabelText('Nacimiento del animal 2'), { target: { value: 'CONOCIDA' } })
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento del animal 2'), { target: { value: '2025-09-03' } })

    expect(screen.queryByLabelText(/^Código/)).not.toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Código' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar lote' }))

    await waitFor(() => expect(createAnimalesLote).toHaveBeenCalledOnce())
    expect(createAnimalesLote).toHaveBeenCalledWith(expect.objectContaining({
      razaPrincipalId: 'raza-1',
      propiedadActualId: 'prop-1',
      potreroActualId: 'pot-1',
      animales: [
        expect.objectContaining({ categoriaActualId: 'cat-1', sexo: 'HEMBRA', pesoIngresoKg: 150, pesoIngresoEstimado: true, fechaNacimiento: undefined, fechaNacimientoEstimada: false, edadDeclaradaValor: 18, edadDeclaradaUnidad: 'MESES', fuenteEdad: 'PROVEEDOR' }),
        expect.objectContaining({ categoriaActualId: 'cat-1', sexo: 'HEMBRA', pesoIngresoKg: 180, pesoIngresoEstimado: false, fechaNacimiento: '2025-09-03', fechaNacimientoEstimada: false }),
      ],
    }))

    for (const animal of createAnimalesLote.mock.calls[0][0].animales) {
      expect(animal).not.toHaveProperty('codigo')
      expect(animal).not.toHaveProperty('pesoNacimientoKg')
    }

    await waitFor(() => expect(createMovimiento).toHaveBeenCalledWith(expect.objectContaining({
      tipo: 'INGRESO_COMPRA',
      destinoPropiedadId: 'prop-1',
      destinoPotreroId: 'pot-1',
      animales: [{ animalId: 'a-1', version: 0 }, { animalId: 'a-2', version: 0 }],
    })))
    expect(confirmarMovimiento).toHaveBeenCalledWith('mov-1', 0)

    expect(await screen.findByText('2 animal(es) registrado(s)')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Declarar historial sanitario' })).toHaveLength(2)
  })

  it('no encadena movimiento de cuarentena si el checkbox no está marcado', async () => {
    createAnimalesLote.mockResolvedValue([{ id: 'a-1', codigo: 'ANI-000001', version: 0 }])
    createMovimiento.mockResolvedValue({ id: 'mov-1', version: 0 })
    confirmarMovimiento.mockResolvedValue({ id: 'mov-1' })
    createMovimiento.mockClear()
    confirmarMovimiento.mockClear()

    renderPage()

    await screen.findByText('Brahman') // espera a que resuelvan los catálogos (raza/categoría/propiedad/potrero)
    fireEvent.change(screen.getByLabelText(/^Raza/), { target: { value: 'raza-1' } })
    fireEvent.change(screen.getByLabelText(/^Propiedad/), { target: { value: 'prop-1' } })
    fireEvent.change(screen.getByLabelText(/^Potrero/), { target: { value: 'pot-1' } })
    fireEvent.change(screen.getByLabelText(/^Categoría/), { target: { value: 'cat-1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Registrar lote' }))

    await waitFor(() => expect(confirmarMovimiento).toHaveBeenCalledOnce())
    expect(createMovimiento).toHaveBeenCalledOnce()
  })
})
