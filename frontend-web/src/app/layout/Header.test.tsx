import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { Header } from './Header'

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('@/shared/hooks/useOnlineStatus', () => ({ useOnlineStatus: () => true }))
vi.mock('@/offline/components/PendingOperationsBadge', () => ({ PendingOperationsBadge: () => <span>0 pendientes</span> }))
vi.mock('@/features/alertas/api', () => ({ getAlertCount: () => Promise.resolve({ total: 0 }) }))

const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })

describe('Header global', () => {
  it('muestra estado global sin repetir el título de la página', () => {
    render(<QueryClientProvider client={client}><MemoryRouter><Header /></MemoryRouter></QueryClientProvider>)
    expect(screen.getByLabelText('GANADERO')).toBeInTheDocument()
    expect(screen.getByText('En línea')).toBeInTheDocument()
    expect(screen.queryByText('Potreros')).not.toBeInTheDocument()
    expect(screen.queryByText('Panel principal')).not.toBeInTheDocument()
  })
})
