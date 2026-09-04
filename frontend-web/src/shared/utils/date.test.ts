import { describe, expect, it } from 'vitest'
import { todayInBolivia } from './date'

describe('todayInBolivia', () => {
  it.each([
    ['2026-09-04T01:00:00Z', '2026-09-03'],
    ['2026-09-04T03:59:59Z', '2026-09-03'],
    ['2026-09-04T04:00:00Z', '2026-09-04'],
    ['2027-01-01T02:00:00Z', '2026-12-31'],
  ])('convierte %s a la fecha de Bolivia %s', (instant, expected) => {
    expect(todayInBolivia(new Date(instant))).toBe(expected)
  })
})
