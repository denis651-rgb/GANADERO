import { Children, isValidElement, type ButtonHTMLAttributes, type PropsWithChildren, type ReactNode } from 'react'
import { LoaderCircle } from 'lucide-react'
import { cn } from '@/shared/utils/cn'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  fullWidth?: boolean
  loading?: boolean
  loadingLabel?: string
}

function textFromNode(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node)
  if (isValidElement<{ children?: ReactNode }>(node)) return textFromNode(node.props.children)
  return Children.toArray(node).map(textFromNode).join(' ').trim()
}

export function Button({ children, variant = 'primary', fullWidth, loading, loadingLabel, className, disabled, ...props }: PropsWithChildren<ButtonProps>) {
  const accessibleLoadingLabel = loadingLabel ?? `Procesando${textFromNode(children) ? `: ${textFromNode(children)}` : ''}…`
  return (
    <button
      className={cn('button', `button-${variant}`, fullWidth && 'button-full', className)}
      {...props}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      aria-label={loading ? accessibleLoadingLabel : props['aria-label']}
    >
      {loading && <LoaderCircle className="spin" size={18} aria-hidden="true" />}
      {children}
    </button>
  )
}
