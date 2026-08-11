import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppShell } from './AppShell'

vi.mock('@/app/layout/Sidebar', () => ({ Sidebar: () => <nav>Sidebar</nav> }))
vi.mock('@/app/layout/Header', () => ({ Header: () => <header>Header</header> }))
vi.mock('@/app/layout/MobileNav', () => ({ MobileNav: () => <nav>Mobile</nav> }))

describe('AppShell accessibility', () => {
  it('incluye un skip link que apunta al contenido principal enfocable', () => {
    render(<MemoryRouter><AppShell /></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Saltar al contenido principal' })).toHaveAttribute('href', '#main-content')
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content')
    expect(screen.getByRole('main')).toHaveAttribute('tabindex', '-1')
  })
})
