import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Power } from 'lucide-react'
import { listRoles } from '@/features/roles/api'
import { createUsuario, listUsuarios, setUsuarioEstado } from '@/features/usuarios/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function UsuariosPage() {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const query = useQuery({ queryKey: ['usuarios-completos'], queryFn: async () => { const [users, roles] = await Promise.all([listUsuarios(), listRoles()]); return { users, roles } } })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createUsuario({ usuarioId: String(data.get('usuarioId')), nombres: String(data.get('nombres')), apellidos: String(data.get('apellidos')), telefono: String(data.get('telefono') ?? ''), cargo: String(data.get('cargo') ?? ''), accesoTodasPropiedades: data.get('accesoTodasPropiedades') === 'on', roles: data.getAll('roles').map(String) })
    },
    onSuccess: () => { setShowForm(false); client.invalidateQueries({ queryKey: ['usuarios-completos'] }) },
  })
  const state = useMutation({
    mutationFn: ({ id, action, version }: { id: string; action: 'activar' | 'bloquear'; version: number }) => setUsuarioEstado(id, action, version),
    onSuccess: () => client.invalidateQueries({ queryKey: ['usuarios-completos'] }),
  })
  const error = query.error ?? create.error ?? state.error

  return <div className="page-stack"><PageHeader eyebrow="Seguridad" title="Usuarios" description="Administra membresías, roles y acceso a la empresa." actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} />Nuevo miembro</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {showForm && <Card><Alert>El UUID debe corresponder a un usuario existente en Supabase Auth.</Alert><form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="UUID de Supabase"><input name="usuarioId" required pattern="[0-9a-fA-F-]{36}" /></Field><Field label="Nombres"><input name="nombres" required /></Field><Field label="Apellidos"><input name="apellidos" required /></Field><Field label="Teléfono"><input name="telefono" /></Field><Field label="Cargo"><input name="cargo" /></Field>
      <fieldset><legend>Roles</legend>{query.data?.roles.filter((role) => role.activo).map((role) => <label key={role.id}><input type="checkbox" name="roles" value={role.id} /> {role.nombre}</label>)}</fieldset>
      <label className="checkbox-line"><input name="accesoTodasPropiedades" type="checkbox" defaultChecked /> Acceso a todas las propiedades</label><div className="form-actions"><Button type="submit" loading={create.isPending}>Crear miembro</Button></div>
    </form></Card>}
    <Card>{query.isPending && <LoadingState message="Consultando usuarios…" />}{query.data?.users.length === 0 && <EmptyState title="No hay miembros" description="Agrega el primer usuario de la empresa." />}{query.data && query.data.users.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Usuario</th><th>Cargo</th><th>Roles</th><th>Acceso</th><th>Estado</th><th></th></tr></thead><tbody>{query.data.users.map((user) => <tr key={user.id}><td><strong>{user.nombres} {user.apellidos}</strong><span className="table-secondary">{user.usuarioId}</span></td><td>{user.cargo || '—'}</td><td>{user.roles.map((role) => role.nombre).join(', ') || 'Sin roles'}</td><td>{user.accesoTodasPropiedades ? 'Todas las propiedades' : `${user.propiedadesPermitidas.length} asignadas`}</td><td><span className="status-badge">{user.estado}</span></td><td><Button variant="ghost" loading={state.isPending} onClick={() => state.mutate({ id: user.id, action: user.estado === 'ACTIVO' ? 'bloquear' : 'activar', version: user.version })}><Power size={16} />{user.estado === 'ACTIVO' ? 'Bloquear' : 'Activar'}</Button></td></tr>)}</tbody></table></div>}</Card>
  </div>
}
