import { describe, expect, it } from 'vitest'
import { getAdjacentChapters, getManualChapter, manual, manualChapters, resolveChapterLink } from './manualRegistry'

describe('manualRegistry', () => {
  it('carga todos los capítulos declarados en manual.json con su contenido', () => {
    expect(manualChapters).toHaveLength(manual.chapters.length)
    manualChapters.forEach((chapter) => {
      expect(chapter.content.length).toBeGreaterThan(0)
      expect(chapter.content).toContain(`# `)
    })
  })

  it('getManualChapter sin id devuelve la portada (primer capítulo)', () => {
    expect(getManualChapter(undefined)?.id).toBe(manualChapters[0].id)
  })

  it('getManualChapter con un id inexistente devuelve undefined', () => {
    expect(getManualChapter('no-existe')).toBeUndefined()
  })

  it('getManualChapter con un id válido devuelve ese capítulo', () => {
    expect(getManualChapter('compras')?.title).toBe('Compras')
  })

  it('getAdjacentChapters devuelve anterior/siguiente correctos en el medio de la lista', () => {
    const { previous, next } = getAdjacentChapters('compras')
    expect(previous?.id).toBe('animales')
    expect(next?.id).toBe('lotes')
  })

  it('getAdjacentChapters no tiene anterior en el primer capítulo ni siguiente en el último', () => {
    expect(getAdjacentChapters(manualChapters[0].id).previous).toBeUndefined()
    expect(getAdjacentChapters(manualChapters[manualChapters.length - 1].id).next).toBeUndefined()
  })

  it('resolveChapterLink resuelve un enlace relativo con ancla a su ruta dentro de la app', () => {
    expect(resolveChapterLink('./08-reproduccion.md#registrar-parto')).toBe('/manual/reproduccion#registrar-parto')
  })

  it('resolveChapterLink resuelve un enlace sin "./" ni ancla', () => {
    expect(resolveChapterLink('04-compras.md')).toBe('/manual/compras')
  })

  it('resolveChapterLink devuelve undefined para enlaces externos o anclas del mismo capítulo', () => {
    expect(resolveChapterLink('https://ejemplo.com')).toBeUndefined()
    expect(resolveChapterLink('#seccion-local')).toBeUndefined()
  })

  it('resolveChapterLink devuelve undefined si el archivo no existe en manual.json', () => {
    expect(resolveChapterLink('./99-inexistente.md')).toBeUndefined()
  })
})
