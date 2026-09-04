import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ControlEctoparasitarioModal } from './ControlEctoparasitarioModal'

const getPrincipiosActivosRecientes = vi.fn()
const crearControlEctoparasitario = vi.fn()
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    getPrincipiosActivosRecientes: (...args: unknown[]) => getPrincipiosActivosRecientes(...args),
    crearControlEctoparasitario: (...args: unknown[]) => crearControlEctoparasitario(...args),
  }
})

function renderModal(props: Partial<{ animalId: string; loteGanaderoId: string }> = { loteGanaderoId: 'lote-1' }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ControlEctoparasitarioModal
        animalId={props.animalId}
        loteGanaderoId={props.loteGanaderoId}
        destinoLabel="Recría 2026"
        onClose={vi.fn()}
        onSaved={vi.fn()}
      />
    </QueryClientProvider>,
  )
}

describe('ControlEctoparasitarioModal — advertencia de rotación', () => {
  it('muestra la advertencia cuando el principio activo coincide con uno de los últimos usados', async () => {
    getPrincipiosActivosRecientes.mockReset().mockResolvedValue(['Cipermetrina', 'Cipermetrina', 'Cipermetrina'])
    renderModal()

    await screen.findByLabelText(/Principio activo/)
    expect(screen.queryByText(/considerá rotar el principio activo/)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Principio activo/), { target: { value: 'Cipermetrina' } })

    expect(await screen.findByText(/considerá rotar el principio activo/)).toBeInTheDocument()
    expect(screen.getByText(/Cipermetrina, Cipermetrina, Cipermetrina/)).toBeInTheDocument()
  })

  it('no muestra la advertencia cuando el principio activo es distinto a los recientes', async () => {
    getPrincipiosActivosRecientes.mockReset().mockResolvedValue(['Cipermetrina', 'Cipermetrina', 'Cipermetrina'])
    renderModal()

    await screen.findByLabelText(/Principio activo/)
    fireEvent.change(screen.getByLabelText(/Principio activo/), { target: { value: 'Ivermectina' } })

    expect(screen.queryByText(/considerá rotar el principio activo/)).not.toBeInTheDocument()
  })
})
