import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Modal } from './Modal'

describe('Modal accessibility', () => {
  it('asocia título y descripción, declara modalidad y ofrece cierre accesible', () => {
    const onClose = vi.fn()
    render(<Modal open title="Editar animal" description="Formulario de edición" onClose={onClose}>Contenido</Modal>)
    const dialog = screen.getByRole('dialog', { name: 'Editar animal' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toHaveAccessibleDescription('Formulario de edición')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledOnce()
  })
})
