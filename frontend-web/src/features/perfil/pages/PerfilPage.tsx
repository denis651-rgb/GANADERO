import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { supabase } from '@/auth/supabase'
import { getPerfil, updatePerfil } from '@/features/perfil/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { useUnsavedChanges } from '@/shared/hooks/useUnsavedChanges'
import { useToast } from '@/shared/toast/useToast'
import { PushSettings } from '@/features/alertas/PushSettings'

export function PerfilPage() {
  const [profileDirty, setProfileDirty] = useState(false)
  const [passwordDirty, setPasswordDirty] = useState(false)
  useUnsavedChanges(profileDirty || passwordDirty)
  const client = useQueryClient()
  const { showToast } = useToast()
  const query = useQuery({ queryKey: ['perfil'], queryFn: getPerfil })

  const save = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const d = new FormData(form)
      return updatePerfil({
        nombres: String(d.get('nombres')),
        apellidos: String(d.get('apellidos')),
        telefono: String(d.get('telefono') ?? ''),
        version: query.data!.version,
      })
    },
    onSuccess: () => {
      setProfileDirty(false)
      showToast('Perfil actualizado.')
      client.invalidateQueries({ queryKey: ['perfil'] })
    },
  })

  const password = useMutation({
    mutationFn: async (form: HTMLFormElement) => {
      const d = new FormData(form)
      const value = String(d.get('password'))
      const confirmation = String(d.get('confirmation'))
      if (value.length < 10) throw new Error('La contraseña debe tener al menos 10 caracteres.')
      if (value !== confirmation) throw new Error('Las contraseñas no coinciden.')
      if (!supabase) throw new Error('Supabase no está configurado.')
      const { error } = await supabase.auth.updateUser({ password: value })
      if (error) throw new Error('No se pudo cambiar la contraseña.')
    },
    onSuccess: () => {
      setPasswordDirty(false)
      showToast('Contraseña actualizada.')
    },
  })

  if (query.isPending) return <LoadingState message="Cargando perfil…" />
  if (!query.data) return <Alert tone="danger">No se pudo cargar el perfil.</Alert>

  const p = query.data

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Cuenta" title="Mi perfil" description={`${p.empresa} · ${p.estado}`} />
      <Card>
        <form className="form-grid" onChange={() => setProfileDirty(true)} onSubmit={(event) => { event.preventDefault(); save.mutate(event.currentTarget) }}>
          <Field label="Correo" disabled><input type="email" autoComplete="email" value={p.email ?? ''} disabled /></Field>
          <Field label="Cargo"><input value={p.cargo ?? ''} disabled /></Field>
          <Field label="Nombres" required><input name="nombres" autoComplete="given-name" required defaultValue={p.nombres} /></Field>
          <Field label="Apellidos" required><input name="apellidos" autoComplete="family-name" required defaultValue={p.apellidos} /></Field>
          <Field label="Teléfono"><input name="telefono" type="tel" autoComplete="tel" inputMode="tel" defaultValue={p.telefono ?? ''} /></Field>
          <div><strong>Roles:</strong> {p.roles.join(', ') || 'Sin roles'}</div>
          <div className="form-actions"><Button type="submit" loading={save.isPending}>Guardar perfil</Button></div>
        </form>
      </Card>
      <Card>
        <h2>Cambiar contraseña</h2>
        {password.error && <Alert tone="danger">{password.error.message}</Alert>}
        <form className="form-grid" onChange={() => setPasswordDirty(true)} onSubmit={(event) => { event.preventDefault(); password.mutate(event.currentTarget) }}>
          <Field label="Nueva contraseña" required><input name="password" type="password" autoComplete="new-password" minLength={10} required /></Field>
          <Field label="Confirmación" required><input name="confirmation" type="password" autoComplete="new-password" minLength={10} required /></Field>
          <div className="form-actions"><Button type="submit" loading={password.isPending}>Cambiar contraseña</Button></div>
        </form>
      </Card>
      <PushSettings />
    </div>
  )
}
