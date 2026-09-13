import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, expect, it, vi } from 'vitest'
import { listMovimientos } from '@/features/movimientos/api'
import { LoteMovimientosPanel } from './LoteMovimientosPanel'

const can = vi.fn()
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can }) }))
vi.mock('@/features/movimientos/api', () => ({ listMovimientos: vi.fn() }))
beforeEach(() => { vi.clearAllMocks(); can.mockReturnValue(true) })

function renderPanel() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter><LoteMovimientosPanel loteId="lote-1" /></MemoryRouter></QueryClientProvider>)
  return client
}

it('consulta el movimiento canónico del lote y enlaza su detalle, con paginación', async () => {
  vi.mocked(listMovimientos).mockResolvedValue({ content: [{ id: 'mov-1', tipo: 'CAMBIO_POTRERO', estado: 'CONFIRMADO', fechaMovimiento: '2026-09-11', version: 0 }], page: 0, size: 10, totalPages: 2, totalElements: 11 })
  const client = renderPanel()
  expect(await screen.findByRole('link', { name: 'Ver movimiento y animales' })).toHaveAttribute('href', '/movimientos?movimientoId=mov-1')
  expect(listMovimientos).toHaveBeenCalledWith({ loteId: 'lote-1', page: 0, size: 10 })
  fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
  await waitFor(() => expect(listMovimientos).toHaveBeenCalledWith({ loteId: 'lote-1', page: 1, size: 10 }))
  client.clear()
})

it('no consulta movimientos sin permiso', () => {
  can.mockReturnValue(false)
  const client = renderPanel()
  expect(listMovimientos).not.toHaveBeenCalled()
  expect(screen.queryByText('Movimientos del lote')).not.toBeInTheDocument()
  client.clear()
})

it('muestra el error de consulta sin perder la pantalla', async () => {
  vi.mocked(listMovimientos).mockRejectedValue(new Error('No se pudo cargar el historial'))
  const client = renderPanel()
  expect(await screen.findByRole('alert')).toBeInTheDocument()
  expect(screen.getByText('Movimientos del lote')).toBeInTheDocument()
  client.clear()
})
