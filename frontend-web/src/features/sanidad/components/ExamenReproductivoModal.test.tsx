import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnimalSummary } from '@/features/animales/types'
import { ExamenReproductivoModal } from './ExamenReproductivoModal'

const listExamenesReproductivos = vi.fn().mockResolvedValue([])
const crearExamenReproductivo = vi.fn().mockResolvedValue({})
const getConfiguracionSanitaria = vi.fn().mockResolvedValue({
  edadMinMachoMeses: 18,
  edadMinHembraMeses: 15,
  horizonteProyeccionMeses: 12,
  version: 0,
})
vi.mock('@/features/sanidad/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/sanidad/api')>('@/features/sanidad/api')
  return {
    ...actual,
    listExamenesReproductivos: (...args: unknown[]) => listExamenesReproductivos(...args),
    crearExamenReproductivo: (...args: unknown[]) => crearExamenReproductivo(...args),
    getConfiguracionSanitaria: (...args: unknown[]) => getConfiguracionSanitaria(...args),
  }
})

function renderModal(sexo: 'MACHO' | 'HEMBRA') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const animal = { id: 'a-1', codigo: 'ANI-001', sexo } as AnimalSummary
  render(
    <QueryClientProvider client={client}>
      <ExamenReproductivoModal animal={animal} onClose={vi.fn()} onSaved={vi.fn()} />
    </QueryClientProvider>,
  )
}

describe('ExamenReproductivoModal — campos según sexo del animal', () => {
  beforeEach(() => {
    crearExamenReproductivo.mockClear()
  })

  it('para un macho muestra solo los campos de toro', async () => {
    renderModal('MACHO')

    expect(await screen.findByLabelText('Circunferencia escrotal (cm)')).toBeInTheDocument()
    expect(screen.getByLabelText(/Motilidad espermática/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Morfología/)).toBeInTheDocument()
    expect(screen.getByLabelText('Libido')).toBeInTheDocument()
    expect(screen.getByLabelText('Capacidad de servicio')).toBeInTheDocument()
    expect(screen.getByLabelText('Veterinario responsable')).toBeInTheDocument()

    expect(screen.queryByLabelText(/Peso \(kg\)/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Porcentaje de peso adulto/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Condición corporal/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Desarrollo reproductivo')).not.toBeInTheDocument()
  })

  it('para una hembra muestra solo los campos de vaquilla', async () => {
    renderModal('HEMBRA')

    expect(await screen.findByLabelText(/Peso \(kg\)/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Porcentaje de peso adulto/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Condición corporal/)).toBeInTheDocument()
    expect(screen.getByLabelText('Desarrollo reproductivo')).toBeInTheDocument()
    expect(screen.getByLabelText('Veterinario responsable')).toBeInTheDocument()

    expect(screen.queryByLabelText('Circunferencia escrotal (cm)')).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Motilidad espermática/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Morfología/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Libido')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Capacidad de servicio')).not.toBeInTheDocument()
  })

  it('envía el veterinario responsable como texto libre', async () => {
    renderModal('MACHO')

    fireEvent.change(await screen.findByLabelText(/Fecha/), { target: { value: '2026-09-10' } })
    fireEvent.change(screen.getByLabelText('Veterinario responsable'), { target: { value: 'Dra. Ana Quispe' } })
    const guardar = screen.getByRole('button', { name: 'Guardar examen' })
    await waitFor(() => expect(guardar).toBeEnabled())
    fireEvent.click(guardar)

    await waitFor(() => expect(crearExamenReproductivo).toHaveBeenCalledWith(
      expect.objectContaining({ veterinarioId: 'Dra. Ana Quispe' }),
    ))
  })
})
