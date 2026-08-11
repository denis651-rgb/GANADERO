import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { listPropiedades } from '@/features/propiedades/api'
import { listRoles } from '@/features/roles/api'
import { assignPropiedades, assignRoles, getUsuario, updateUsuario, type Miembro } from '@/features/usuarios/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'

interface UsuarioDetailModalProps {
  userId: string | null
  onClose: () => void
  onSaved: (usuario: Miembro) => void
}

function sameIds(current: string[], initial: string[]) {
  if (current.length !== initial.length) return false
  const expected = new Set(initial)
  return current.every((id) => expected.has(id))
}

export function UsuarioDetailModal({ userId, onClose, onSaved }: UsuarioDetailModalProps) {
  const { can } = useAuth()
  const canEdit = can('USUARIO_EDITAR')
  const canAssignRoles = can('USUARIO_ASIGNAR_ROL')
  const [roleSelection, setRoleSelection] = useState<{ userId: string; ids: string[] } | null>(null)
  const [propertySelection, setPropertySelection] = useState<{ userId: string; ids: string[] } | null>(null)
  const [allPropertiesSelection, setAllPropertiesSelection] = useState<{ userId: string; value: boolean } | null>(null)

  const detail = useQuery({
    queryKey: ['usuario', userId],
    queryFn: () => getUsuario(userId!),
    enabled: Boolean(userId),
  })
  const catalogs = useQuery({
    queryKey: ['usuario-edicion-catalogos'],
    queryFn: async () => {
      const [roles, properties] = await Promise.all([
        canAssignRoles ? listRoles() : Promise.resolve([]),
        canEdit ? listPropiedades() : Promise.resolve([]),
      ])
      return { roles: roles.filter((role) => role.activo), properties: properties.filter((property) => property.activo) }
    },
    enabled: Boolean(userId) && (canEdit || canAssignRoles),
  })

  const selectedRoles = roleSelection?.userId === userId ? roleSelection.ids : (detail.data?.roles.map((role) => role.id) ?? [])
  const selectedProperties = propertySelection?.userId === userId ? propertySelection.ids : (detail.data?.propiedadesPermitidas ?? [])
  const allProperties = allPropertiesSelection?.userId === userId ? allPropertiesSelection.value : (detail.data?.accesoTodasPropiedades ?? false)

  const save = useMutation({
    mutationFn: async (form: HTMLFormElement) => {
      const current = detail.data!
      const data = new FormData(form)
      let updated = current

      if (canEdit) {
        updated = await updateUsuario(current.id, {
          nombres: String(data.get('nombres') ?? '').trim(),
          apellidos: String(data.get('apellidos') ?? '').trim(),
          telefono: String(data.get('telefono') ?? '').trim() || undefined,
          cargo: String(data.get('cargo') ?? '').trim() || undefined,
          accesoTodasPropiedades: allProperties,
          perfilVersion: updated.perfilVersion,
          version: updated.version,
        })
      }
      const initialRoleIds = current.roles.map((role) => role.id)
      if (canAssignRoles && !sameIds(selectedRoles, initialRoleIds)) {
        updated = await assignRoles(current.id, selectedRoles, updated.version)
      }
      if (canEdit && !allProperties && (!sameIds(selectedProperties, current.propiedadesPermitidas) || current.accesoTodasPropiedades)) {
        updated = await assignPropiedades(current.id, selectedProperties, updated.version)
      }
      return updated
    },
    onSuccess: (usuario) => onSaved(usuario),
  })

  function toggle(kind: 'roles' | 'properties', values: string[], id: string, checked: boolean) {
    if (!userId) return
    const ids = checked ? [...values, id] : values.filter((value) => value !== id)
    if (kind === 'roles') setRoleSelection({ userId, ids })
    else setPropertySelection({ userId, ids })
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (canAssignRoles && selectedRoles.length === 0) return
    save.mutate(event.currentTarget)
  }

  const user = detail.data
  const error = detail.error ?? catalogs.error ?? save.error
  const readonly = !canEdit && !canAssignRoles

  return (
    <Modal open={Boolean(userId)} wide title={user ? `${user.nombres} ${user.apellidos}` : 'Detalle del usuario'} description="Consulta y administra los datos, roles y acceso a propiedades del usuario." onClose={onClose}>
      {detail.isPending && <LoadingState message="Consultando usuario…" />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {user && (
        <form className="page-stack" onSubmit={submit}>
          <div className="usuario-summary">
            <span className="status-badge">{user.estado}</span>
            <span>Último acceso: {user.ultimoAccesoAt ? new Date(user.ultimoAccesoAt).toLocaleString('es-BO') : 'Nunca'}</span>
            {user.fechaIngreso && <span>Ingreso: {new Date(user.fechaIngreso).toLocaleDateString('es-BO')}</span>}
          </div>
          <div className="form-grid">
            <Field label="Nombres" required disabled={!canEdit}><input name="nombres" defaultValue={user.nombres} required disabled={!canEdit} autoComplete="given-name" /></Field>
            <Field label="Apellidos" required disabled={!canEdit}><input name="apellidos" defaultValue={user.apellidos} required disabled={!canEdit} autoComplete="family-name" /></Field>
            <Field label="Teléfono" disabled={!canEdit}><input name="telefono" type="tel" defaultValue={user.telefono ?? ''} maxLength={40} disabled={!canEdit} autoComplete="tel" /></Field>
            <Field label="Cargo" disabled={!canEdit}><input name="cargo" defaultValue={user.cargo ?? ''} maxLength={120} disabled={!canEdit} /></Field>
          </div>
          <fieldset className="usuario-access-section" disabled={!canEdit}>
            <legend>Acceso a propiedades</legend>
            <label className="checkbox-line"><input type="checkbox" checked={allProperties} onChange={(event) => { if (userId) setAllPropertiesSelection({ userId, value: event.target.checked }) }} />Puede acceder a todas las propiedades</label>
            {!allProperties && <div className="usuario-choice-grid">
              {catalogs.data?.properties.map((property) => <label key={property.id} className="checkbox-line"><input type="checkbox" checked={selectedProperties.includes(property.id)} onChange={(event) => toggle('properties', selectedProperties, property.id, event.target.checked)} /><span><strong>{property.nombre}</strong><small>{property.codigo}</small></span></label>)}
              {catalogs.data?.properties.length === 0 && <p className="muted">No hay propiedades disponibles para asignar.</p>}
            </div>}
          </fieldset>
          <fieldset className="usuario-access-section" disabled={!canAssignRoles}>
            <legend>Roles</legend>
            <p className="muted">Cada usuario debe conservar al menos un rol.</p>
            <div className="usuario-choice-grid">
              {catalogs.data?.roles.map((role) => <label key={role.id} className="checkbox-line"><input type="checkbox" checked={selectedRoles.includes(role.id)} onChange={(event) => toggle('roles', selectedRoles, role.id, event.target.checked)} /><span><strong>{role.nombre}</strong><small>{role.codigo}</small></span></label>)}
              {!canAssignRoles && user.roles.map((role) => <span key={role.id} className="status-badge">{role.nombre}</span>)}
            </div>
            {canAssignRoles && selectedRoles.length === 0 && <span className="field-error" role="alert">Selecciona al menos un rol.</span>}
          </fieldset>
          <div className="form-actions"><Button type="button" variant="secondary" onClick={onClose}>Cerrar</Button>{!readonly && <Button type="submit" loading={save.isPending} disabled={canAssignRoles && selectedRoles.length === 0}><Save size={17} aria-hidden="true" />Guardar cambios</Button>}</div>
        </form>
      )}
    </Modal>
  )
}
