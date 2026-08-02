import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, ShieldCheck } from 'lucide-react'
import { assignPermisos, createRol, listPermisos, listRoles, type Permiso } from '@/features/roles/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function RolesPage() {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [selected, setSelected] = useState<string | null>(null)
  const [selectedPermissions, setSelectedPermissions] = useState<Set<string>>(new Set())
  const [toast, setToast] = useState<string | null>(null)
  const permissionSectionRef = useRef<HTMLDivElement>(null)

  const query = useQuery({
    queryKey: ['roles-completos'],
    queryFn: async () => {
      const [roles, permissions] = await Promise.all([listRoles(), listPermisos()])
      return { roles, permissions }
    },
  })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createRol({ codigo: String(data.get('codigo')).toUpperCase(), nombre: String(data.get('nombre')), descripcion: String(data.get('descripcion') ?? '') })
    },
    onSuccess: () => {
      setShowForm(false)
      void client.invalidateQueries({ queryKey: ['roles-completos'] })
    },
  })
  const permissions = useMutation({
    mutationFn: () => {
      const role = query.data!.roles.find((item) => item.id === selected)!
      return assignPermisos(role.id, Array.from(selectedPermissions), role.version)
    },
    onSuccess: async (role) => {
      setSelectedPermissions(new Set(role.permisos.map((permission) => permission.id)))
      setToast(`Los permisos de ${role.nombre} se actualizaron correctamente.`)
      await client.invalidateQueries({ queryKey: ['roles-completos'] })
    },
  })

  const error = query.error ?? create.error ?? permissions.error
  const selectedRole = query.data?.roles.find((item) => item.id === selected)
  const byModule = (query.data?.permissions ?? []).reduce<Map<string, Permiso[]>>((groups, permission) => {
    const values = groups.get(permission.modulo) ?? []
    values.push(permission)
    groups.set(permission.modulo, values)
    return groups
  }, new Map())

  useEffect(() => {
    if (!toast) return
    const timeoutId = window.setTimeout(() => setToast(null), 3500)
    return () => window.clearTimeout(timeoutId)
  }, [toast])

  function configureRole(roleId: string) {
    const role = query.data?.roles.find((item) => item.id === roleId)
    if (!role) return
    setSelected(roleId)
    setSelectedPermissions(new Set(role.permisos.map((permission) => permission.id)))
    window.setTimeout(() => {
      permissionSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      permissionSectionRef.current?.focus({ preventScroll: true })
    }, 0)
  }

  function togglePermission(permissionId: string, checked: boolean) {
    setSelectedPermissions((current) => {
      const next = new Set(current)
      if (checked) next.add(permissionId)
      else next.delete(permissionId)
      return next
    })
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Seguridad" title="Roles y permisos" description="Define qué puede consultar y modificar cada perfil." actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} />Nuevo rol</Button>} />
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {showForm && <Card><form className="form-grid" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}><Field label="Código"><input name="codigo" required pattern="[A-Za-z0-9_]+" /></Field><Field label="Nombre"><input name="nombre" required /></Field><Field label="Descripción"><input name="descripcion" /></Field><div className="form-actions"><Button type="submit" loading={create.isPending}>Crear rol</Button></div></form></Card>}

      <Card>
        {query.isPending && <LoadingState message="Cargando roles…" />}
        {query.data?.roles.length === 0 && <EmptyState title="No hay roles" description="Crea el primer rol empresarial." />}
        {query.data && <div className="table-wrapper"><table><thead><tr><th>Rol</th><th>Tipo</th><th>Permisos</th><th>Estado</th><th></th></tr></thead><tbody>{query.data.roles.map((role) => <tr key={role.id}><td><strong>{role.nombre}</strong><span className="table-secondary">{role.codigo}</span></td><td>{role.sistema ? 'Sistema' : 'Personalizado'}</td><td>{role.permisos.length}</td><td><span className="status-badge">{role.activo ? 'ACTIVO' : 'INACTIVO'}</span></td><td><Button variant="ghost" onClick={() => configureRole(role.id)}><ShieldCheck size={16} />Configurar</Button></td></tr>)}</tbody></table></div>}
      </Card>

      {selectedRole && <div ref={permissionSectionRef} tabIndex={-1} className="permission-section"><Card>
        <h3>Permisos de {selectedRole.nombre}</h3>
        <form onSubmit={(event) => { event.preventDefault(); permissions.mutate() }}>
          <div className="permission-grid">{Array.from(byModule.entries()).map(([module, items]) => <fieldset key={module}><legend>{module}</legend>{items.map((permission) => <label key={permission.id}><input type="checkbox" name="permisos" value={permission.id} checked={selectedPermissions.has(permission.id)} onChange={(event) => togglePermission(permission.id, event.target.checked)} /> {permission.nombre}</label>)}</fieldset>)}</div>
          <div className="form-actions"><Button type="submit" loading={permissions.isPending}>Guardar permisos</Button></div>
        </form>
      </Card></div>}

      {toast && <div className="success-toast" role="status" aria-live="polite"><ShieldCheck size={21} /><div><strong>Permisos actualizados</strong><span>{toast}</span></div></div>}
    </div>
  )
}
