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
