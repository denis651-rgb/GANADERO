import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { LoadingState } from '@/shared/components/LoadingState'

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <LoadingState fullscreen message="Comprobando sesión…" />
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
