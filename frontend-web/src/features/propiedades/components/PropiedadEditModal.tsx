import { type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type { Propiedad } from '@/features/propiedades/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'

export interface PropiedadEditInput {
  nombre: string
  departamento?: string
  municipio?: string
  localidad?: string
  direccionReferencia?: string
  superficieHa?: number
  version: number
}

interface PropiedadEditModalProps {
  propiedad: Propiedad | null
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (input: PropiedadEditInput) => void
  onReload: () => void
}

export function PropiedadEditModal({ propiedad, loading, error, onClose, onSubmit, onReload }: PropiedadEditModalProps) {
  const normalized = error ? normalizeApiError(error) : null
  const conflict = normalized?.code === 'VERSION_CONFLICT'

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!propiedad || loading) return
    const data = new FormData(event.currentTarget)
    onSubmit({
      nombre: String(data.get('nombre') ?? '').trim(),
      departamento: String(data.get('departamento') ?? '').trim() || undefined,
      municipio: String(data.get('municipio') ?? '').trim() || undefined,
      localidad: String(data.get('localidad') ?? '').trim() || undefined,
      direccionReferencia: String(data.get('direccionReferencia') ?? '').trim() || undefined,
      superficieHa: Number(data.get('superficieHa')) || undefined,
      version: propiedad.version,
    })
  }

  return <Modal open={Boolean(propiedad)} title="Editar propiedad" description="Modifica los datos de la propiedad seleccionada." onClose={onClose}>
    {propiedad && <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger" title={conflict ? 'La propiedad cambió mientras la editabas' : undefined}>
        {conflict ? 'Recarga la información antes de volver a guardar.' : normalized.message}
      </Alert>}
      <div className="form-grid">
        <Field label="Código" hint="Identificador interno permanente"><input value={propiedad.codigo} readOnly /></Field>
        <Field label="Nombre" required><input name="nombre" defaultValue={propiedad.nombre} required maxLength={160} /></Field>
        <Field label="Departamento"><input name="departamento" defaultValue={propiedad.departamento ?? ''} /></Field>
        <Field label="Municipio"><input name="municipio" defaultValue={propiedad.municipio ?? ''} /></Field>
        <Field label="Localidad"><input name="localidad" defaultValue={propiedad.localidad ?? ''} /></Field>
        <Field label="Referencia de dirección"><input name="direccionReferencia" defaultValue={propiedad.direccionReferencia ?? ''} /></Field>
        <Field label="Superficie (ha)"><input name="superficieHa" type="number" inputMode="decimal" min="0" step="0.0001" defaultValue={propiedad.superficieHa ?? ''} /></Field>
      </div>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        {conflict && <Button type="button" variant="secondary" onClick={onReload}>Recargar datos</Button>}
        <Button type="submit" loading={loading} disabled={conflict}><Save size={17} aria-hidden="true" />Guardar propiedad</Button>
      </div>
    </form>}
  </Modal>
}
