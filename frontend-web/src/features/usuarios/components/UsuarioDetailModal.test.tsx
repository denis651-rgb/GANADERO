import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsuarioDetailModal } from './UsuarioDetailModal'

const getUsuario = vi.fn()
const updateUsuario = vi.fn()
const assignRoles = vi.fn()
const assignPropiedades = vi.fn()

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ can: () => true }) }))
vi.mock('@/features/usuarios/api', () => ({
  getUsuario: (...args: unknown[]) => getUsuario(...args),
  updateUsuario: (...args: unknown[]) => updateUsuario(...args),
  assignRoles: (...args: unknown[]) => assignRoles(...args),
  assignPropiedades: (...args: unknown[]) => assignPropiedades(...args),
}))
vi.mock('@/features/roles/api', () => ({
  listRoles: vi.fn().mockResolvedValue([
    { id: 'r-1', codigo: 'ADMIN', nombre: 'Administrador', activo: true },
    { id: 'r-2', codigo: 'CAMPO', nombre: 'Trabajador de campo', activo: true },
  ]),
}))
vi.mock('@/features/propiedades/api', () => ({
  listPropiedades: vi.fn().mockResolvedValue([
    { id: 'p-1', codigo: 'PR-1', nombre: 'La Esperanza', activo: true },
    { id: 'p-2', codigo: 'PR-2', nombre: 'El Prado', activo: true },
  ]),
}))

const usuario = {
  id: 'u-1', usuarioId: 'auth-1', nombres: 'Ana', apellidos: 'Rojas', telefono: '70000000', cargo: 'Encargada',
  estado: 'ACTIVO' as const, accesoTodasPropiedades: false, perfilVersion: 2, version: 3,
  roles: [{ id: 'r-1', codigo: 'ADMIN', nombre: 'Administrador' }], propiedadesPermitidas: ['p-1'],
}

describe('UsuarioDetailModal', () => {
  beforeEach(() => {
    getUsuario.mockReset().mockResolvedValue(usuario)
    updateUsuario.mockReset().mockResolvedValue({ ...usuario, cargo: 'Supervisora', version: 4, perfilVersion: 3 })
    assignRoles.mockReset().mockResolvedValue({ ...usuario, version: 5 })
    assignPropiedades.mockReset().mockResolvedValue({ ...usuario, version: 6 })
  })

  it('consulta el detalle y encadena edición, roles y propiedades con la versión vigente', async () => {
    const onSaved = vi.fn()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    render(<QueryClientProvider client={client}><UsuarioDetailModal userId="u-1" onClose={vi.fn()} onSaved={onSaved} /></QueryClientProvider>)

    expect(await screen.findByRole('dialog', { name: 'Ana Rojas' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Cargo'), { target: { value: 'Supervisora' } })
    fireEvent.click(screen.getByLabelText(/Trabajador de campo/))
    fireEvent.click(screen.getByLabelText(/El Prado/))
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    expect(updateUsuario).toHaveBeenCalledWith('u-1', expect.objectContaining({ cargo: 'Supervisora', version: 3, perfilVersion: 2 }))
    expect(assignRoles).toHaveBeenCalledWith('u-1', ['r-1', 'r-2'], 4)
    expect(assignPropiedades).toHaveBeenCalledWith('u-1', ['p-1', 'p-2'], 5)
  })
})
