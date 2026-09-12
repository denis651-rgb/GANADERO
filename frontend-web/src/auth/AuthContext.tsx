import { useMemo, type PropsWithChildren } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getConfiguracion } from '@/features/configuracion/api'
import { AuthContext, type AuthContextValue } from '@/auth/auth-context'

const LOCAL_USER_ID = '00000000-0000-0000-0000-000000000001'
const DEFAULT_DISPLAY_NAME = 'Usuario local'

export function AuthProvider({ children }: PropsWithChildren) {
  // El nombre para mostrar es editable en Configuración general (Configuracion.nombreUsuario);
  // no bloquea el render mientras carga para no volver toda la app dependiente de este endpoint.
  const config = useQuery({ queryKey: ['configuracion'], queryFn: getConfiguracion, staleTime: 60_000 })
  const value = useMemo<AuthContextValue>(() => ({
    user: { id: LOCAL_USER_ID, displayName: config.data?.nombreUsuario?.trim() || DEFAULT_DISPLAY_NAME },
    can: () => true,
  }), [config.data?.nombreUsuario])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
