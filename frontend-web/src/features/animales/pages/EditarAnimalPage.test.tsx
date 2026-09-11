import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { EditarAnimalPage } from './EditarAnimalPage'

const updateAnimal = vi.fn().mockResolvedValue({})
vi.mock('@/features/animales/api', () => ({
  updateAnimal: (...args: unknown[]) => updateAnimal(...args),
  getAnimal: vi.fn().mockResolvedValue({
    id: 'a-1', codigo: 'ANI-000001', nombre: 'LB-01', sexo: 'MACHO', origen: 'COMPRADO',
    estado: 'ACTIVO', proposito: 'CARNE', razaPrincipalId: 'r-1', categoriaActualId: 'c-1',
    propiedadActualId: 'p-1', potreroActualId: 'pot-1', fechaIngreso: '2026-09-03',
    fechaNacimiento: '2025-09-03', fechaNacimientoEstimada: true, pesoNacimientoKg: 150, version: 2,
    condicionCorporalActual: null, pesoIngresoKg: null, pesoIngresoEstimado: null, precioAdquisicion: 5600,
  }),
  listRazas: vi.fn().mockResolvedValue([{ id: 'r-1', nombre: 'Nelore' }]),
  listCategorias: vi.fn().mockResolvedValue([{ id: 'c-1', codigo: 'NOVILLO', nombre: 'Novillo', sexoAplicable: 'MACHO', edadMinMeses: 0, edadMaxMeses: 35, activo: true, clasificacionAutomatica: true, ordenEvaluacion: 0 }]),
}))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', nombre: 'Finca', activo: true }]) }))
vi.mock('@/features/potreros/api', () => ({ listAllPotreros: vi.fn().mockResolvedValue([{ id: 'pot-1', nombre: 'Corral', propiedadId: 'p-1', activo: true }]) }))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/animales/a-1/editar']}><Routes>
    <Route path="/animales/:id/editar" element={<EditarAnimalPage />} />
    <Route path="/animales/:id" element={<p>Guardado</p>} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

describe('EditarAnimalPage: datos desconocidos y corrección de compra', () => {
  it('requiere confirmación para trasladar el peso y permite eliminar una fecha desconocida', async () => {
    renderPage()
    const confirmacion = await screen.findByLabelText('Este peso corresponde a la compra, no al nacimiento')
    expect(confirmacion).not.toBeChecked()
    expect(screen.getByLabelText('Peso al ingreso (kg)')).toHaveValue(null)
    fireEvent.click(confirmacion)
    expect(screen.getByLabelText('Peso al ingreso (kg)')).toHaveValue(150)
    expect(screen.getByLabelText('Peso al nacer (kg)')).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Nacimiento'), { target: { value: 'DESCONOCIDA' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(updateAnimal).toHaveBeenCalledWith('a-1', expect.objectContaining({
      pesoIngresoKg: 150, pesoIngresoEstimado: true, corregirPesoCompra: true,
      fechaNacimiento: undefined, fechaNacimientoEstimada: false, quitarFechaNacimiento: true, version: 2,
      condicionCorporalActual: undefined,
    })))
  })

  it('conserva el peso al nacer sin confirmación y permite registrar un peso medido separado', async () => {
    renderPage()
    await screen.findByLabelText('Este peso corresponde a la compra, no al nacimiento')
    fireEvent.change(screen.getByLabelText('Peso al ingreso (kg)'), { target: { value: '180' } })
    fireEvent.change(screen.getByLabelText('Tipo de peso al ingreso'), { target: { value: 'false' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(updateAnimal).toHaveBeenCalledWith('a-1', expect.objectContaining({
      pesoIngresoKg: 180, pesoIngresoEstimado: false, pesoNacimientoKg: 150, corregirPesoCompra: false,
      fechaNacimiento: '2025-09-03', fechaNacimientoEstimada: true, quitarFechaNacimiento: false,
    })))
  })
})
