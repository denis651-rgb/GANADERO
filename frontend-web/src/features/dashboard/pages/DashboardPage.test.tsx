import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from './DashboardPage'

vi.mock('@/features/dashboard/api', () => ({
  getDashboardResumen: vi.fn().mockResolvedValue({
    totalAnimales: 42, animalesEnPotrero: 38, lotesActivos: 3, potrerosActivos: 6,
    pesoPromedioKg: 384.5, gananciaPromedioKg: 0.72, pesajesUltimos7Dias: 8,
    movimientosUltimos7Dias: 4, animalesSinPesaje: 5, animalesPorCategoria: [],
    animalesPorPotrero: [], animalesPorLote: [], pesajesRecientes: [], alertas: [],
    generadoEn: '2026-08-10T12:00:00Z',
  }),
}))
vi.mock('dexie-react-hooks', () => ({ useLiveQuery: (_callback: unknown, _deps: unknown[], fallback: unknown) => fallback }))
vi.mock('@/shared/hooks/useOnlineStatus', () => ({ useOnlineStatus: () => true }))
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ user: { displayName: 'Juan Pérez', propertyIds: [] }, can: () => true }) }))

describe('DashboardPage operativo', () => {
  it('prioriza indicadores y acciones ganaderas sin estados internos de módulos', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    expect(await screen.findByText('42')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Atención requerida' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Registrar animal/ })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Registrar pesaje/ })).not.toHaveLength(0)
    expect(screen.queryByText('Módulos completados')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Módulos' })).not.toBeInTheDocument()
  })
})
