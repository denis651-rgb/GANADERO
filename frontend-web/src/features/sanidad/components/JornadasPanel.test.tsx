import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { JornadaSanitaria } from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { JornadasPanel } from './JornadasPanel'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true, user: { id: 'u-1' } }) }))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return { ...actual, crearJornada: vi.fn() }
})

function renderPanel(tipoJornadaSugerida?: 'PRUEBA_DIAGNOSTICA', jornadas: JornadaSanitaria[] = []) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const catalogs = {
    properties: [{ id: 'prop-1', codigo: 'FIN-1', nombre: 'Finca Norte', activo: true, version: 0 }],
    paddocks: [], categories: [], lots: [], animals: [], animalLabel: () => 'Animal',
  } as SanidadCatalogs
  render(
    <QueryClientProvider client={client}>
      <JornadasPanel jornadas={jornadas} isLoading={false} error={null} refresh={vi.fn()} catalogs={catalogs} tipoJornadaSugerida={tipoJornadaSugerida} />
    </QueryClientProvider>,
  )
}

describe('JornadasPanel — atajo desde "Registrar prueba diagnóstica"', () => {
  it('sin tipoJornadaSugerida, el formulario de nueva jornada arranca cerrado', () => {
    renderPanel()
    expect(screen.queryByRole('dialog', { name: 'Nueva jornada sanitaria' })).not.toBeInTheDocument()
  })

  it('con tipoJornadaSugerida=PRUEBA_DIAGNOSTICA, abre el formulario y preselecciona el tipo', () => {
    renderPanel('PRUEBA_DIAGNOSTICA')
    expect(screen.getByRole('dialog', { name: 'Nueva jornada sanitaria' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /^Tipo de jornada/ })).toHaveValue('PRUEBA_DIAGNOSTICA')
  })

  it('solo ofrece editar, preparar y cancelar cuando la jornada está en borrador', () => {
    const base: JornadaSanitaria = { id: 'j-1', empresaId: 'e-1', tipoJornada: 'VACUNACION', fechaInicio: '2026-09-04', propiedadId: 'prop-1', responsableId: 'u-1', estado: 'BORRADOR', version: 0 }
    renderPanel(undefined, [base, { ...base, id: 'j-2', estado: 'CONFIRMADA' }])

    expect(screen.getAllByRole('button', { name: /Editar/ })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: /Preparar y confirmar/ })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: /Cancelar/ })).toHaveLength(2)
    expect(screen.getAllByText('Sin acciones')).toHaveLength(2)
  })
})
