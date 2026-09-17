import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConfiguracionGeneralPage } from './ConfiguracionGeneralPage'

const updateConfiguracion = vi.fn()
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))
vi.mock('@/features/configuracion/api', () => ({
  getConfiguracion: vi.fn().mockResolvedValue({
    zonaHoraria: 'America/La_Paz', moneda: 'BOB', unidadPeso: 'KG', unidadSuperficie: 'HA',
    diasAlertaPreparto: 15, diasSinPesaje: 30, diasAlertaDestete: 7,
    diasDiagnosticoPostServicio: 30, diasGestacionEstimada: 285, comprimirImagenes: true, calidadImagen: 80,
    pinConfigurado: false, version: 1,
  }),
  updateConfiguracion: (...args: unknown[]) => updateConfiguracion(...args),
}))

describe('ConfiguracionGeneralPage operational protection', () => {
  it('bloquea los ajustes fijos y conserva KG al guardar', async () => {
    updateConfiguracion.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ConfiguracionGeneralPage /></QueryClientProvider>)

    expect(await screen.findByLabelText('Zona horaria')).toBeDisabled()
    expect(screen.getByLabelText('Moneda')).toBeDisabled()
    expect(screen.getByLabelText('Unidad de peso')).toBeDisabled()
    expect(screen.getByLabelText('Calidad de imagen (1-100)')).not.toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }))
    await waitFor(() => expect(updateConfiguracion).toHaveBeenCalledOnce())
    expect(updateConfiguracion).toHaveBeenCalledWith(expect.objectContaining({ unidadPeso: 'KG', calidadImagen: 80 }))
    expect(updateConfiguracion.mock.calls[0][0]).not.toHaveProperty('zonaHoraria')
    expect(updateConfiguracion.mock.calls[0][0]).not.toHaveProperty('moneda')
  })
})
