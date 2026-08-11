import { Children, cloneElement, isValidElement, useId, type AriaAttributes, type ReactNode } from 'react'

interface FieldProps {
  label: string
  error?: string
  hint?: string
  icon?: ReactNode
  id?: string
  required?: boolean
  disabled?: boolean
  children: ReactNode
}

interface AccessibleControlProps extends AriaAttributes {
  id?: string
  required?: boolean
  disabled?: boolean
}

export function Field({ label, error, hint, icon, id, required, disabled, children }: FieldProps) {
  const generatedId = useId()
  const childArray = Children.toArray(children)
  const formControlIndex = childArray.findIndex((child) => isValidElement<AccessibleControlProps>(child)
    && typeof child.type === 'string' && ['input', 'select', 'textarea'].includes(child.type))
  const formControl = formControlIndex >= 0 ? childArray[formControlIndex] : undefined
  const childId = isValidElement<AccessibleControlProps>(formControl) ? formControl.props.id : undefined
  const controlId = id ?? childId ?? `field-${generatedId}`
  const hintId = `${controlId}-hint`
  const errorId = `${controlId}-error`
  const existingDescription = isValidElement<AccessibleControlProps>(formControl) ? formControl.props['aria-describedby'] : undefined
  const describedBy = [existingDescription, hint ? hintId : undefined, error ? errorId : undefined].filter(Boolean).join(' ') || undefined
  const control = Children.map(children, (child, index) => {
    if (index !== formControlIndex || !isValidElement<AccessibleControlProps>(child)) return child
    return cloneElement(child, {
        id: controlId,
        required: required ?? child.props.required,
        disabled: disabled ?? child.props.disabled,
        'aria-invalid': error ? true : child.props['aria-invalid'],
        'aria-describedby': describedBy,
      })
  })

  return (
    <div className="field">
      <label className="field-label" htmlFor={controlId}>{label}{required ? <span aria-hidden="true"> *</span> : null}</label>
      <span className={`field-control ${error ? 'invalid' : ''}`}>
        {icon && <span className="field-icon" aria-hidden="true">{icon}</span>}
        {control}
      </span>
      {hint && <span id={hintId} className="field-hint">{hint}</span>}
      {error && <span id={errorId} className="field-error">{error}</span>}
    </div>
  )
}
