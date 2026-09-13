import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { ManualSidebar } from './ManualSidebar'

describe('ManualSidebar', () => {
  it('resalta el capítulo activo en el índice de escritorio', () => {
    render(<MemoryRouter><ManualSidebar activeChapterId="compras" /></MemoryRouter>)
    const nav = screen.getByRole('navigation', { name: 'Capítulos del manual' })
    expect(within(nav).getByRole('link', { name: 'Compras' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Lotes' })).not.toHaveAttribute('aria-current')
  })

  it('el botón de índice móvil abre el listado en un cajón y navegar lo cierra', () => {
    render(<MemoryRouter><ManualSidebar activeChapterId="compras" /></MemoryRouter>)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Índice del manual' }))

    const dialog = screen.getByRole('dialog', { name: 'Índice del manual' })
    const lotesLink = within(dialog).getByRole('link', { name: 'Lotes' })
    expect(lotesLink).toBeInTheDocument()

    fireEvent.click(lotesLink)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
