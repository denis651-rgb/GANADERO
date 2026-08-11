import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Button } from './Button'

describe('Button accessibility', () => {
  it('comunica loading, conserva un nombre accesible y bloquea doble interacción', () => {
    const onClick = vi.fn()
    render(<Button loading loadingLabel="Guardando animal…" onClick={onClick}>Guardar animal</Button>)
    const button = screen.getByRole('button', { name: 'Guardando animal…' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'true')
    expect(button.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })
})
