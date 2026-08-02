import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import type { Session } from '@supabase/supabase-js'
import { supabase } from '@/auth/supabase'
import type { AuthStatus, AuthUser, SignInInput } from '@/auth/auth.types'
import { AuthContext, type AuthContextValue } from '@/auth/auth-context'
import { setAccessTokenProvider } from '@/shared/api/http'

const MOCK_STORAGE_KEY = 'ganadero.mock.user'
const authMode = import.meta.env.VITE_AUTH_MODE ?? 'supabase'
const enforcePermissions = import.meta.env.VITE_ENFORCE_UI_PERMISSIONS !== 'false'

function mockUser(email: string): AuthUser {
  return {
    id: '00000000-0000-0000-0000-000000000001',
    email,
    displayName: 'Administrador GANADERO',
    companyId: '00000000-0000-0000-0000-000000000001',
    companyName: 'Empresa piloto',
    roles: ['PROPIETARIO'],
    permissions: ['*'],
    propertyIds: [],
  }
}

function storedMockUser(): AuthUser | null {
  if (authMode !== 'mock') return null
  try {
    const stored = localStorage.getItem(MOCK_STORAGE_KEY)
    return stored ? JSON.parse(stored) as AuthUser : null
  } catch {
    localStorage.removeItem(MOCK_STORAGE_KEY)
    return null
  }
}

function userFromSession(session: Session): AuthUser {
  const metadata = session.user.user_metadata
  const name = typeof metadata.full_name === 'string' ? metadata.full_name : session.user.email ?? 'Usuario'
  return {
    id: session.user.id,
    email: session.user.email ?? '',
    displayName: name,
    companyName: 'Empresa pendiente de configurar',
    roles: [],
    permissions: [],
    propertyIds: [],
  }
}

async function enrichUser(session: Session): Promise<AuthUser> {
  const fallback = userFromSession(session)
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

  try {
    const response = await fetch(`${apiUrl}/api/v1/auth/me`, {
      headers: { Authorization: `Bearer ${session.access_token}` },
    })
    if (!response.ok) return fallback
    const body = await response.json() as {
      data?: {
        usuarioId?: string
        nombres?: string
        apellidos?: string
        empresa?: { id?: string; nombre?: string }
        roles?: string[]
        permisos?: string[]
        propiedadesPermitidas?: string[]
      }
    }
    const data = body.data
    if (!data) return fallback

    return {
      id: data.usuarioId ?? fallback.id,
      email: fallback.email,
      displayName: [data.nombres, data.apellidos].filter(Boolean).join(' ') || fallback.displayName,
      companyId: data.empresa?.id,
      companyName: data.empresa?.nombre ?? fallback.companyName,
      roles: data.roles ?? [],
      permissions: data.permisos ?? [],
      propertyIds: data.propiedadesPermitidas ?? [],
    }
  } catch {
    return fallback
  }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [initialMockUser] = useState(() => storedMockUser())
  const [status, setStatus] = useState<AuthStatus>(authMode === 'mock'
    ? (initialMockUser ? 'authenticated' : 'anonymous')
    : (supabase ? 'loading' : 'anonymous'))
  const [user, setUser] = useState<AuthUser | null>(initialMockUser)
  const [session, setSession] = useState<Session | null>(null)

  useEffect(() => {
    setAccessTokenProvider(async () => session?.access_token ?? null)
  }, [session])

  useEffect(() => {
    if (authMode === 'mock') {
      return
    }

    if (!supabase) {
      console.error('Supabase Auth está habilitado, pero faltan VITE_SUPABASE_URL o VITE_SUPABASE_ANON_KEY.')
      return
    }

    let active = true
    void supabase.auth.getSession().then(async ({ data }) => {
      if (!active) return
      setSession(data.session)
      if (data.session) {
        setUser(await enrichUser(data.session))
        setStatus('authenticated')
      } else {
        setStatus('anonymous')
      }
    })

    const { data: subscription } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession)
      if (nextSession) {
        void enrichUser(nextSession).then((nextUser) => {
          if (!active) return
          setUser(nextUser)
          setStatus('authenticated')
        })
      } else {
        setUser(null)
        setStatus('anonymous')
      }
    })

    return () => {
      active = false
      subscription.subscription.unsubscribe()
    }
  }, [])

  const signIn = useCallback(async ({ email, password }: SignInInput) => {
    if (authMode === 'mock') {
      if (!email.trim() || password.length < 4) {
        throw new Error('Ingresa un correo y una contraseña de al menos 4 caracteres.')
      }
      const nextUser = mockUser(email)
      localStorage.setItem(MOCK_STORAGE_KEY, JSON.stringify(nextUser))
      setUser(nextUser)
      setStatus('authenticated')
      return
    }

    if (!supabase) throw new Error('Supabase no está configurado.')
    const { error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) throw error
  }, [])

  const signOut = useCallback(async () => {
    if (authMode === 'mock') {
      localStorage.removeItem(MOCK_STORAGE_KEY)
    } else if (supabase) {
      await supabase.auth.signOut()
    }
    setSession(null)
    setUser(null)
    setStatus('anonymous')
  }, [])

  const can = useCallback((permission: string) => {
    if (!enforcePermissions) return true
    return Boolean(user?.permissions.includes('*') || user?.permissions.includes(permission))
  }, [user])

  const value = useMemo<AuthContextValue>(() => ({ status, user, signIn, signOut, can }), [status, user, signIn, signOut, can])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
