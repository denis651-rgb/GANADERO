import { describe, expect, it } from 'vitest'
import { foldText, searchManual } from './search'

describe('foldText', () => {
  it('pasa a minúsculas', () => {
    expect(foldText('EDAD Aproximada')).toBe('edad aproximada')
  })

  it('quita tildes sin cambiar la posición de las letras', () => {
    expect(foldText('Diagnóstico')).toBe('diagnostico')
    expect(foldText('Diagnóstico').length).toBe('Diagnóstico'.length)
  })
})

describe('searchManual', () => {
  it('con menos de 2 caracteres no busca nada', () => {
    expect(searchManual('')).toEqual([])
    expect(searchManual('a')).toEqual([])
  })

  it('encuentra "edad aproximada" en el capítulo de Compras, ignorando mayúsculas y tildes', () => {
    const results = searchManual('EDAD APROXIMADA')
    expect(results.length).toBeGreaterThan(0)
    expect(results.some((r) => r.chapterId === 'compras')).toBe(true)
    const match = results.find((r) => r.chapterId === 'compras')!
    expect(match.excerpt.toLowerCase()).toContain('edad aproximada')
    expect(match.anchor.length).toBeGreaterThan(0)
  })

  it('busca por título de capítulo además de por contenido', () => {
    const results = searchManual('reproduccion')
    expect(results.some((r) => r.chapterId === 'reproduccion')).toBe(true)
  })

  it('limita la cantidad de resultados', () => {
    const results = searchManual('el', 3)
    expect(results.length).toBeLessThanOrEqual(3)
  })

  it('no incluye el markdown crudo (## , **, etc.) en el título/encabezado indexado', () => {
    const results = searchManual('compra de ganado')
    const hit = results.find((r) => r.chapterId === 'compras')
    expect(hit?.heading).not.toMatch(/[#*_]/)
  })
})
