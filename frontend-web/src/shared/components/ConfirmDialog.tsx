import type { ReactNode } from 'react'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface ConfirmDialogProps {
  open: boolean
  title: string
  confirmLabel: string
  onConfirm: () => void
  onClose: () => void
  cancelLabel?: string
  description?: ReactNode
  variant?: 'default' | 'warning' | 'danger'
  tone?: 'default' | 'danger'
  confirmIcon?: ReactNode
  loading?: boolean
  disabled?: boolean
  error?: unknown
  children: ReactNode
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  onConfirm,
  onClose,
  cancelLabel = 'Cancelar',
  variant,
  tone = 'danger',
  confirmIcon,
  loading = false,
  disabled = false,
  error,
  children,
}: ConfirmDialogProps) {
  const resolvedVariant = variant ?? tone
  return (
    <Modal open={open} title={title} onClose={() => { if (!loading) onClose() }}>
      <div className="page-stack">
        {description && <p className="muted">{description}</p>}
        {children}
        {Boolean(error) && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
        <div className="form-actions">
          <Button variant="ghost" onClick={onClose} disabled={loading}>{cancelLabel}</Button>
          <Button variant={resolvedVariant === 'danger' ? 'danger' : 'primary'} loading={loading} disabled={disabled || loading} onClick={onConfirm}>{confirmIcon}{confirmLabel}</Button>
        </div>
      </div>
    </Modal>
  )
}
