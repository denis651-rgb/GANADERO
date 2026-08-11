import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { UsuarioCreateModal } from './UsuarioCreateModal'

const createUsuario = vi.fn()
vi.mock('@/features/usuarios/api', () => ({ createUsuario: (...args: unknown[]) => createUsuario(...args) }))
vi.mock('@/features/roles/api', () => ({ listRoles: vi.fn().mockResolvedValue([{ id: 'r-1', codigo: 'CAMPO', nombre: 'Campo', activo: true }]) }))
vi.mock('@/features/propiedades/api', () => ({ listPropiedades: vi.fn().mockResolvedValue([{ id: 'p-1', codigo: 'PR-1', nombre: 'La Esperanza', activo: true }]) }))

describe('UsuarioCreateModal', () => {
  it('crea un usuario con su rol y alcance inicial', async () => {
    const created = { id: 'u-1', nombres: 'Luis', apellidos: 'Flores' }
    createUsuario.mockReset().mockResolvedValue(created)
    const onCreated = vi.fn()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    render(<QueryClientProvider client={client}><UsuarioCreateModal open onClose={vi.fn()} onCreated={onCreated} /></QueryClientProvider>)

    fireEvent.change(await screen.findByRole('textbox', { name: /Correo/ }), { target: { value: 'luis@example.com' } })
    fireEvent.change(screen.getByRole('textbox', { name: /Nombres/ }), { target: { value: 'Luis' } })
    fireEvent.change(screen.getByRole('textbox', { name: /Apellidos/ }), { target: { value: 'Flores' } })
    fireEvent.click(screen.getByLabelText(/Campo/))
    fireEvent.click(screen.getByRole('button', { name: 'Crear y enviar invitación' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalled())
    expect(onCreated.mock.calls[0][0]).toEqual(created)
    expect(createUsuario).toHaveBeenCalledWith(expect.objectContaining({ email: 'luis@example.com', roles: ['r-1'], accesoTodasPropiedades: true, propiedades: [] }))
  })
})
