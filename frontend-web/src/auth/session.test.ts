import { describe, expect, it } from 'vitest'
import { accessTokenExpiry, isAccessTokenExpired } from '@/auth/session'

function buildToken(exp: number | undefined): string {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const payload = btoa(JSON.stringify({ sub: 'usuario', exp }))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${header}.${payload}.firma`
}

describe('session', () => {
  it('lee la expiración de un JWT', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600
    expect(accessTokenExpiry(buildToken(exp))).toBe(exp * 1000)
  })

  it('considera vencido un token con exp pasado', () => {
    const exp = Math.floor(Date.now() / 1000) - 60
    expect(isAccessTokenExpired(buildToken(exp))).toBe(true)
  })

  it('considera vigente un token con exp futuro', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600
    expect(isAccessTokenExpired(buildToken(exp))).toBe(false)
  })

  it('no marca vencidos tokens sin payload ni valores nulos', () => {
    expect(isAccessTokenExpired(null)).toBe(false)
    expect(isAccessTokenExpired('no-es-un-jwt')).toBe(false)
    expect(isAccessTokenExpired(buildToken(undefined))).toBe(false)
  })
})
