import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'

describe('ConfirmDialog', () => {
  it('cancela sin confirmar y expone un cierre visible y accesible', () => {
    const onClose = vi.fn()
    const onConfirm = vi.fn()
    render(<ConfirmDialog open title="Confirmar operación" confirmLabel="Confirmar" onClose={onClose} onConfirm={onConfirm}>Detalle</ConfirmDialog>)

    expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveClass('modal-close')
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledOnce()
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('impide confirmar dos veces durante loading', () => {
    const onConfirm = vi.fn()
    render(<ConfirmDialog open title="Confirmar operación" confirmLabel="Confirmar" loading onClose={vi.fn()} onConfirm={onConfirm}>Detalle</ConfirmDialog>)

    const confirm = screen.getByRole('button', { name: 'Procesando: Confirmar…' })
    expect(confirm).toBeDisabled()
    fireEvent.click(confirm)
    fireEvent.click(confirm)
    expect(onConfirm).not.toHaveBeenCalled()
  })
})
