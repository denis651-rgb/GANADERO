import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PropiedadesPage } from './PropiedadesPage'

const updatePropiedad = vi.fn()
const updateConfiguracion = vi.fn()
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', codigo: 'PRP-001', nombre: 'La Esperanza', activo: true, version: 2 }]),
  listSectores: vi.fn().mockResolvedValue([]), createPropiedad: vi.fn(), createSector: vi.fn(),
  updatePropiedad: (...args: unknown[]) => updatePropiedad(...args), updateSector: vi.fn(),
}))
vi.mock('@/features/configuracion/api', () => ({
  getConfiguracion: vi.fn().mockResolvedValue({
    zonaHoraria: 'America/La_Paz', moneda: 'BOB', unidadPeso: 'KG', unidadSuperficie: 'HA',
    diasAlertaPreparto: 15, diasAlertaVacunacion: 7, diasSinPesaje: 30, diasAlertaDestete: 7,
    diasDiagnosticoPostServicio: 30, diasGestacionEstimada: 285, comprimirImagenes: true, calidadImagen: 80,
    pinConfigurado: false, version: 1,
  }),
  updateConfiguracion: (...args: unknown[]) => updateConfiguracion(...args),
}))

describe('PropiedadesPage operational protection', () => {
  it('bloquea los ajustes fijos y conserva KG al guardar', async () => {
    updateConfiguracion.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PropiedadesPage /></QueryClientProvider>)

    expect(await screen.findByLabelText('Zona horaria')).toBeDisabled()
    expect(screen.getByLabelText('Moneda')).toBeDisabled()
    expect(screen.getByLabelText('Unidad de peso')).toBeDisabled()
    expect(screen.getByLabelText('Calidad de imagen (1-100)')).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }))
    await waitFor(() => expect(updateConfiguracion).toHaveBeenCalledOnce())
    expect(updateConfiguracion).toHaveBeenCalledWith(expect.objectContaining({ unidadPeso: 'KG' }))
    expect(updateConfiguracion.mock.calls[0][0]).not.toHaveProperty('zonaHoraria')
    expect(updateConfiguracion.mock.calls[0][0]).not.toHaveProperty('moneda')
    expect(updateConfiguracion.mock.calls[0][0]).not.toHaveProperty('calidadImagen')
  })

  it('pide confirmación antes de desactivar una propiedad', async () => {
    updatePropiedad.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><PropiedadesPage /></QueryClientProvider>)
    fireEvent.click((await screen.findAllByRole('button', { name: 'Desactivar' }))[0])
    expect(updatePropiedad).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Desactivar propiedad' }))
    await waitFor(() => expect(updatePropiedad).toHaveBeenCalledOnce())
  })
})
