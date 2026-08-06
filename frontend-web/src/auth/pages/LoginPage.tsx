import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { LockKeyhole, Mail } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { Button } from '@/shared/components/Button'
import { Alert } from '@/shared/components/Alert'
import { Field } from '@/shared/components/Field'

const schema = z.object({
  email: z.string().email('Ingresa un correo válido.'),
  password: z.string().min(4, 'La contraseña debe tener al menos 4 caracteres.'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { status, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  if (status === 'authenticated') return <Navigate to="/" replace />

  const destination = (location.state as { from?: string } | null)?.from ?? '/'

  async function submit(values: FormValues) {
    setError(null)
    try {
      await signIn(values)
      navigate(destination, { replace: true })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo iniciar sesión.')
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-hero">
        <div className="auth-hero-content">
          <img src="/logo.svg" alt="" width="72" height="72" />
          <p className="eyebrow light">Gestión productiva y de campo</p>
          <h1>Controla cada animal, movimiento y decisión.</h1>
          <p>Una aplicación preparada para la operación diaria, el trabajo móvil y la sincronización sin conexión.</p>
        </div>
      </section>

      <section className="auth-form-panel">
        <form className="auth-card" onSubmit={handleSubmit(submit)} noValidate>
          <div>
            <p className="eyebrow">Bienvenido</p>
            <h2>Iniciar sesión</h2>
            <p className="muted">Accede al sistema GANADERO.</p>
          </div>

          {import.meta.env.VITE_AUTH_MODE !== 'supabase' && (
            <Alert tone="info" title="Modo de desarrollo">
              Puedes ingresar con los datos precargados. Cambia VITE_AUTH_MODE a supabase para autenticación real.
            </Alert>
          )}

          {error && <Alert tone="danger" title="No se pudo ingresar">{error}</Alert>}

          <Field label="Correo electrónico" error={errors.email?.message} icon={<Mail size={18} />}>
            <input type="email" autoComplete="email" {...register('email')} />
          </Field>

          <Field label="Contraseña" error={errors.password?.message} icon={<LockKeyhole size={18} />}>
            <input type="password" autoComplete="current-password" {...register('password')} />
          </Field>

          <Button type="submit" fullWidth loading={isSubmitting}>Ingresar</Button>
          <Link className="text-link centered" to="/recuperar-contrasena">¿Olvidaste tu contraseña?</Link>
        </form>
      </section>
    </main>
  )
}
