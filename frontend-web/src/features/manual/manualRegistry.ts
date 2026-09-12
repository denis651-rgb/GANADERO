import manualMeta from '../../manual/manual.json'

export interface ManualChapterMeta {
  id: string
  title: string
  file: string
}

export interface ManualChapter extends ManualChapterMeta {
  content: string
}

export interface ManualMeta {
  title: string
  version: string
  systemVersion: string
  updatedAt: string
  chapters: ManualChapterMeta[]
}

/**
 * Carga los .md en tiempo de compilación (eager) para que el manual funcione
 * sin conexión a Internet, empaquetado junto con la app de escritorio.
 */
const rawChapterFiles = import.meta.glob('../../manual/chapters/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>

function contentForFile(file: string): string {
  const match = Object.entries(rawChapterFiles).find(([path]) => path.endsWith(`/${file}`))
  if (!match) {
    throw new Error(`No se encontró "${file}" en src/manual/chapters/. Revisa manual.json.`)
  }
  return match[1]
}

export const manual: ManualMeta = manualMeta

export const manualChapters: ManualChapter[] = manual.chapters.map((chapter) => ({
  ...chapter,
  content: contentForFile(chapter.file),
}))

export function getManualChapter(chapterId: string | undefined): ManualChapter | undefined {
  if (!chapterId) return manualChapters[0]
  return manualChapters.find((chapter) => chapter.id === chapterId)
}

export function getAdjacentChapters(chapterId: string): {
  previous: ManualChapter | undefined
  next: ManualChapter | undefined
} {
  const index = manualChapters.findIndex((chapter) => chapter.id === chapterId)
  if (index < 0) return { previous: undefined, next: undefined }
  return {
    previous: index > 0 ? manualChapters[index - 1] : undefined,
    next: index < manualChapters.length - 1 ? manualChapters[index + 1] : undefined,
  }
}

/**
 * Resuelve un enlace Markdown entre capítulos, p. ej. `./08-reproduccion.md#registrar-parto`
 * o `08-reproduccion.md`, a su ruta dentro de la app (`/manual/reproduccion#registrar-parto`).
 * Devuelve `undefined` si el href no apunta a un archivo de `chapters/` (enlace externo, ancla
 * dentro del mismo capítulo, etc.) — en ese caso el enlace se renderiza tal cual.
 */
export function resolveChapterLink(href: string): string | undefined {
  const [filePart, hash] = href.split('#')
  const fileName = filePart.split('/').pop()
  if (!fileName || !fileName.endsWith('.md')) return undefined
  const chapter = manual.chapters.find((item) => item.file === fileName)
  if (!chapter) return undefined
  return `/manual/${chapter.id}${hash ? `#${hash}` : ''}`
}
