import { createContext, useContext } from 'react'
import type { AuthStatus, AuthUser, SignInInput } from '@/auth/auth.types'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthUser | null
  signIn: (input: SignInInput) => Promise<void>
  signOut: () => Promise<void>
  can: (permission: string) => boolean
  sessionExpired: boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe utilizarse dentro de AuthProvider.')
  return context
}
