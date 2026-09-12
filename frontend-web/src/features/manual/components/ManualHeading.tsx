import { Children, isValidElement, type ReactNode } from 'react'
import { slugify } from '@/features/manual/slug'
import { ManualAnchorButton } from '@/features/manual/components/ManualAnchorButton'

function textContent(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node)
  if (isValidElement<{ children?: ReactNode }>(node)) return textContent(node.props.children)
  return Children.toArray(node).map(textContent).join('')
}

type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6

interface ManualHeadingProps {
  level: HeadingLevel
  chapterId: string
  children: ReactNode
}

/** h1-h6 del contenido Markdown: id estable + botón para copiar el enlace directo a esta sección. */
export function ManualHeading({ level, chapterId, children }: ManualHeadingProps) {
  const Tag = `h${level}` as const
  const id = slugify(textContent(children))
  const path = `/manual/${chapterId}#${id}`

  return (
    <Tag id={id} className="manual-heading">
      {children}
      <ManualAnchorButton path={path} />
    </Tag>
  )
}
