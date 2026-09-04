import { type FormEvent } from 'react'
import { Save } from 'lucide-react'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'

interface SectorAddModalProps {
  open: boolean
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (form: HTMLFormElement) => void
}

export function SectorAddModal({ open, loading, error, onClose, onSubmit }: SectorAddModalProps) {
  const normalized = error ? normalizeApiError(error) : null

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (loading) return
    onSubmit(event.currentTarget)
  }

  return <Modal open={open} title="Añadir sector" description="Registra un nuevo sector dentro de la propiedad seleccionada." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger">{normalized.message}</Alert>}
      <Field label="Código" hint="Se asigna al guardar"><input value="Automático · PRP-###-SEC-###" readOnly aria-label="Código automático de sector" /></Field>
      <Field label="Nombre" required><input name="nombre" required maxLength={160} /></Field>
      <Field label="Descripción"><input name="descripcion" /></Field>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button type="submit" loading={loading}><Save size={17} aria-hidden="true" />Añadir sector</Button>
      </div>
    </form>
  </Modal>
}
