import { type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type { Sector } from '@/features/propiedades/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'

interface SectorEditModalProps {
  sector: Sector | null
  online: boolean
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (input: { codigo: string; nombre: string; descripcion?: string; activo: boolean; version: number }) => void
  onReload: () => void
}

export function SectorEditModal({ sector, online, loading, error, onClose, onSubmit, onReload }: SectorEditModalProps) {
  const normalized = error ? normalizeApiError(error) : null
  const conflict = normalized?.code === 'VERSION_CONFLICT'

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!sector || !online || loading) return
    const data = new FormData(event.currentTarget)
    onSubmit({
      codigo: String(data.get('codigo') ?? '').trim(),
      nombre: String(data.get('nombre') ?? '').trim(),
      descripcion: String(data.get('descripcion') ?? '').trim() || undefined,
      activo: sector.activo,
      version: sector.version,
    })
  }

  return <Modal open={Boolean(sector)} title="Editar sector" description="Modifica los datos del sector seleccionado." onClose={onClose}>
    {sector && <form className="page-stack" onSubmit={submit}>
      {!online && <Alert tone="info">Necesitas conexión para editar este sector.</Alert>}
      {normalized && <Alert tone="danger" title={conflict ? 'El sector cambió mientras lo editabas' : undefined}>
        {conflict ? 'Recarga la información antes de volver a guardar.' : normalized.message}
      </Alert>}
      <Field label="Código" required disabled={!online}><input name="codigo" defaultValue={sector.codigo} required maxLength={60} disabled={!online} /></Field>
      <Field label="Nombre" required disabled={!online}><input name="nombre" defaultValue={sector.nombre} required maxLength={160} disabled={!online} /></Field>
      <Field label="Descripción" disabled={!online}><textarea name="descripcion" defaultValue={sector.descripcion ?? ''} rows={3} disabled={!online} /></Field>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        {conflict && <Button type="button" variant="secondary" onClick={onReload}>Recargar datos</Button>}
        <Button type="submit" loading={loading} disabled={!online || conflict}><Save size={17} aria-hidden="true" />Guardar sector</Button>
      </div>
    </form>}
  </Modal>
}
