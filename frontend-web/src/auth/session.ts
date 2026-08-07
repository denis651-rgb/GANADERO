export interface SessionExpiryInfo {
  expiresAt: number | null
  expired: boolean
}

export function accessTokenExpiry(token: string): number | null {
  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) return null
    const padded = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    const json = JSON.parse(atob(padded))
    return typeof json.exp === 'number' ? json.exp * 1000 : null
  } catch {
    return null
  }
}

export function isAccessTokenExpired(token: string | null | undefined): boolean {
  if (!token) return false
  const expiresAt = accessTokenExpiry(token)
  if (expiresAt == null) return false
  return Date.now() >= expiresAt
}

export function sessionExpiryInfo(token: string | null | undefined): SessionExpiryInfo {
  if (!token) {
    return { expiresAt: null, expired: false }
  }
  const expiresAt = accessTokenExpiry(token)
  if (expiresAt == null) {
    return { expiresAt: null, expired: false }
  }
  return { expiresAt, expired: Date.now() >= expiresAt }
}
