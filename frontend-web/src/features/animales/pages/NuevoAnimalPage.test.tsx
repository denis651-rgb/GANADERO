import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { NuevoAnimalPage } from './NuevoAnimalPage'
import { calcularNacimientoEstimado } from '@/features/animales/edad'
import { todayInBolivia } from '@/shared/utils/date'

const catalogId = '00000000-0000-0000-0000-000000000001'
const createAnimal = vi.fn().mockResolvedValue({ id: 'a-1' })
const crearCompra = vi.fn().mockResolvedValue({ id: 'compra-1', version: 0 })
const confirmarCompra = vi.fn().mockResolvedValue({ id: 'compra-1', version: 1 })
const getCompraDetalles = vi.fn().mockResolvedValue([{ id: 'detalle-1', animalId: 'animal-1', numeroLinea: 1, precioAsignado: 0 }])
vi.mock('@/features/animales/api', () => ({
  createAnimal: (...args: unknown[]) => createAnimal(...args),
  listRazas: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', nombre: 'Nelore' }]),
  listCategorias: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', codigo: 'VAQUILLA', nombre: 'Vaquilla', sexoAplicable: 'HEMBRA', edadMinMeses: 13, edadMaxMeses: 35, activo: true, clasificacionAutomatica: true, ordenEvaluacion: 0 }]),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', nombre: 'Finca', activo: true }]) }))
vi.mock('@/features/potreros/api', () => ({ listPotreros: vi.fn().mockResolvedValue([{ id: '00000000-0000-0000-0000-000000000001', propiedadId: '00000000-0000-0000-0000-000000000001', nombre: 'Corral', activo: true }]) }))
vi.mock('@/features/compras/api', () => ({
  crearCompra: (...args: unknown[]) => crearCompra(...args),
  confirmarCompra: (...args: unknown[]) => confirmarCompra(...args),
  getCompraDetalles: (...args: unknown[]) => getCompraDetalles(...args),
}))
vi.mock('@/features/proveedores/api', () => ({ buscarProveedores: vi.fn().mockResolvedValue([]) }))

function registrarProveedorNuevo(nombre: string) {
  fireEvent.click(screen.getByRole('button', { name: 'Registrar proveedor nuevo' }))
  fireEvent.change(screen.getByLabelText('Nombre', { selector: 'input' }), { target: { value: nombre } })
}

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
    registrarProveedorNuevo('Estancia El Roble')
    fireEvent.click(screen.getByRole('button', { name: 'Guardar animal' }))
    await waitFor(() => expect(crearCompra).toHaveBeenCalledWith(expect.objectContaining({
      proveedorNuevo: expect.objectContaining({ nombre: 'Estancia El Roble' }),
      detalles: [expect.objectContaining({
        pesoIngresoKg: 150, tipoPeso: tipo === 'true' ? 'ESTIMADO' : 'MEDIDO', fechaNacimiento: undefined,
        fechaNacimientoEstimada: false,
      })],
    })))
    expect(confirmarCompra).toHaveBeenCalledWith('compra-1', 0)
  })

  it('envía la edad aproximada y su procedencia sin inventar una fecha exacta', async () => {
    crearCompra.mockClear()
    const hoy = todayInBolivia()
    const nacimientoEsperado = calcularNacimientoEstimado(hoy, 18, 'MESES')
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><MemoryRouter><NuevoAnimalPage /></MemoryRouter></QueryClientProvider>)
    await screen.findByText('Nelore')
    for (const label of ['Raza', 'Categoría', 'Propiedad', 'Potrero']) fireEvent.change(screen.getByLabelText(label), { target: { value: catalogId } })
    fireEvent.change(screen.getByLabelText('Origen'), { target: { value: 'COMPRADO' } })
    fireEvent.change(screen.getByLabelText('Nacimiento'), { target: { value: 'EDAD_APROXIMADA' } })
    fireEvent.change(screen.getByLabelText('Edad aproximada'), { target: { value: '18' } })
    expect(screen.getByLabelText('Nacimiento calculado')).toHaveValue(nacimientoEsperado)
    registrarProveedorNuevo('Estancia El Roble')
    fireEvent.click(screen.getByRole('button', { name: 'Guardar animal' }))

    await waitFor(() => expect(crearCompra).toHaveBeenCalledWith(expect.objectContaining({
      detalles: [expect.objectContaining({
        fechaNacimiento: undefined,
        edadDeclaradaValor: 18,
        edadDeclaradaUnidad: 'MESES',
        fuenteEdadDeclarada: 'PROVEEDOR',
      })],
    })))
  })

  it('bloquea el envío de una compra sin proveedor seleccionado', async () => {
    createAnimal.mockClear()
    crearCompra.mockClear()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><MemoryRouter><NuevoAnimalPage /></MemoryRouter></QueryClientProvider>)
    await screen.findByText('Nelore')
    for (const label of ['Raza', 'Categoría', 'Propiedad', 'Potrero']) fireEvent.change(screen.getByLabelText(label), { target: { value: catalogId } })
    fireEvent.change(screen.getByLabelText('Origen'), { target: { value: 'COMPRADO' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar animal' }))
    await screen.findByText('Selecciona o registra el proveedor de la compra.')
    expect(crearCompra).not.toHaveBeenCalled()
  })
})
