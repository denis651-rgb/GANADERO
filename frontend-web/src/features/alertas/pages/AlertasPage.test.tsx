import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AlertasPage } from './AlertasPage'
import type { GanaderoAlert } from '../api'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
const alerts: GanaderoAlert[] = vi.hoisted(() => [{
  id: 'alerta-1',
  tipo: 'MOVIMIENTO_PENDIENTE',
  titulo: 'Sugerencia: enviar el lote a cuarentena',
  mensaje: 'El lote recién ingresado por compra puede enviarse a cuarentena.',
  severidad: 'WARNING',
  fechaProgramada: '2026-09-17T14:15:44.150Z',
  origenTipo: 'INGRESO_COMPRA_CUARENTENA',
  origenId: 'mov-123',
  estado: 'PENDIENTE',
  metadata: {},
}])
vi.mock('../api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api')>()),
  listAlerts: vi.fn().mockResolvedValue(alerts),
  getAlertSummary: vi.fn().mockResolvedValue({ pendientes: 1, urgentes: 0, hoy: 0, resueltas: 0 }),
  listReminders: vi.fn().mockResolvedValue([]),
  attendAlert: vi.fn(),
  resolveAlert: vi.fn(),
}))

describe('AlertasPage', () => {
  it('enlaza la sugerencia de cuarentena al movimiento de origen, no a la misma página', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><MemoryRouter><AlertasPage /></MemoryRouter></QueryClientProvider>)
    const card = await screen.findByText('Sugerencia: enviar el lote a cuarentena')
    const link = within(card.closest('.notification-card') as HTMLElement).getByRole('link', { name: 'Ver' })
    expect(link).toHaveAttribute('href', '/movimientos?movimientoId=mov-123')
  })
})
