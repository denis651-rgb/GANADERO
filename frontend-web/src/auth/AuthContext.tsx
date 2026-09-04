import { useMemo, type PropsWithChildren } from 'react'
import type { AuthUser } from '@/auth/auth.types'
import { AuthContext, type AuthContextValue } from '@/auth/auth-context'

const LOCAL_USER: AuthUser = {
  id: '00000000-0000-0000-0000-000000000001',
  displayName: 'Usuario local',
}

export function AuthProvider({ children }: PropsWithChildren) {
  const value = useMemo<AuthContextValue>(() => ({ user: LOCAL_USER, can: () => true }), [])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
