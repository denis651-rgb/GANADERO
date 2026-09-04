import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { JornadasPanel } from './JornadasPanel'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true, user: { id: 'u-1' } }) }))
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return { ...actual, crearJornada: vi.fn() }
})

function renderPanel(tipoJornadaSugerida?: 'PRUEBA_DIAGNOSTICA') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <JornadasPanel jornadas={[]} isLoading={false} error={null} refresh={vi.fn()} tipoJornadaSugerida={tipoJornadaSugerida} />
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
})
