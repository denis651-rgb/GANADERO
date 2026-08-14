import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Power } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { cambiarEstadoEnfermedad, crearEnfermedad, listEnfermedades, type Enfermedad } from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

export function EnfermedadesPanel() {
  const client = useQueryClient()
  const { can } = useAuth()
  const canAdmin = can('SANIDAD_PLAN_ADMINISTRAR')
  const [showForm, setShowForm] = useState(false)
  const [target, setTarget] = useState<Enfermedad | null>(null)

  const query = useQuery({ queryKey: ['sanidad-enfermedades'], queryFn: () => listEnfermedades(true) })
  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return crearEnfermedad({ codigo: String(data.get('codigo')), nombre: String(data.get('nombre')), descripcion: String(data.get('descripcion') || '') || undefined, esNotificable: data.get('esNotificable') === 'on' })
    },
    onSuccess: () => { setShowForm(false); void client.invalidateQueries({ queryKey: ['sanidad-enfermedades'] }) },
  })
  const cambiar = useMutation({
    mutationFn: (enfermedad: Enfermedad) => cambiarEstadoEnfermedad(enfermedad.id, !enfermedad.activo),
    onSuccess: () => { setTarget(null); void client.invalidateQueries({ queryKey: ['sanidad-enfermedades'] }) },
  })
  const error = query.error ?? crear.error ?? cambiar.error

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Enfermedades</h2>
        {canAdmin && <Button onClick={() => setShowForm(true)}><Plus size={18} aria-hidden="true" />Nueva enfermedad</Button>}
      </div>
      {query.isPending && <LoadingState message="Cargando enfermedades…" />}
      {!query.isPending && query.data?.length === 0 && <EmptyState title="No hay enfermedades" description="Registra el catálogo de enfermedades que usan los casos clínicos." />}
      {query.data && query.data.length > 0 && <div className="table-wrapper"><table><caption className="visually-hidden">Enfermedades registradas</caption><thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Descripción</th><th scope="col">Notificable</th><th scope="col">Estado</th>{canAdmin && <th scope="col">Acciones</th>}</tr></thead><tbody>{query.data.map((enfermedad) => <tr key={enfermedad.id}>
        <td><strong>{enfermedad.codigo}</strong></td><td>{enfermedad.nombre}</td><td className="table-secondary">{enfermedad.descripcion ?? '—'}</td><td>{enfermedad.esNotificable ? 'Sí' : 'No'}</td><td><span className="status-badge">{enfermedad.activo ? 'ACTIVO' : 'INACTIVO'}</span></td>
        {canAdmin && <td><Button variant="ghost" onClick={() => setTarget(enfermedad)}><Power size={16} aria-hidden="true" />{enfermedad.activo ? 'Desactivar' : 'Activar'}</Button></td>}
      </tr>)}</tbody></table></div>}
    </Card>

    <Modal open={showForm} title="Nueva enfermedad" onClose={() => setShowForm(false)} description="Registra una enfermedad del catálogo sanitario.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Código" required><input name="codigo" required maxLength={60} placeholder="ENF-001…" autoComplete="off" spellCheck={false} /></Field>
        <Field label="Nombre" required><input name="nombre" required maxLength={160} autoComplete="off" /></Field>
        <div className="form-full"><Field label="Descripción"><textarea name="descripcion" rows={3} maxLength={2000} placeholder="Signos, transmisión…" /></Field></div>
        <label className="checkbox-line"><input name="esNotificable" type="checkbox" /> Es notificable a sanidad</label>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Crear enfermedad</Button></div>
      </form>
    </Modal>

    <ConfirmDialog
      open={Boolean(target)}
      title={target?.activo ? 'Desactivar enfermedad' : 'Activar enfermedad'}
      confirmLabel={target?.activo ? 'Desactivar' : 'Activar'}
      variant={target?.activo ? 'danger' : 'warning'}
      loading={cambiar.isPending}
      error={cambiar.error}
      onClose={() => setTarget(null)}
      onConfirm={() => { if (target && !cambiar.isPending) cambiar.mutate(target) }}
    >
      {target && <p className="muted">«{target.nombre}» quedará {target.activo ? 'inactiva' : 'activa'} en el catálogo.</p>}
    </ConfirmDialog>
  </div>
}
