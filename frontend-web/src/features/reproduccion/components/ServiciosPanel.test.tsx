import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { ServiciosPanel } from './ServiciosPanel'
import type { CeloResponse } from '../api'
import type { ReproduccionCatalogs } from '../catalogs'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('./AnimalSearchSelect', () => ({ AnimalSearchSelect: ({ onChange }: { onChange: (id: string) => void }) => <button type="button" onClick={() => onChange('hembra')}>Elegir hembra</button> }))

it('excluye anulados y otras hembras y limpia una selección recién anulada', () => {
 const celos = [
  { id: 'activo', animalId: 'hembra', estado: 'ACTIVO', fechaDeteccion: '2026-09-04T13:11:00Z' },
  { id: 'anulado', animalId: 'hembra', estado: 'ANULADO', fechaDeteccion: '2026-09-04T13:29:00Z' },
  { id: 'otra', animalId: 'otra-hembra', estado: 'ACTIVO', fechaDeteccion: '2026-09-04T13:30:00Z' },
 ] as CeloResponse[]
 const client = new QueryClient()
 const catalogs = { hembras: [], properties: [], paddocks: [], lots: [] } as unknown as ReproduccionCatalogs
 const view = (items: CeloResponse[]) => <QueryClientProvider client={client}><ServiciosPanel celos={items} servicios={{ content: [], totalElements: 0, totalPages: 0, page: 0, size: 50 }} isLoading={false} error={null} catalogs={catalogs} refresh={vi.fn()} /></QueryClientProvider>
 const { rerender } = render(view(celos))
 fireEvent.click(screen.getByRole('button', { name: 'Registrar servicio' }))
 expect(screen.getByLabelText('Celo asociado')).toBeDisabled()
 fireEvent.click(screen.getByRole('button', { name: 'Elegir hembra' }))
 const select = screen.getByLabelText('Celo asociado')
 expect(within(select).getAllByRole('option')).toHaveLength(2)
 fireEvent.change(select, { target: { value: 'activo' } })
 expect(select).toHaveValue('activo')
 rerender(view(celos.map((celo) => ({ ...celo, estado: 'ANULADO' }))))
 expect(screen.getByLabelText('Celo asociado')).toHaveValue('')
 expect(within(screen.getByLabelText('Celo asociado')).getAllByRole('option')).toHaveLength(1)
})
