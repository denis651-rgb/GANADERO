import { useState } from 'react'
import { Modal } from '@/shared/components/Modal'
import { Button } from '@/shared/components/Button'

interface MotivoModalProps {
  open: boolean
  title: string
  confirmLabel: string
  loading?: boolean
  error?: string
  onConfirm: (motivo: string) => void
  onClose: () => void
}

export function MotivoModal({ open, title, confirmLabel, loading, error, onConfirm, onClose }: MotivoModalProps) {
  const [motivo, setMotivo] = useState('')
  const [localError, setLocalError] = useState('')

  return (
    <Modal open={open} onClose={onClose} title={title}>
      <label className="field">
        <span className="field-label">Motivo (obligatorio)</span>
        <span className="field-control"><textarea value={motivo} onChange={(event) => { setMotivo(event.target.value); setLocalError('') }} rows={4} /></span>
        <span className="field-error">{localError || error}</span>
      </label>
      <div className="form-actions" style={{ marginTop: 16 }}>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button variant="danger" loading={loading} onClick={() => {
          if (motivo.trim().length < 3) { setLocalError('Indica un motivo con al menos 3 caracteres.'); return }
          onConfirm(motivo.trim())
        }}>{confirmLabel}</Button>
      </div>
    </Modal>
  )
}
