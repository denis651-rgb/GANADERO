import { createElement } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { DiagnosticosPanel } from './DiagnosticosPanel'
import type { AnimalSummary } from '@/features/animales/types'
import type { ReproduccionCatalogs } from '../catalogs'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('./AnimalSearchSelect', () => ({ AnimalSearchSelect: ({ onChange }: { onChange: (id: string, animal?: AnimalSummary) => void }) =>
  createElement('div', null,
    createElement('button', { type: 'button', onClick: () => onChange('con-lote', { id: 'con-lote', loteActualId: 'lote-1' } as AnimalSummary) }, 'Con lote'),
    createElement('button', { type: 'button', onClick: () => onChange('sin-lote', { id: 'sin-lote' } as AnimalSummary) }, 'Sin lote'),
    createElement('button', { type: 'button', onClick: () => onChange('') }, 'Limpiar')) }))

it('completa el nombre del lote y lo limpia al cambiar a un animal sin lote', () => {
  const catalogs = { lots: [{ id: 'lote-1', nombre: 'Vacas reproductoras' }], hembras: [], properties: [], paddocks: [] } as unknown as ReproduccionCatalogs
  render(createElement(QueryClientProvider, { client: new QueryClient() }, createElement(DiagnosticosPanel, {
    diagnosticos: { content: [], page: 0, size: 20, totalPages: 0, totalElements: 0 }, servicios: [], isLoading: false, error: null, catalogs, refresh: vi.fn(),
  })))
  fireEvent.click(screen.getByRole('button', { name: 'Registrar diagnóstico' }))
  fireEvent.click(screen.getByRole('button', { name: 'Con lote' }))
  expect(screen.getByLabelText('Lote')).toHaveValue('Vacas reproductoras')
  expect(screen.getByLabelText('Lote')).toHaveAttribute('readonly')
  fireEvent.click(screen.getByRole('button', { name: 'Sin lote' }))
  expect(screen.getByLabelText('Lote')).toHaveValue('Sin lote asignado')
  fireEvent.click(screen.getByRole('button', { name: 'Limpiar' }))
  expect(screen.getByLabelText('Lote')).toHaveValue('Selecciona primero el animal')
})

it('muestra el nombre del animal y la raza debajo en la tabla sin mostrar el codigo', async () => {
  const catalogs = {
    lots: [],
    properties: [],
    paddocks: [],
    hembras: [{ id: 'ani-1', codigo: 'ANI-000040', nombre: 'LVN-10', razaPrincipalId: 'raza-nelore' } as unknown as AnimalSummary],
    animales: [{ id: 'ani-1', codigo: 'ANI-000040', nombre: 'LVN-10', razaPrincipalId: 'raza-nelore' } as unknown as AnimalSummary],
  } as unknown as ReproduccionCatalogs

  const queryClient = new QueryClient()
  queryClient.setQueryData(['razas'], [{ id: 'raza-nelore', nombre: 'Nelore' }])

  render(createElement(QueryClientProvider, { client: queryClient }, createElement(DiagnosticosPanel, {
    diagnosticos: {
      content: [{
        id: 'diag-1',
        animalId: 'ani-1',
        codigoAnimal: 'ANI-000040',
        nombreAnimal: 'LVN-10',
        fechaDiagnostico: '2026-10-09T11:10:00Z',
        resultado: 'POSITIVO',
        metodo: 'PALPACION',
        fechaProbableParto: '2027-07-21',
        estado: 'ACTIVO',
        version: 1,
      }],
      page: 0,
      size: 20,
      totalPages: 1,
      totalElements: 1,
    },
    servicios: [],
    isLoading: false,
    error: null,
    catalogs,
    refresh: vi.fn(),
  })))

  expect(screen.getAllByText('LVN-10')).toHaveLength(2)
  expect(screen.getAllByText('Nelore')).toHaveLength(2)
  expect(screen.queryByText('ANI-000040')).not.toBeInTheDocument()
})

