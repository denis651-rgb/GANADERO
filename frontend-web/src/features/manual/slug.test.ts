import { describe, expect, it } from 'vitest'
import { slugify } from './slug'

describe('slugify', () => {
  it('pasa a minúsculas y reemplaza espacios por guiones', () => {
    expect(slugify('Compra individual')).toBe('compra-individual')
  })

  it('quita tildes y eñes conservando el sonido', () => {
    expect(slugify('Diagnóstico de gestación')).toBe('diagnostico-de-gestacion')
  })

  it('quita signos de interrogación y otros símbolos', () => {
    expect(slugify('¿Pregunta tal como la haría el usuario?')).toBe('pregunta-tal-como-la-haria-el-usuario')
  })

  it('colapsa espacios múltiples en un solo guion', () => {
    expect(slugify('No   aparece el potrero')).toBe('no-aparece-el-potrero')
  })
})
