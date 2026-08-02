import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'
import { LoaderCircle } from 'lucide-react'
import { cn } from '@/shared/utils/cn'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  fullWidth?: boolean
  loading?: boolean
}

export function Button({ children, variant = 'primary', fullWidth, loading, className, disabled, ...props }: PropsWithChildren<ButtonProps>) {
  return (
    <button
      className={cn('button', `button-${variant}`, fullWidth && 'button-full', className)}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <LoaderCircle className="spin" size={18} />}
      {children}
    </button>
  )
}
