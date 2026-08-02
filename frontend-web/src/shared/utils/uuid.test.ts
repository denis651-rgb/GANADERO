import { describe, expect, it } from 'vitest'
import { createUuid } from '@/shared/utils/uuid'

describe('createUuid', () => {
  it('genera identificadores UUID', () => {
    expect(createUuid()).toMatch(/^[0-9a-f-]{36}$/i)
  })
})
