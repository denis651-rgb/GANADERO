import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AppError } from '@/shared/api/errors'
import { PropiedadFormModal } from './PropiedadFormModal'

describe('PropiedadFormModal', () => {
  it('no renderiza nada cuando está cerrado', () => {
    render(<PropiedadFormModal open={false} loading={false} error={null} onClose={vi.fn()} onSubmit={vi.fn()} />)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('envía el formulario con los datos ingresados', () => {
    const onSubmit = vi.fn()
    render(<PropiedadFormModal open loading={false} error={null} onClose={vi.fn()} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText(/^Nombre/), { target: { value: 'La Esperanza' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear propiedad' }))

    expect(onSubmit).toHaveBeenCalledOnce()
    expect(onSubmit.mock.calls[0][0]).toBeInstanceOf(HTMLFormElement)
  })

  it('muestra el error del backend y deshabilita el botón mientras guarda', () => {
    render(<PropiedadFormModal open loading error={new AppError('No se pudo crear')} onClose={vi.fn()} onSubmit={vi.fn()} />)

    expect(screen.getByText('No se pudo crear')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Crear propiedad/ })).toBeDisabled()
  })

  it('cierra al hacer clic en Cancelar', () => {
    const onClose = vi.fn()
    render(<PropiedadFormModal open loading={false} error={null} onClose={onClose} onSubmit={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
