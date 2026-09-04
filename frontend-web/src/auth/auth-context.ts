import { createContext, useContext } from 'react'
import type { AuthUser } from '@/auth/auth.types'

export interface AuthContextValue {
  user: AuthUser
  can: (permission: string) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe utilizarse dentro de AuthProvider.')
  return context
}
