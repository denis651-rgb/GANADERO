import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MailPlus, Power } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { listUsuarios, setUsuarioEstado } from '@/features/usuarios/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function UsuariosPage() {
  const client = useQueryClient()
  const navigate = useNavigate()
  const { can } = useAuth()
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
        {query.isPending && <LoadingState message="Consultando usuarios…" />}
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
                        loading={state.isPending}
                        onClick={() => state.mutate({
                          id: user.id,
                          action: user.estado === 'ACTIVO' ? 'bloquear' : 'activar',
                          version: user.version,
                        })}
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
    </div>
  )
}
