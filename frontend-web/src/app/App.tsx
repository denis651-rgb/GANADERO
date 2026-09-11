import { BrowserRouter } from 'react-router'
import { AppProviders } from '@/app/providers/AppProviders'
import { AppRouter } from '@/app/router'
import { RecoveryScreen } from '@/features/recuperacion/RecoveryScreen'

export function App() {
  // El backend (y por lo tanto la base de datos) no pudo iniciar: se muestra la pantalla de
  // recuperación en vez del router normal — no depende de ninguna llamada HTTP al backend.
  if (window.ganadero?.backups?.backendStatus === 'failed') {
    return <RecoveryScreen />
  }

  return (
    <BrowserRouter>
      <AppProviders>
        <AppRouter />
      </AppProviders>
    </BrowserRouter>
  )
}
