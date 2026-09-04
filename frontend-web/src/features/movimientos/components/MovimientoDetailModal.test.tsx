import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import { getAnimal } from '@/features/animales/api'
import { MovimientoDetailModal } from './MovimientoDetailModal'

vi.mock('@/features/animales/api', () => ({ getAnimal: vi.fn() }))

describe('MovimientoDetailModal', () => {
  it('resuelve el nombre de un animal vendido y oculta los datos técnicos', async () => {
    vi.mocked(getAnimal).mockResolvedValue({ id: 'animal-vendido', nombre: 'LB-01', codigo: 'ANI-000001' } as Awaited<ReturnType<typeof getAnimal>>)
    render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MovimientoDetailModal open onClose={vi.fn()} onValidar={vi.fn()} onConfirmar={vi.fn()} onAnular={vi.fn()} onRevertir={vi.fn()} onViewRelated={vi.fn()} pending={{}}
        movimiento={{ id: 'venta', tipo: 'SALIDA_VENTA', estado: 'CONFIRMADO', fechaMovimiento: '2026-09-03', version: 1, usuarioConfirma: '00000000', fechaConfirmacion: '2026-09-04T02:32:17Z' }}
        detalles={[{ id: 'detalle', animalId: 'animal-vendido', animalVersionEsperada: 3, estadoDespues: 'ACTIVO', estadoResultado: 'OK' }]}
        catalogs={{ animales: [], propiedades: [], potreros: [], lotes: [] }} />
    </QueryClientProvider>)
    expect(await screen.findByText('LB-01')).toBeInTheDocument()
    expect(screen.getByText('Venta registrada correctamente')).toBeInTheDocument()
    expect(screen.getByText('03/09/2026')).toBeInTheDocument()
    expect(screen.queryByText('Versión esperada')).not.toBeInTheDocument()
    expect(screen.queryByText('ACTIVO')).not.toBeInTheDocument()
    expect(screen.queryByText('00000000')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Deshacer movimiento' })).toBeInTheDocument()
    expect(screen.getByText('Venta registrada correctamente').tagName).toBe('TD')
    expect(screen.getByText('Venta registrada correctamente')).not.toHaveClass('table-secondary')
  })
})
