import type { AnchorHTMLAttributes, ReactNode } from 'react'
import { Link } from 'react-router'
import { resolveChapterLink } from '@/features/manual/manualRegistry'

interface ManualLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  children?: ReactNode
}

const EXTERNAL_HREF = /^(https?:)?\/\//i
const MAIL_HREF = /^mailto:/i

/**
 * Enlace del contenido Markdown. Un enlace a otro capítulo (`./08-reproduccion.md#ancla`)
 * navega dentro de la app; uno externo se abre en pestaña aparte; el resto (anclas del
 * mismo capítulo, etc.) se deja como `<a>` normal.
 */
export function ManualLink({ href, children, ...rest }: ManualLinkProps) {
  const chapterPath = href ? resolveChapterLink(href) : undefined
  if (chapterPath) {
    return <Link to={chapterPath}>{children}</Link>
  }
  const isExternal = Boolean(href) && (EXTERNAL_HREF.test(href!) || MAIL_HREF.test(href!))
  return (
    <a href={href} {...rest} target={isExternal ? '_blank' : undefined} rel={isExternal ? 'noreferrer noopener' : undefined}>
      {children}
    </a>
  )
}
