import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AppError } from '@/shared/api/errors'
import { SectorAddModal } from './SectorAddModal'

describe('SectorAddModal', () => {
  it('no renderiza nada cuando está cerrado', () => {
    render(<SectorAddModal open={false} loading={false} error={null} onClose={vi.fn()} onSubmit={vi.fn()} />)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('envía el formulario con los datos ingresados', () => {
    const onSubmit = vi.fn()
    render(<SectorAddModal open loading={false} error={null} onClose={vi.fn()} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText(/^Nombre/), { target: { value: 'Norte' } })
    fireEvent.click(screen.getByRole('button', { name: 'Añadir sector' }))

    expect(onSubmit).toHaveBeenCalledOnce()
    expect(onSubmit.mock.calls[0][0]).toBeInstanceOf(HTMLFormElement)
  })

  it('muestra el error del backend', () => {
    render(<SectorAddModal open loading={false} error={new AppError('No se pudo crear el sector')} onClose={vi.fn()} onSubmit={vi.fn()} />)
    expect(screen.getByText('No se pudo crear el sector')).toBeInTheDocument()
  })

  it('cierra al hacer clic en Cancelar', () => {
    const onClose = vi.fn()
    render(<SectorAddModal open loading={false} error={null} onClose={onClose} onSubmit={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
