import { describe, expect, it } from 'vitest'
import { periodoKeyActual } from './scheduler'

describe('periodoKeyActual', () => {
  it('genera la misma clave diaria para dos momentos del mismo día calendario en Bolivia', () => {
    const manana = new Date('2026-09-06T13:00:00Z') // 09:00 en America/La_Paz (UTC-4)
    const noche = new Date('2026-09-07T02:00:00Z') // 22:00 del mismo 6 de septiembre en Bolivia
    expect(periodoKeyActual('DIARIA', manana)).toBe('BACKUP:DIARIA:2026-09-06')
    expect(periodoKeyActual('DIARIA', noche)).toBe('BACKUP:DIARIA:2026-09-06')
  })

  it('cambia la clave diaria al cruzar la medianoche de Bolivia', () => {
    const antesDeMedianoche = new Date('2026-09-07T03:59:00Z') // 23:59 del 6 de sept. en Bolivia
    const despuesDeMedianoche = new Date('2026-09-07T04:01:00Z') // 00:01 del 7 de sept. en Bolivia
    expect(periodoKeyActual('DIARIA', antesDeMedianoche)).toBe('BACKUP:DIARIA:2026-09-06')
    expect(periodoKeyActual('DIARIA', despuesDeMedianoche)).toBe('BACKUP:DIARIA:2026-09-07')
  })

  it('genera una clave semanal en formato ISO-8601 (YYYY-Www)', () => {
    const lunes = new Date('2026-09-07T13:00:00Z') // lunes en Bolivia
    expect(periodoKeyActual('SEMANAL', lunes)).toMatch(/^BACKUP:SEMANAL:\d{4}-W\d{2}$/)
  })

  it('genera una clave mensual en formato YYYY-MM', () => {
    const fecha = new Date('2026-09-06T13:00:00Z')
    expect(periodoKeyActual('MENSUAL', fecha)).toBe('BACKUP:MENSUAL:2026-09')
  })
})
