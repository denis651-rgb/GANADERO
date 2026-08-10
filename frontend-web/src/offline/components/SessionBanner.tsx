import { KeyRound } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { Button } from '@/shared/components/Button'

export function SessionBanner() {
  const { sessionExpired, signOut } = useAuth()
  const navigate = useNavigate()

  if (!sessionExpired) return null

  return (
    <div className="session-banner" role="alertdialog" aria-label="Sesión expirada">
      <span>Tu sesión expiró. Conéctate e inicia sesión de nuevo para sincronizar; tus datos locales están a salvo.</span>
      <Button
        onClick={() => {
          void signOut().then(() => navigate('/login', { replace: true }))
        }}
      >
        <KeyRound size={15} />Reautenticar
      </Button>
    </div>
  )
}
