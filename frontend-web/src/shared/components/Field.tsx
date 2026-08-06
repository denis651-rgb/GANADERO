import type { PropsWithChildren, ReactNode } from 'react'

interface FieldProps {
  label: string
  error?: string
  hint?: string
  icon?: ReactNode
}

export function Field({ label, error, hint, icon, children }: PropsWithChildren<FieldProps>) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <span className={`field-control ${error ? 'invalid' : ''}`}>
        {icon && <span className="field-icon">{icon}</span>}
        {children}
      </span>
      {error ? <span className="field-error">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  )
}
