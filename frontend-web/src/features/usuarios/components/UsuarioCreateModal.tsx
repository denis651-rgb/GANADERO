import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { UserPlus } from 'lucide-react'
import { listPropiedades } from '@/features/propiedades/api'
import { listRoles } from '@/features/roles/api'
import { createUsuario, type Miembro } from '@/features/usuarios/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'

interface UsuarioCreateModalProps {
  open: boolean
  onClose: () => void
  onCreated: (usuario: Miembro) => void
}

export function UsuarioCreateModal({ open, onClose, onCreated }: UsuarioCreateModalProps) {
  const [roleIds, setRoleIds] = useState<string[]>([])
  const [propertyIds, setPropertyIds] = useState<string[]>([])
  const [allProperties, setAllProperties] = useState(true)
  const catalogs = useQuery({
    queryKey: ['usuario-alta-catalogos'],
    queryFn: async () => {
      const [roles, properties] = await Promise.all([listRoles(), listPropiedades()])
      return { roles: roles.filter((role) => role.activo), properties: properties.filter((property) => property.activo) }
    },
    enabled: open,
  })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createUsuario({
        email: String(data.get('email') ?? '').trim(),
        nombres: String(data.get('nombres') ?? '').trim(),
        apellidos: String(data.get('apellidos') ?? '').trim(),
        telefono: String(data.get('telefono') ?? '').trim() || undefined,
        cargo: String(data.get('cargo') ?? '').trim() || undefined,
        accesoTodasPropiedades: allProperties,
        roles: roleIds,
        propiedades: allProperties ? [] : propertyIds,
      })
    },
    onSuccess: (usuario) => onCreated(usuario),
  })

  function toggle(values: string[], setValues: (ids: string[]) => void, id: string, checked: boolean) {
    setValues(checked ? [...values, id] : values.filter((value) => value !== id))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (roleIds.length === 0) return
    create.mutate(event.currentTarget)
  }

  return <Modal open={open} wide title="Nuevo usuario" description="Crea el acceso inicial y envía una invitación para que la persona defina su contraseña." onClose={onClose}>
    {catalogs.isPending && <LoadingState message="Cargando roles y propiedades…" />}
    {(catalogs.error || create.error) && <Alert tone="danger">{normalizeApiError(catalogs.error ?? create.error).message}</Alert>}
    {catalogs.data && <form className="page-stack" onSubmit={submit}>
      <Alert tone="info">Al guardar, se enviará una invitación al correo indicado.</Alert>
      <div className="form-grid">
        <Field label="Correo" required><input name="email" type="email" required maxLength={180} autoComplete="email" /></Field>
        <Field label="Cargo"><input name="cargo" maxLength={120} /></Field>
        <Field label="Nombres" required><input name="nombres" required maxLength={120} autoComplete="given-name" /></Field>
        <Field label="Apellidos" required><input name="apellidos" required maxLength={120} autoComplete="family-name" /></Field>
        <Field label="Teléfono"><input name="telefono" type="tel" maxLength={40} autoComplete="tel" /></Field>
      </div>
      <fieldset className="usuario-access-section">
        <legend>Roles iniciales</legend>
        <div className="usuario-choice-grid">{catalogs.data.roles.map((role) => <label key={role.id} className="checkbox-line"><input type="checkbox" checked={roleIds.includes(role.id)} onChange={(event) => toggle(roleIds, setRoleIds, role.id, event.target.checked)} /><span><strong>{role.nombre}</strong><small>{role.codigo}</small></span></label>)}</div>
        {roleIds.length === 0 && <span className="field-error">Selecciona al menos un rol.</span>}
      </fieldset>
      <fieldset className="usuario-access-section">
        <legend>Acceso a propiedades</legend>
        <label className="checkbox-line"><input type="checkbox" checked={allProperties} onChange={(event) => setAllProperties(event.target.checked)} />Puede acceder a todas las propiedades</label>
        {!allProperties && <div className="usuario-choice-grid">{catalogs.data.properties.map((property) => <label key={property.id} className="checkbox-line"><input type="checkbox" checked={propertyIds.includes(property.id)} onChange={(event) => toggle(propertyIds, setPropertyIds, property.id, event.target.checked)} /><span><strong>{property.nombre}</strong><small>{property.codigo}</small></span></label>)}</div>}
      </fieldset>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button><Button type="submit" loading={create.isPending} disabled={roleIds.length === 0}><UserPlus size={17} aria-hidden="true" />Crear y enviar invitación</Button></div>
    </form>}
  </Modal>
}
