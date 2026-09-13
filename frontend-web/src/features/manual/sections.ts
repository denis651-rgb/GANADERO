import { slugify } from '@/features/manual/slug'

export interface ManualSection {
  id: string
  title: string
  /** Se pliega en un acordeón solo si agrupa sub-encabezados (patrón "forma B" de CONVENCIONES.md). */
  collapsible: boolean
  /** ids de este encabezado y de todos los anidados debajo, para saber si un hash de la URL cae dentro. */
  headingIds: string[]
  /** Markdown de la sección sin la línea `## Título` (ese título se renderiza aparte). */
  body: string
}

export interface ManualChapterSplit {
  /** Markdown antes del primer `##` (título del capítulo y bajada, si las hay). */
  intro: string
  sections: ManualSection[]
}

const H2_RE = /^##\s+(.+?)\s*$/
const NESTED_HEADING_RE = /^#{3,6}\s+(.+?)\s*$/gm

/** Separa un capítulo en su intro y sus secciones de nivel `##`, para poder plegar cada una por separado. */
export function splitChapterContent(markdown: string): ManualChapterSplit {
  const introLines: string[] = []
  const sections: { title: string; lines: string[] }[] = []

  for (const line of markdown.split('\n')) {
    const match = H2_RE.exec(line)
    if (match) {
      sections.push({ title: match[1], lines: [] })
      continue
    }
    if (sections.length > 0) sections[sections.length - 1].lines.push(line)
    else introLines.push(line)
  }

  return {
    intro: introLines.join('\n').trim(),
    sections: sections.map(({ title, lines }) => {
      const body = lines.join('\n')
      const nestedIds = [...body.matchAll(NESTED_HEADING_RE)].map((match) => slugify(match[1]))
      const id = slugify(title)
      return { id, title, collapsible: nestedIds.length > 0, headingIds: [id, ...nestedIds], body }
    }),
  }
}
