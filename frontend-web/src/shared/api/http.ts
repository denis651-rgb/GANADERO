import axios from 'axios'
import { createUuid } from '@/shared/utils/uuid'

let tokenProvider: () => Promise<string | null> = async () => null

export function setAccessTokenProvider(provider: () => Promise<string | null>) {
  tokenProvider = provider
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use(async (config) => {
  const token = await tokenProvider()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  config.headers.set('X-Correlation-Id', createUuid())
  return config
})
