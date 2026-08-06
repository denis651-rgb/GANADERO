import type { HTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '@/shared/utils/cn'

export function Card({ children, className, ...props }: PropsWithChildren<HTMLAttributes<HTMLDivElement>>) {
  return <section className={cn('card', className)} {...props}>{children}</section>
}
