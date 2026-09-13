import { useMemo } from 'react'
import { useLocation } from 'react-router'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { ManualChapter } from '@/features/manual/manualRegistry'
import { splitChapterContent } from '@/features/manual/sections'
import { ManualHeading } from '@/features/manual/components/ManualHeading'
import { ManualImage } from '@/features/manual/components/ManualImage'
import { ManualLink } from '@/features/manual/components/ManualLink'
import { ManualNote } from '@/features/manual/components/ManualNote'
import { ManualSection } from '@/features/manual/components/ManualSection'
import { ManualTable } from '@/features/manual/components/ManualTable'

interface ManualViewerProps {
  chapter: ManualChapter
}

/**
 * Componentes seguros para el contenido Markdown: nada de `dangerouslySetInnerHTML`
 * ni HTML crudo del autor, solo estos elementos controlados.
 */
function buildComponents(chapterId: string): Components {
  const heading = (level: 1 | 2 | 3 | 4 | 5 | 6) =>
    function Heading({ children }: { children?: React.ReactNode }) {
      return <ManualHeading level={level} chapterId={chapterId}>{children}</ManualHeading>
    }
  return {
    h1: heading(1),
    h2: heading(2),
    h3: heading(3),
    h4: heading(4),
    h5: heading(5),
    h6: heading(6),
    table: ({ children }) => <ManualTable>{children}</ManualTable>,
    img: ({ src, alt }) => <ManualImage src={typeof src === 'string' ? src : undefined} alt={alt} />,
    a: ({ href, children, ...rest }) => <ManualLink href={href} {...rest}>{children}</ManualLink>,
    blockquote: ({ children }) => <ManualNote>{children}</ManualNote>,
  }
}

export function ManualViewer({ chapter }: ManualViewerProps) {
  const components = useMemo(() => buildComponents(chapter.id), [chapter.id])
  const { intro, sections } = useMemo(() => splitChapterContent(chapter.content), [chapter.content])
  const { hash } = useLocation()
  const hashId = hash ? decodeURIComponent(hash.slice(1)) : undefined

  return (
    <article className="manual-chapter">
      {intro && <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>{intro}</ReactMarkdown>}
      {sections.map((section) => section.collapsible
        ? <ManualSection
            key={section.id}
            section={section}
            chapterId={chapter.id}
            components={components}
            forceOpen={hashId ? section.headingIds.includes(hashId) : false}
          />
        : <ReactMarkdown key={section.id} remarkPlugins={[remarkGfm]} components={components}>
            {`## ${section.title}\n${section.body}`}
          </ReactMarkdown>)}
    </article>
  )
}
