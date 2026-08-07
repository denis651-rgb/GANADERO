import axios, { type AxiosError } from 'axios'
import { isAccessTokenExpired } from '@/auth/session'
import { createUuid } from '@/shared/utils/uuid'

let tokenProvider: () => Promise<string | null> = async () => null

export function setAccessTokenProvider(provider: () => Promise<string | null>) {
  tokenProvider = provider
}

export function getAccessToken(): Promise<string | null> {
  return tokenProvider()
}

export class SessionExpiredError extends Error {
  readonly code = 'SESSION_EXPIRED'

  constructor() {
    super('La sesión expiró. Vuelve a iniciar sesión para continuar sincronizando.')
    this.name = 'SessionExpiredError'
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use(async (config) => {
  const token = await tokenProvider()
  if (token && isAccessTokenExpired(token)) {
    const error = new SessionExpiredError() as AxiosError
    error.status = 401
    throw error
  }
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  config.headers.set('X-Correlation-Id', createUuid())
  return config
})
