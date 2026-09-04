import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { GanaderoAlert } from '@/features/alertas/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { ResumenPanel } from './ResumenPanel'

const attendAlert = vi.fn().mockResolvedValue({})
let alertsByType: Record<string, GanaderoAlert[]> = {}

vi.mock('@/features/alertas/api', () => ({
  listAlerts: vi.fn((params?: { tipo?: string }) => Promise.resolve(alertsByType[params?.tipo ?? ''] ?? [])),
  attendAlert: (...args: unknown[]) => attendAlert(...args),
}))

function alerta(overrides: Partial<GanaderoAlert>): GanaderoAlert {
  return {
    id: 'a-1', tipo: 'VACUNA_VENCIDA', titulo: 'Vacuna vencida', mensaje: 'Detalle',
    severidad: 'CRITICA', fechaProgramada: '2026-08-01T00:00:00Z', origenTipo: 'JORNADA',
    estado: 'PENDIENTE', metadata: {}, ...overrides,
  }
}

const catalogs: SanidadCatalogs = { properties: [], paddocks: [], categories: [], lots: [], animals: [], animalLabel: () => '—' }

function renderPanel(onIrA = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ResumenPanel planes={[]} jornadas={[]} casos={[]} tratamientos={[]} catalogs={catalogs} onIrA={onIrA} />
    </QueryClientProvider>,
  )
  return { onIrA }
}

describe('ResumenPanel — atención sanitaria', () => {
  beforeEach(() => {
    attendAlert.mockClear()
  })

  it('muestra "Todo al día" cuando no hay alertas pendientes', async () => {
    alertsByType = {}
    renderPanel()
    expect(await screen.findByText('Todo al día')).toBeInTheDocument()
  })

  it('muestra las alertas críticas antes que las de seguimiento normal', async () => {
    alertsByType = {
      VACUNA_PROXIMA: [alerta({ id: 'w-1', tipo: 'VACUNA_PROXIMA', titulo: 'Vacuna próxima' })],
      VACUNA_VENCIDA: [alerta({ id: 'd-1', tipo: 'VACUNA_VENCIDA', titulo: 'Vacuna vencida' })],
    }
    renderPanel()

    const items = await screen.findAllByRole('listitem')
    expect(items[0].className).toBe('attention-danger')
    expect(items[1].className).toBe('attention-warning')
  })

  it('el link "Ir a" cambia de sección sin navegar', async () => {
    alertsByType = { VACUNA_VENCIDA: [alerta({ id: 'd-1' })] }
    const { onIrA } = renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: 'Ir a Jornadas' }))
    expect(onIrA).toHaveBeenCalledWith('jornadas')
  })

  it('marcar como atendida pide confirmación y llama a attendAlert', async () => {
    alertsByType = { CASO_CLINICO_CRITICO: [alerta({ id: 'c-1', tipo: 'CASO_CLINICO_CRITICO', titulo: 'Caso crítico' })] }
    renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: 'Marcar atendida' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    await waitFor(() => expect(attendAlert).toHaveBeenCalledWith('c-1'))
  })
})
