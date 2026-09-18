import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ControlesPanel } from './ControlesPanel'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { todayInBolivia } from '@/shared/utils/date'
import { listControlesEctoparasitarios, listControlesNeonatales, listExamenesReproductivos } from '@/features/sanidad/api'

vi.mock('@/features/reproduccion/components/AnimalSearchSelect', () => ({
  AnimalSearchSelect: () => <input aria-label="Animal" />,
}))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    getConfiguracionSanitaria: vi.fn().mockResolvedValue({ edadMinMachoMeses: 25, edadMinHembraMeses: 25, horizonteProyeccionMeses: 12, version: 1 }),
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
  }, {
    id: 'a-2', codigo: 'ANI-002', nombre: 'Recién nacida', sexo: 'HEMBRA', estado: 'ACTIVO',
    categoriaActualId: 'c-1', razaPrincipalId: 'r-1', fechaNacimiento: todayInBolivia(),
    fechaNacimientoEstimada: false, proposito: 'REPRODUCCION', origen: 'NACIDO',
    propiedadActualId: 'p-1', potreroActualId: 'pot-1', fechaIngreso: todayInBolivia(), version: 0,
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

  it('bloquea el examen reproductivo de un animal que no alcanza la edad mínima', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ControlesPanel catalogs={catalogs} initialAnimalId="a-2" /></QueryClientProvider>)

    const boton = await screen.findByRole('button', { name: 'Registrar examen reproductivo' })
    await waitFor(() => expect(boton).toBeDisabled())
    expect(screen.getByText(/no alcanza la edad mínima configurada de 25 meses/)).toBeInTheDocument()
  })

  it('muestra las fechas de los controles tal como se guardaron, sin correrlas un día', async () => {
    vi.mocked(listControlesNeonatales).mockResolvedValueOnce([
      { id: 'n-1', fechaControl: '2024-01-01', momento: 'DIA_0', calostrado: 'ADECUADO', diarrea: false },
    ] as unknown as Awaited<ReturnType<typeof listControlesNeonatales>>)
    vi.mocked(listControlesEctoparasitarios).mockResolvedValueOnce([
      { id: 'e-1', fecha: '2024-02-01', tipo: 'GARRAPATA', nivelCarga: 'ALTA', tratado: true },
    ] as unknown as Awaited<ReturnType<typeof listControlesEctoparasitarios>>)
    vi.mocked(listExamenesReproductivos).mockResolvedValueOnce([
      { id: 'x-1', fecha: '2024-03-31', resultado: 'APTO', observaciones: 'Sin novedades' },
    ] as unknown as Awaited<ReturnType<typeof listExamenesReproductivos>>)
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ControlesPanel catalogs={catalogs} initialAnimalId="a-1" /></QueryClientProvider>)

    expect(await screen.findByText(/^01\/01\/2024 · /)).toBeInTheDocument()
    expect(screen.getByText(/^01\/02\/2024 · /)).toBeInTheDocument()
    expect(screen.getByText(/^31\/03\/2024 · /)).toBeInTheDocument()
    expect(screen.queryByText(/31\/12\/2023|31\/01\/2024|30\/03\/2024/)).not.toBeInTheDocument()
  })
})
