import type { PropsWithChildren } from 'react'
import { AlertCircle, AlertTriangle, CheckCircle2, Info } from 'lucide-react'

interface AlertProps {
  tone?: 'info' | 'success' | 'warning' | 'danger'
  title?: string
}

export function Alert({ tone = 'info', title, children }: PropsWithChildren<AlertProps>) {
  const Icon = tone === 'danger' ? AlertCircle : tone === 'warning' ? AlertTriangle : tone === 'success' ? CheckCircle2 : Info
  return (
    <div className={`alert alert-${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <Icon size={20} aria-hidden="true" />
      <div>{title && <strong>{title}</strong>}<span>{children}</span></div>
    </div>
  )
}
