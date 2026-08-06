import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { supabase } from '@/auth/supabase'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'

const GENERIC_MESSAGE = 'Si el correo está registrado, recibirás instrucciones para continuar.'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [diagnostic, setDiagnostic] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setMessage(null)
    setDiagnostic(null)

    try {
      if (!supabase) {
        throw new Error('Supabase no está configurado en el frontend.')
      }

      const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
        redirectTo: `${window.location.origin}/auth/restablecer-contrasena`,
      })

      if (error) {
        throw error
      }

      setMessage(GENERIC_MESSAGE)
    } catch (error) {
      console.error('No se pudo solicitar la recuperación de contraseña.', error)
      setMessage(GENERIC_MESSAGE)

      if (import.meta.env.DEV) {
        const details = error instanceof Error ? error.message : 'Error desconocido de Supabase Auth.'
        setDiagnostic(`Diagnóstico local: ${details}`)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="simple-auth-page">
      <form className="auth-card" onSubmit={submit}>
        <img src="/logo.svg" alt="GANADERO" width="58" height="58" />
        <div>
          <p className="eyebrow">Recuperación</p>
          <h1>Restablecer contraseña</h1>
          <p className="muted">Te enviaremos instrucciones si el correo está registrado.</p>
        </div>
        {message && <Alert tone="info">{message}</Alert>}
        {diagnostic && <Alert tone="danger">{diagnostic}</Alert>}
        <Field label="Correo electrónico">
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>
        <Button type="submit" fullWidth loading={loading}>Enviar enlace</Button>
        <Link className="text-link centered" to="/login">Volver al inicio de sesión</Link>
      </form>
    </main>
  )
}
