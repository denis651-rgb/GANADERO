import { Children, isValidElement, type PropsWithChildren, type ReactNode } from 'react'
import { FloatingNotice } from '@/shared/toast/FloatingNotice'

interface AlertProps {
  tone?: 'info' | 'success' | 'warning' | 'danger'
  title?: string
}

function messageText(node: ReactNode): string {
  return Children.toArray(node).map((child) => isValidElement<{ children?: ReactNode }>(child)
    ? messageText(child.props.children) : String(child)).join('')
}

export function Alert({ tone = 'info', title, children }: PropsWithChildren<AlertProps>) {
  return <FloatingNotice key={`${tone}:${title ?? ''}:${messageText(children)}`} tone={tone}>
    {title && <strong>{title}</strong>}<div>{children}</div>
  </FloatingNotice>
}
