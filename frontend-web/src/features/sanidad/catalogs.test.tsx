import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/shared/api/http'
import { useReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { useSanidadCatalogs } from './catalogs'

vi.mock('@/shared/api/http', () => ({ http: { get: vi.fn() } }))

afterEach(() => vi.clearAllMocks())

describe.each([
  ['Sanidad', useSanidadCatalogs],
  ['Reproducción', useReproduccionCatalogs],
] as const)('Catálogos de %s', (_name, useCatalogs) => {
  it('carga los potreros desde la respuesta paginada sin exigir filtros del usuario', async () => {
    const paddock = { id: 'pot-1', propiedadId: 'prop-1', codigo: 'P1', nombre: 'Potrero norte', activo: true }
    vi.mocked(http.get).mockImplementation(async (url) => ({
      data: { data: url === '/api/v1/potreros'
        ? { content: [paddock] }
        : url === '/api/v1/propiedades' || url === '/api/v1/categorias-animal'
          ? []
          : { content: [] } },
    }))
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
    const { result } = renderHook(() => useCatalogs(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.paddocks).toEqual([paddock])
    expect(http.get).toHaveBeenCalledWith('/api/v1/potreros', { params: { page: 0, size: 500 } })
    client.clear()
  })
})
