import { BrowserRouter, useLocation, useNavigate } from 'react-router'
import { AppProviders } from '@/app/providers/AppProviders'
import { AppRouter } from '@/app/router'
import { RecoveryScreen } from '@/features/recuperacion/RecoveryScreen'
import { ErrorBoundary } from '@/shared/components/ErrorBoundary'

function RouterErrorBoundary() {
  // La key por ruta reinicia el boundary al navegar, para no quedar "atascado" en el error.
  const location = useLocation()
  const navigate = useNavigate()
  return <ErrorBoundary key={location.pathname} onBack={() => navigate(-1)}><AppRouter /></ErrorBoundary>
}

export function App() {
  // El backend (y por lo tanto la base de datos) no pudo iniciar: se muestra la pantalla de
  // recuperación en vez del router normal — no depende de ninguna llamada HTTP al backend.
  if (window.ganadero?.backups?.backendStatus === 'failed') {
    return <RecoveryScreen />
  }

  return (
    <BrowserRouter>
      <AppProviders>
        <RouterErrorBoundary />
      </AppProviders>
    </BrowserRouter>
  )
}
