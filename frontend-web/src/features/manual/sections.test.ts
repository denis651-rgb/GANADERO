import { describe, expect, it } from 'vitest'
import { splitChapterContent } from './sections'

describe('splitChapterContent', () => {
  it('deja en la intro todo lo anterior al primer ##', () => {
    const { intro, sections } = splitChapterContent('# Sanidad\n\nBajada del capítulo.\n\n## Plan sanitario\n\nContenido.')
    expect(intro).toBe('# Sanidad\n\nBajada del capítulo.')
    expect(sections).toHaveLength(1)
  })

  it('una sección sin sub-encabezados no es plegable', () => {
    const { sections } = splitChapterContent('## Antes de comenzar\n\nDebes tener un potrero.')
    expect(sections[0]).toMatchObject({ id: 'antes-de-comenzar', collapsible: false })
  })

  it('una sección con ### anidados es plegable', () => {
    const { sections } = splitChapterContent(
      '## Plan sanitario\n\n### Para qué sirve\n\nAgrupa actividades.\n\n### Procedimiento\n\n1. Paso uno.',
    )
    expect(sections[0].collapsible).toBe(true)
  })

  it('headingIds incluye el propio id y el de cada sub-encabezado', () => {
    const { sections } = splitChapterContent('## Jornada sanitaria\n\n### Para qué sirve\n\n### Procedimiento\n')
    expect(sections[0].headingIds).toEqual(['jornada-sanitaria', 'para-que-sirve', 'procedimiento'])
  })

  it('separa varias secciones de nivel ## una de otra', () => {
    const { sections } = splitChapterContent('## Plan sanitario\n\nUno.\n\n## Jornada sanitaria\n\nDos.')
    expect(sections.map((section) => section.title)).toEqual(['Plan sanitario', 'Jornada sanitaria'])
    expect(sections[0].body.trim()).toBe('Uno.')
    expect(sections[1].body.trim()).toBe('Dos.')
  })
})
