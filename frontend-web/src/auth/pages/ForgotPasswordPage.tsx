import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { supabase } from '@/auth/supabase'
import { Button } from '@/shared/components/Button'
import { Alert } from '@/shared/components/Alert'
import { Field } from '@/shared/components/Field'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setMessage(null)
    try {
      if (!supabase) {
        setMessage('En modo local no se envían correos. Configura Supabase para habilitar esta función.')
        return
      }
      const { error } = await supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${window.location.origin}/login`,
      })
      if (error) throw error
      setMessage('Revisa tu correo para continuar con la recuperación.')
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : 'No se pudo procesar la solicitud.')
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
          <p className="muted">Te enviaremos un enlace al correo registrado.</p>
        </div>
        {message && <Alert tone="info">{message}</Alert>}
        <Field label="Correo electrónico">
          <input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </Field>
        <Button type="submit" fullWidth loading={loading}>Enviar enlace</Button>
        <Link className="text-link centered" to="/login">Volver al inicio de sesión</Link>
      </form>
    </main>
  )
}
