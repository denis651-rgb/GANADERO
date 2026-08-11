import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Field } from './Field'

describe('Field accessibility', () => {
  it('asocia label, hint y error con el control mediante IDs estables', () => {
    const { rerender } = render(<Field label="Código" hint="Usa el arete" error="El código es obligatorio"><input name="codigo" /></Field>)
    const input = screen.getByRole('textbox', { name: 'Código' })
    const firstId = input.id
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.getAttribute('aria-describedby')).toContain(`${firstId}-hint`)
    expect(input.getAttribute('aria-describedby')).toContain(`${firstId}-error`)
    expect(screen.getByText('Usa el arete')).toHaveAttribute('id', `${firstId}-hint`)
    expect(screen.getByText('El código es obligatorio')).toHaveAttribute('id', `${firstId}-error`)

    rerender(<Field label="Código" hint="Usa el arete" error="El código es obligatorio"><input name="codigo" /></Field>)
    expect(screen.getByRole('textbox', { name: 'Código' })).toHaveAttribute('id', firstId)
  })
})
