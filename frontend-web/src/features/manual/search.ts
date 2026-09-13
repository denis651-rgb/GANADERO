import { manualChapters } from '@/features/manual/manualRegistry'
import { slugify } from '@/features/manual/slug'

export interface ManualSearchEntry {
  chapterId: string
  chapterTitle: string
  heading: string
  anchor: string
  content: string
}

export interface ManualSearchResult extends ManualSearchEntry {
  /** Ventana de texto alrededor de la coincidencia (o el inicio del contenido si solo coincidió el título/encabezado). */
  excerpt: string
}

const EXCERPT_RADIUS = 60
const MAX_RESULTS = 8

/** Minúsculas y sin tildes/diéresis, para comparar ignorando acentos — conserva la longitud del texto en español. */
export function foldText(text: string): string {
  return text.toLocaleLowerCase('es-BO').normalize('NFD').replace(/[̀-ͯ]/g, '')
}

function stripMarkdown(text: string): string {
  return text
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/[*_>#]/g, ' ')
    .replace(/^[\s-]*-\s+/gm, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Un registro por cada tramo de texto entre dos encabezados de un capítulo. */
function buildIndex(): ManualSearchEntry[] {
  const entries: ManualSearchEntry[] = []
  for (const chapter of manualChapters) {
    let heading = chapter.title
    let anchor = ''
    let buffer: string[] = []

    const flush = () => {
      const content = stripMarkdown(buffer.join('\n'))
      // Un encabezado real (con o sin texto debajo) siempre es buscable por su propio título;
      // el "encabezado" por defecto (antes del primer #) solo cuenta si de verdad trae contenido.
      const isDefaultHeading = anchor === ''
      if (content || !isDefaultHeading) {
        entries.push({ chapterId: chapter.id, chapterTitle: chapter.title, heading, anchor, content })
      }
      buffer = []
    }

    for (const line of chapter.content.split('\n')) {
      const match = /^(#{1,6})\s+(.*)$/.exec(line)
      if (match) {
        flush()
        heading = stripMarkdown(match[2])
        anchor = slugify(heading)
      } else {
        buffer.push(line)
      }
    }
    flush()
  }
  return entries
}

export const manualSearchIndex: ManualSearchEntry[] = buildIndex()

function excerptAround(content: string, matchIndex: number, matchLength: number): string {
  const start = Math.max(0, matchIndex - EXCERPT_RADIUS)
  const end = Math.min(content.length, matchIndex + matchLength + EXCERPT_RADIUS)
  return `${start > 0 ? '…' : ''}${content.slice(start, end)}${end < content.length ? '…' : ''}`
}

/**
 * Búsqueda local sin backend: ignora mayúsculas/tildes, busca en título de capítulo,
 * encabezado y contenido. Prioriza coincidencias de título/encabezado sobre las de solo
 * contenido, y corta en `limit` — para un manual de este tamaño no hace falta más.
 */
export function searchManual(query: string, limit = MAX_RESULTS): ManualSearchResult[] {
  const q = foldText(query.trim())
  if (q.length < 2) return []

  const titleOrHeadingHits: ManualSearchResult[] = []
  const contentOnlyHits: ManualSearchResult[] = []

  for (const entry of manualSearchIndex) {
    const titleHit = foldText(entry.chapterTitle).includes(q)
    const headingHit = foldText(entry.heading).includes(q)
    const contentIndex = foldText(entry.content).indexOf(q)

    if (!titleHit && !headingHit && contentIndex === -1) continue

    const excerpt = contentIndex >= 0
      ? excerptAround(entry.content, contentIndex, query.trim().length)
      : excerptAround(entry.content, 0, 0)
    const result: ManualSearchResult = { ...entry, excerpt }
    if (titleHit || headingHit) titleOrHeadingHits.push(result)
    else contentOnlyHits.push(result)
  }

  return [...titleOrHeadingHits, ...contentOnlyHits].slice(0, limit)
}
