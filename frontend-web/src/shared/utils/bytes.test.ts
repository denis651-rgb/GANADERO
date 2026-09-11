import { describe, expect, it } from 'vitest'
import { formatBytes } from './bytes'

describe('formatBytes', () => {
  it('formatea bytes crudos', () => {
    expect(formatBytes(512)).toBe('512 B')
  })

  it('formatea KB, MB y GB', () => {
    expect(formatBytes(2048)).toBe('2.0 KB')
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB')
    expect(formatBytes(3 * 1024 * 1024 * 1024)).toBe('3.0 GB')
  })

  it('devuelve un guion para valores ausentes', () => {
    expect(formatBytes(undefined)).toBe('—')
    expect(formatBytes(null)).toBe('—')
  })
})
