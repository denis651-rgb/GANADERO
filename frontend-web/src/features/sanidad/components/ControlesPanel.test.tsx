import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ControlesPanel } from './ControlesPanel'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'

vi.mock('@/features/reproduccion/components/AnimalSearchSelect', () => ({
  AnimalSearchSelect: () => <input aria-label="Animal" />,
}))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listControlesNeonatales: vi.fn().mockResolvedValue([]),
    listControlesEctoparasitarios: vi.fn().mockResolvedValue([]),
    listExamenesReproductivos: vi.fn().mockResolvedValue([]),
  }
})

const catalogs: SanidadCatalogs = {
  properties: [], paddocks: [], categories: [], lots: [],
  animals: [{
    id: 'a-1', codigo: 'ANI-001', nombre: 'Beto', sexo: 'MACHO', estado: 'ACTIVO',
    categoriaActualId: 'c-1', razaPrincipalId: 'r-1', fechaNacimiento: '2024-01-01',
    fechaNacimientoEstimada: false, proposito: 'REPRODUCCION', origen: 'NACIDO',
    propiedadActualId: 'p-1', potreroActualId: 'pot-1', fechaIngreso: '2024-01-01', version: 0,
  }],
  animalLabel: () => 'ANI-001 · Beto',
}

describe('ControlesPanel', () => {
  it('centraliza las acciones y bloquea el control neonatal de un adulto', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ControlesPanel catalogs={catalogs} initialAnimalId="a-1" /></QueryClientProvider>)

    expect(await screen.findByText('Controles individuales')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Registrar control neonatal' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Registrar control ectoparasitario' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Registrar examen reproductivo' })).toBeEnabled()
    expect(await screen.findByText('Sin controles neonatales registrados.')).toBeInTheDocument()
  })
})
