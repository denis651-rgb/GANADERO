import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConfiguracionGeneralPage } from './ConfiguracionGeneralPage'
import { getConfiguracion } from '@/features/configuracion/api'

const updateConfiguracion = vi.fn()
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))
vi.mock('@/features/configuracion/api', () => ({
  getConfiguracion: vi.fn().mockResolvedValue({
    zonaHoraria: 'America/La_Paz', moneda: 'BOB', unidadPeso: 'KG', unidadSuperficie: 'HA',
    diasAlertaPreparto: 15, diasSinPesaje: 30, diasAlertaDestete: 7,
    diasDiagnosticoPostServicio: 30, diasGestacionEstimada: 285, horaAvisos: '08:00', comprimirImagenes: true, calidadImagen: 80,
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

  it('avisa de forma permanente que el bloqueo con PIN todavía no está en funcionamiento', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ConfiguracionGeneralPage /></QueryClientProvider>)

    const aviso = await screen.findByRole('note')
    expect(aviso).toHaveTextContent('El bloqueo con PIN todavía no está en funcionamiento.')
    expect(aviso).toHaveTextContent('no lo pide al abrirse')
    expect(screen.queryByText(/cualquiera puede abrir la aplicación/)).not.toBeInTheDocument()
    expect(screen.queryByText(/tiene un PIN configurado/)).not.toBeInTheDocument()
  })

  it('con un PIN guardado mantiene el aviso y aclara que queda para cuando el bloqueo esté disponible', async () => {
    vi.mocked(getConfiguracion).mockResolvedValueOnce({
      zonaHoraria: 'America/La_Paz', moneda: 'BOB', unidadPeso: 'KG', unidadSuperficie: 'HA',
      diasAlertaPreparto: 15, diasSinPesaje: 30, diasAlertaDestete: 7,
      diasDiagnosticoPostServicio: 30, diasGestacionEstimada: 285, horaAvisos: '08:00', comprimirImagenes: true, calidadImagen: 80,
      pinConfigurado: true, version: 1,
    } as Awaited<ReturnType<typeof getConfiguracion>>)
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ConfiguracionGeneralPage /></QueryClientProvider>)

    expect(await screen.findByRole('note')).toHaveTextContent('todavía no está en funcionamiento')
    expect(screen.getByText('Hay un PIN guardado para cuando el bloqueo esté disponible.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cambiar PIN' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Quitar PIN' })).toBeInTheDocument()
  })

  it('muestra la hora de los avisos y la envía al guardar', async () => {
    updateConfiguracion.mockReset().mockResolvedValue({})
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ConfiguracionGeneralPage /></QueryClientProvider>)

    const hora = await screen.findByLabelText(/^Hora de los avisos/)
    expect(hora).toHaveValue('08:00')
    fireEvent.change(hora, { target: { value: '09:30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }))

    await waitFor(() => expect(updateConfiguracion).toHaveBeenCalledOnce())
    expect(updateConfiguracion).toHaveBeenCalledWith(expect.objectContaining({ horaAvisos: '09:30' }))
  })
})
