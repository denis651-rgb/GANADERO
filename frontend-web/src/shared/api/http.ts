import axios from 'axios'
import { createUuid } from '@/shared/utils/uuid'

declare global {
  interface Window {
    ganadero?: {
      apiBaseUrl?: string
      googleCalendar?: {
        status: () => Promise<GoogleOAuthDesktopStatus>
        importClientConfig: (jsonText: string, fileName: string) => Promise<GoogleOAuthDesktopStatus>
        connect: () => Promise<GoogleOAuthDesktopStatus>
        revoke: () => Promise<GoogleOAuthDesktopStatus>
        changeAccount: () => Promise<GoogleOAuthDesktopStatus>
        syncNow: () => Promise<GoogleCalendarSyncResult>
      }
    }
  }
}

export interface GoogleOAuthDesktopStatus {
  available: boolean
  connected: boolean
  email?: string
  expiresAt?: string
  message?: string
}

export interface GoogleCalendarSyncResult {
  generated: number
  claimed: number
  completed: number
  failed: number
  skipped?: 'NOT_CONNECTED' | 'AUTOMATIC_DISABLED'
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
