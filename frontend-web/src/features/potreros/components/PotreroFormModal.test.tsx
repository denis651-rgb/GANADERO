import type { ComponentProps } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Propiedad } from '@/features/propiedades/api'
import { AppError } from '@/shared/api/errors'
import { PotreroFormModal } from './PotreroFormModal'

vi.mock('@/features/propiedades/api', () => ({ listSectores: vi.fn().mockResolvedValue([{ id: 's-1', propiedadId: 'p-1', codigo: 'S-1', nombre: 'Norte', activo: true, version: 1 }]) }))

const propiedad = { id: 'p-1', nombre: 'La Esperanza', activo: true, version: 1 } as Propiedad

function renderModal(props: Partial<ComponentProps<typeof PotreroFormModal>> = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <PotreroFormModal properties={[propiedad]} grasses={[]} loading={false} error={null} onClose={vi.fn()} onSubmit={vi.fn()} {...props} />
    </QueryClientProvider>,
  )
}

describe('PotreroFormModal', () => {
  it('no muestra "Cargando sectores" antes de elegir una propiedad', () => {
    renderModal()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.queryByText('Cargando sectores…')).not.toBeInTheDocument()
  })

  it('habilita el sector recién después de elegir una propiedad', async () => {
    renderModal()
    expect(screen.getByLabelText('Sector')).toBeDisabled()
    fireEvent.change(screen.getByLabelText(/^Propiedad/), { target: { value: 'p-1' } })
    await waitFor(() => expect(screen.getByLabelText('Sector')).not.toBeDisabled())
    expect(screen.getByRole('option', { name: 'Norte' })).toBeInTheDocument()
  })

  it('envía el formulario al confirmar', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })
    fireEvent.change(screen.getByLabelText(/^Propiedad/), { target: { value: 'p-1' } })
    fireEvent.change(screen.getByLabelText(/^Nombre/), { target: { value: 'Potrero norte' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear potrero' }))
    expect(onSubmit).toHaveBeenCalledOnce()
    expect(onSubmit.mock.calls[0][0]).toBeInstanceOf(HTMLFormElement)
  })

  it('muestra el error del backend', () => {
    renderModal({ error: new AppError('No se pudo crear el potrero') })
    expect(screen.getByText('No se pudo crear el potrero')).toBeInTheDocument()
  })
})
