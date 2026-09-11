import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ReportesPage } from './ReportesPage'

const getReporteNacimientos = vi.fn()
const getReporteMuertes = vi.fn()
const getReporteVentas = vi.fn()

vi.mock('@/features/reportes/api', () => ({
  getReporteNacimientos: (...args: unknown[]) => getReporteNacimientos(...args),
  getReporteMuertes: (...args: unknown[]) => getReporteMuertes(...args),
  getReporteVentas: (...args: unknown[]) => getReporteVentas(...args),
}))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><ReportesPage /></QueryClientProvider>)
}

describe('ReportesPage', () => {
  it('muestra los datos del período por defecto (trimestre actual)', async () => {
    getReporteNacimientos.mockResolvedValue([
      { animalId: 'a1', codigo: 'ANI-000001', nombre: 'Luna', sexo: 'HEMBRA', fechaNacimiento: '2026-02-01', fechaNacimientoEstimada: false, raza: 'Brahman', categoria: 'Ternera' },
    ])
    getReporteMuertes.mockResolvedValue([
      { animalId: 'a2', codigo: 'ANI-000002', sexo: 'HEMBRA', raza: 'Nelore', categoria: 'Vaca', fechaMuerte: '2026-02-10', motivo: 'Neumonía' },
    ])
    getReporteVentas.mockResolvedValue([
      { ventaId: 'v1', animalId: 'a3', codigo: 'ANI-000003', raza: 'Brahman', fechaVenta: '2026-02-15', comprador: 'Frigorífico Norte', precio: 5000, moneda: 'BOB', modalidad: 'EN_PIE' },
    ])

    renderPage()

    await waitFor(() => expect(screen.getByText('ANI-000001')).toBeInTheDocument())
    expect(screen.getByText('Neumonía')).toBeInTheDocument()
    expect(screen.getByText('Frigorífico Norte')).toBeInTheDocument()
  })

  it('cambia a un rango personalizado y consulta con las fechas elegidas', async () => {
    getReporteNacimientos.mockResolvedValue([])
    getReporteMuertes.mockResolvedValue([])
    getReporteVentas.mockResolvedValue([])
    renderPage()

    fireEvent.change(screen.getByLabelText('Tipo de período'), { target: { value: 'PERSONALIZADO' } })
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2025-01-01' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2025-06-30' } })

    await waitFor(() => expect(getReporteNacimientos).toHaveBeenCalledWith('2025-01-01', '2025-06-30'))
    expect(getReporteMuertes).toHaveBeenCalledWith('2025-01-01', '2025-06-30')
    expect(getReporteVentas).toHaveBeenCalledWith('2025-01-01', '2025-06-30')
  })
})
