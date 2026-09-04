import { type FormEvent } from 'react'
import { Save } from 'lucide-react'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'

interface PropiedadFormModalProps {
  open: boolean
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (form: HTMLFormElement) => void
}

export function PropiedadFormModal({ open, loading, error, onClose, onSubmit }: PropiedadFormModalProps) {
  const normalized = error ? normalizeApiError(error) : null

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (loading) return
    onSubmit(event.currentTarget)
  }

  return <Modal open={open} title="Nueva propiedad" description="Registra un nuevo establecimiento de la operación." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger">{normalized.message}</Alert>}
      <div className="form-grid">
        <Field label="Código" hint="Se asigna al guardar"><input value="Automático · PRP-###" readOnly aria-label="Código automático de propiedad" /></Field>
        <Field label="Nombre" required><input name="nombre" required maxLength={160} /></Field>
        <Field label="Departamento"><input name="departamento" /></Field>
        <Field label="Municipio"><input name="municipio" /></Field>
        <Field label="Superficie (ha)"><input name="superficieHa" type="number" inputMode="decimal" min="0" step="0.0001" /></Field>
      </div>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button type="submit" loading={loading}><Save size={17} aria-hidden="true" />Crear propiedad</Button>
      </div>
    </form>
  </Modal>
}
