import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { JornadaPrepararModal } from './JornadaPrepararModal'
import type { JornadaSanitaria } from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'

vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listPlanes: vi.fn().mockResolvedValue([{ id: 'plan-1', estado: 'ACTIVO' }]),
    listPlanItems: vi.fn().mockResolvedValue([{ id: 'item-1', activo: true, tipoActividad: 'VACUNACION' }]),
    obtenerElegibilidadJornada: vi.fn().mockResolvedValue({ elegibles: [], noElegibles: [] }),
  }
})

const jornada = { id: 'j-1', tipoJornada: 'VACUNACION' } as JornadaSanitaria
const catalogs = { categories: [] } as unknown as SanidadCatalogs

describe('JornadaPrepararModal', () => {
  it('no muestra verificación bloqueada antes de seleccionar una actividad', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><JornadaPrepararModal jornada={jornada} catalogs={catalogs} onClose={vi.fn()} onSaved={vi.fn()} /></QueryClientProvider>)

    expect(await screen.findByRole('option', { name: 'Vacunación' })).toBeInTheDocument()
    expect(screen.queryByText('Verificando animales…')).not.toBeInTheDocument()
    expect(screen.getByText('Selecciona una actividad del plan para verificar los animales elegibles.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar a confirmación' })).toBeDisabled()
  })
})
