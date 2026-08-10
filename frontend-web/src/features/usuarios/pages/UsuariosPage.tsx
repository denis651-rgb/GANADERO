import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MailPlus, Power } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { listUsuarios, setUsuarioEstado, type Miembro } from '@/features/usuarios/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Modal } from '@/shared/components/Modal'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function UsuariosPage() {
  const client = useQueryClient()
  const navigate = useNavigate()
  const { can } = useAuth()
  const [blockTarget, setBlockTarget] = useState<Miembro | null>(null)
  const query = useQuery({ queryKey: ['usuarios-completos'], queryFn: listUsuarios })
  const state = useMutation({
    mutationFn: ({ id, action, version }: { id: string; action: 'activar' | 'bloquear'; version: number }) =>
      setUsuarioEstado(id, action, version),
    onSuccess: () => client.invalidateQueries({ queryKey: ['usuarios-completos'] }),
  })
  const error = query.error ?? state.error

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Seguridad"
        title="Usuarios"
        description="Administra el acceso de los colaboradores de tu empresa."
        actions={can('USUARIO_CREAR') && (
          <Button onClick={() => navigate('/invitaciones')}>
            <MailPlus size={18} />Nueva invitación
          </Button>
        )}
      />
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

      <Card>
        {query.isPending && <TableSkeleton rows={6} columns={7} />}
        {query.data?.length === 0 && (
          <EmptyState title="No hay miembros" description="Invita al primer usuario de la empresa." />
        )}
        {query.data && query.data.length > 0 && (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Cargo</th>
                  <th>Roles</th>
                  <th>Acceso</th>
                  <th>Último acceso</th>
                  <th>Estado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {query.data.map((user) => (
                  <tr key={user.id}>
                    <td><strong>{user.nombres} {user.apellidos}</strong></td>
                    <td>{user.cargo || '—'}</td>
                    <td>{user.roles.map((role) => role.nombre).join(', ') || 'Sin roles'}</td>
                    <td>{user.accesoTodasPropiedades ? 'Todas' : `${user.propiedadesPermitidas.length} asignadas`}</td>
                    <td>{user.ultimoAccesoAt ? new Date(user.ultimoAccesoAt).toLocaleString('es-BO') : 'Nunca'}</td>
                    <td><span className="status-badge">{user.estado}</span></td>
                    <td>
                      <Button
                        variant="ghost"
                        loading={state.isPending && state.variables?.id === user.id}
                        onClick={() => {
                          if (user.estado === 'ACTIVO') setBlockTarget(user)
                          else state.mutate({ id: user.id, action: 'activar', version: user.version })
                        }}
                      >
                        <Power size={16} />{user.estado === 'ACTIVO' ? 'Bloquear' : 'Activar'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        open={Boolean(blockTarget)}
        title="Bloquear acceso"
        onClose={() => { if (!state.isPending) setBlockTarget(null) }}
      >
        {blockTarget && (
          <div className="page-stack">
            <p className="muted">
              ¿Bloquear el acceso de <strong>{blockTarget.nombres} {blockTarget.apellidos}</strong>? Esta persona dejará de
              poder ingresar a la aplicación inmediatamente.
            </p>
            {state.error && <Alert tone="danger">{normalizeApiError(state.error).message}</Alert>}
            <div className="form-actions">
              <Button variant="ghost" onClick={() => setBlockTarget(null)} disabled={state.isPending}>Cancelar</Button>
              <Button variant="danger" loading={state.isPending} onClick={() => state.mutate({ id: blockTarget.id, action: 'bloquear', version: blockTarget.version })}>
                <Power size={16} />Bloquear acceso
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
