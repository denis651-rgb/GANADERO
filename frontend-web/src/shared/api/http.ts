import axios from 'axios'
import { createUuid } from '@/shared/utils/uuid'

declare global {
  interface Window {
    ganadero?: { apiBaseUrl?: string }
  }
}

export const http = axios.create({
  baseURL: window.ganadero?.apiBaseUrl || import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  config.headers.set('X-Correlation-Id', createUuid())
  return config
})
