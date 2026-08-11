import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { MobileNav } from './MobileNav'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))

describe('MobileNav drawer accessibility', () => {
  it('mueve el foco al diálogo, cierra con Escape y lo devuelve al botón Más', async () => {
    render(<MemoryRouter><MobileNav /></MemoryRouter>)
    const more = screen.getByRole('button', { name: 'Más' })
    more.focus()
    fireEvent.click(more)

    const dialog = await screen.findByRole('dialog', { name: 'Todos los módulos' })
    await waitFor(() => expect(dialog).toHaveFocus())
    fireEvent.keyDown(document, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Todos los módulos' })).not.toBeInTheDocument())
    expect(more).toHaveFocus()
  })
})
