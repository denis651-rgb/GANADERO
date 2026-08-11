import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Header } from './Header'

vi.mock('@/shared/hooks/useOnlineStatus', () => ({ useOnlineStatus: () => true }))
vi.mock('@/offline/components/PendingOperationsBadge', () => ({ PendingOperationsBadge: () => <span>0 pendientes</span> }))

describe('Header global', () => {
  it('muestra estado global sin repetir el título de la página', () => {
    render(<Header />)
    expect(screen.getByLabelText('GANADERO')).toBeInTheDocument()
    expect(screen.getByText('En línea')).toBeInTheDocument()
    expect(screen.queryByText('Potreros')).not.toBeInTheDocument()
    expect(screen.queryByText('Panel principal')).not.toBeInTheDocument()
  })
})
