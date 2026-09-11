import { render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from './DashboardPage'

vi.mock('@/features/dashboard/api', () => ({
  getDashboardResumen: vi.fn().mockResolvedValue({
    totalAnimales: 42, animalesEnPotrero: 38, lotesActivos: 3, potrerosActivos: 6,
    pesoPromedioKg: 384.5, gananciaPromedioKg: 0.72, pesajesUltimos7Dias: 8,
    movimientosUltimos7Dias: 4, animalesSinPesaje: 5, animalesPorCategoria: [],
    animalesPorPotrero: [], animalesPorLote: [],
    pesajesRecientes: [{ id: 'p1', animalId: 'a1', animalCodigo: 'A-001', animalNombre: 'Vaca 1', fecha: '2026-08-05', pesoKg: 410 }],
    alertas: [{ tipo: 'SANIDAD_VENCIDA', mensaje: 'Tratamientos vencidos', severidad: 'danger', total: 2 }],
    generadoEn: '2026-08-10T12:00:00Z',
  }),
}))
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ user: { displayName: 'Juan Pérez', propertyIds: [] }, can: () => true }) }))

describe('DashboardPage operativo', () => {
  it('prioriza indicadores y acciones ganaderas sin estados internos de módulos', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    expect((await screen.findAllByText('42')).length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Atención requerida' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Registrar animal/ })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Registrar pesaje/ })).not.toHaveLength(0)
    expect(screen.queryByText('Módulos completados')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Módulos' })).not.toBeInTheDocument()
  })

  it('muestra la alerta crítica antes que el recordatorio de pesaje', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    const attentionItems = await screen.findAllByRole('listitem')
    expect(within(attentionItems[0]).getByText('Tratamientos vencidos')).toBeInTheDocument()
    expect(attentionItems[0].querySelector('.dp-att-dot')).toHaveStyle({ background: 'var(--danger)' })
    expect(attentionItems[1].querySelector('.dp-att-dot')).toHaveStyle({ background: 'var(--warning)' })
  })

  it('lista los pesajes recientes en una tabla con columnas alineadas', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    const table = await screen.findByRole('table', { name: /pesajes registrados/i })
    expect(within(table).getByRole('columnheader', { name: 'Peso' })).toBeInTheDocument()
    expect(within(table).getByRole('link', { name: 'Vaca 1' })).toBeInTheDocument()
    expect(within(table).getByText('410 kg')).toBeInTheDocument()
  })
})
