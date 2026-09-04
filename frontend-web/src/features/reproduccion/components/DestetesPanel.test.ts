import { createElement } from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { DestetesPanel } from './DestetesPanel'
import { getMadreDestete } from '../api'
import type { ReproduccionCatalogs } from '../catalogs'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('../api', async (original) => ({ ...await original<typeof import('../api')>(), getMadreDestete: vi.fn() }))
vi.mock('./AnimalSearchSelect', () => ({ AnimalSearchSelect: ({ onChange }: { onChange: (id: string) => void }) => createElement('div', null,
  createElement('button', { type: 'button', onClick: () => onChange('cria') }, 'Elegir cría'),
  createElement('button', { type: 'button', onClick: () => onChange('') }, 'Limpiar cría')) }))

it('completa la madre del parto y la limpia al quitar la cría', async () => {
  vi.mocked(getMadreDestete).mockResolvedValue({ id: 'madre', nombre: 'Lucera', codigo: 'ANI-000001' })
  render(createElement(QueryClientProvider, { client: new QueryClient() }, createElement(DestetesPanel, {
    destetes: { content: [], page: 0, size: 20, totalPages: 0, totalElements: 0 }, isLoading: false, error: null, catalogs: {} as ReproduccionCatalogs, refresh: vi.fn(),
  })))
  fireEvent.click(screen.getByRole('button', { name: 'Registrar destete' }))
  fireEvent.click(screen.getByRole('button', { name: 'Elegir cría' }))
  await waitFor(() => expect(screen.getByLabelText('Madre')).toHaveValue('Lucera · ANI-000001'))
  expect(getMadreDestete).toHaveBeenCalledWith('cria')
  expect(screen.getByLabelText('Madre')).toHaveAttribute('readonly')
  fireEvent.click(screen.getByRole('button', { name: 'Limpiar cría' }))
  expect(screen.getByLabelText('Madre')).toHaveValue('Selecciona primero la cría')
})
