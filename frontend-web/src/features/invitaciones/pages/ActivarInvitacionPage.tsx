import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { supabase } from '@/auth/supabase'
import { activarInvitacion, type ActivacionInvitacionResponse } from '@/features/invitaciones/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'

export function ActivarInvitacionPage() {
  const navigate = useNavigate()
  const [ready, setReady] = useState(false)
  const [invalid, setInvalid] = useState(!supabase)
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [activated, setActivated] = useState<ActivacionInvitacionResponse | null>(null)

  useEffect(() => {
    if (supabase) void supabase.auth.getSession().then(({ data }) => {
      setReady(Boolean(data.session))
      setInvalid(!data.session)
    })
  }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    if (password.length < 10) {
      setMessage('La contraseña debe tener al menos 10 caracteres.')
      return
    }
    if (password !== confirmation) {
      setMessage('Las contraseñas no coinciden.')
      return
    }
    if (!supabase || !ready) {
      setInvalid(true)
      return
    }
    setLoading(true)
    const { error } = await supabase.auth.updateUser({ password })
    if (error) {
      setMessage('No se pudo actualizar la contraseña. El enlace puede haber vencido.')
      setLoading(false)
      return
    }
    try {
      const result = await activarInvitacion()
      setActivated(result)
      await supabase.auth.signOut()
      setMessage('Invitación activada correctamente. Ya puedes iniciar sesión.')
      window.setTimeout(() => navigate('/login', { replace: true }), 1500)
    } catch {
      setMessage('No se pudo activar la invitación. El enlace puede haber vencido o ya fue utilizado.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="simple-auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>Activar invitación</h1>
        <p className="muted">
          {activated
            ? `Bienvenido a ${activated.nombreEmpresa}.`
            : 'Define tu contraseña para activar tu acceso a la empresa.'}
        </p>
        {activated && (
          <Alert tone="success">
            Tu cuenta ya tiene acceso a {activated.nombreEmpresa}. Serás redirigido al inicio de sesión.
          </Alert>
        )}
        {invalid && <Alert tone="danger">El enlace es inválido, venció o ya fue utilizado.</Alert>}
        {message && <Alert tone={message.includes('correctamente') ? 'success' : 'danger'}>{message}</Alert>}
        <Field label="Nueva contraseña">
          <input
            type="password"
            autoComplete="new-password"
            minLength={10}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>
        <Field label="Confirmar contraseña">
          <input
            type="password"
            autoComplete="new-password"
            minLength={10}
            required
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
          />
        </Field>
        <Button type="submit" fullWidth loading={loading} disabled={!ready || Boolean(activated)}>
          Guardar contraseña y activar acceso
        </Button>
        <Link className="text-link centered" to="/login">Volver al login</Link>
      </form>
    </main>
  )
}
