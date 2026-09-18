import { render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from './DashboardPage'

vi.mock('@/features/dashboard/api', () => ({
  getDashboardResumen: vi.fn().mockResolvedValue({
    totalAnimales: 42, animalesEnPotrero: 38, lotesActivos: 3, potrerosActivos: 6,
    pesoPromedioKg: 384.5, pesajesUltimos7Dias: 8,
    movimientosUltimos7Dias: 4, animalesSinPesaje: 5, animalesPorCategoria: [],
    animalesPorPotrero: [], animalesPorLote: [],
    pesajesRecientes: [{ id: 'p1', animalId: 'a1', animalCodigo: 'A-001', animalNombre: 'Vaca 1', fecha: '2026-08-05', pesoKg: 410 }],
    alertas: [
      { tipo: 'ACTIVIDAD_SANITARIA_VENCIDA', mensaje: 'Actividades sanitarias vencidas', severidad: 'danger', total: 2 },
      { tipo: 'PARTO_PROXIMO', mensaje: 'Partos próximos', severidad: 'warning', total: 1 },
    ],
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

  it('muestra la alerta crítica antes que las advertencias y el recordatorio de pesaje', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    const attentionItems = await screen.findAllByRole('listitem')
    expect(within(attentionItems[0]).getByText('Actividades sanitarias vencidas')).toBeInTheDocument()
    expect(attentionItems[0].querySelector('.dp-att-dot')).toHaveStyle({ background: 'var(--danger)' })
    expect(within(attentionItems[1]).getByText('Partos próximos')).toBeInTheDocument()
    expect(attentionItems[1].querySelector('.dp-att-dot')).toHaveStyle({ background: 'var(--warning)' })
    expect(within(attentionItems[2]).getByText('5 animales sin pesaje reciente')).toBeInTheDocument()
  })

  it('en «Atención requerida» muestra las alertas de sanidad y reproducción con su botón al módulo', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    const seccion = (await screen.findByRole('heading', { name: 'Atención requerida' })).closest('section') as HTMLElement
    expect(await within(seccion).findByText('2 registros requieren atención.')).toBeInTheDocument()
    expect(within(seccion).getByRole('link', { name: 'Ir a Sanidad' })).toHaveAttribute('href', '/sanidad')
    expect(within(seccion).getByRole('link', { name: 'Ir a Reproducción' })).toHaveAttribute('href', '/reproduccion')
    expect(within(seccion).getByRole('link', { name: 'Registrar pesaje' })).toHaveAttribute('href', '/pesajes')
    expect(within(seccion).getByText('3 pendientes')).toBeInTheDocument()
  })

  it('muestra un solo aviso de pesaje y ya no muestra la ganancia diaria', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<MemoryRouter><QueryClientProvider client={client}><DashboardPage /></QueryClientProvider></MemoryRouter>)

    const seccion = (await screen.findByRole('heading', { name: 'Atención requerida' })).closest('section') as HTMLElement
    await within(seccion).findByText('5 animales sin pesaje reciente') // espera a que lleguen los datos
    expect(within(seccion).getAllByText(/sin pesaje/i)).toHaveLength(1)
    expect(screen.queryByText('Ganancia diaria')).not.toBeInTheDocument()
    expect(screen.queryByText(/ganancia diaria negativa/i)).not.toBeInTheDocument()
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
