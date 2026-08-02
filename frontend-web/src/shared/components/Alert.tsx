import type { PropsWithChildren } from 'react'
import { AlertCircle, CheckCircle2, Info } from 'lucide-react'

interface AlertProps {
  tone?: 'info' | 'success' | 'danger'
  title?: string
}

export function Alert({ tone = 'info', title, children }: PropsWithChildren<AlertProps>) {
  const Icon = tone === 'danger' ? AlertCircle : tone === 'success' ? CheckCircle2 : Info
  return (
    <div className={`alert alert-${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <Icon size={20} />
      <div>{title && <strong>{title}</strong>}<span>{children}</span></div>
    </div>
  )
}
