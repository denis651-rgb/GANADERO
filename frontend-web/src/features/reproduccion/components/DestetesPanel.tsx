import { useState } from 'react'
import { AnimalSearchSelect } from './AnimalSearchSelect'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  estadoRegistroBadge,
  ESTADO_REGISTRO_LABELS,
  registrarDestete,
  getMadreDestete,
  TIPO_DESTETE_LABELS,
  type Destete,
  type PageResponse,
} from '@/features/reproduccion/api'
import type { ReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'
import { formatDate } from '@/shared/utils/date'
import './PartosPanel.css'

interface DestetesPanelProps {
  destetes: PageResponse<Destete>
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

export function DestetesPanel({ destetes, isLoading, error, catalogs, refresh }: DestetesPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const [showForm, setShowForm] = useState(false)
  const [criaId, setCriaId] = useState('')
  const madre = useQuery({ queryKey: ['madre-destete', criaId], queryFn: () => getMadreDestete(criaId), enabled: showForm && !!criaId, retry: false })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      if (!criaId || !madre.data || madre.isFetching || madre.isError) throw new Error('No se pudo identificar la madre registrada de la cría.')
      return registrarDestete({
        animalCriaId: criaId,
        madreId: madre.data.id,
        fechaDestete: String(data.get('fechaDestete')),
        pesoDesteteKg: Number(data.get('pesoDesteteKg')),
        tipoDestete: String(data.get('tipoDestete')) as Destete['tipoDestete'],
        motivo: String(data.get('motivo') || '') || undefined,
        responsableId: String(data.get('responsableId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      })
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const errorVisible = error ?? crear.error

  return <div className="page-stack ciclo-panel">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="partos-heading">
        <h2>Destetes</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar destete</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando destetes…" />}
      {!isLoading && destetes.content.length === 0 && <EmptyState title="No hay destetes registrados" description="Registra el primer destete para controlar el peso de las crías." />}
      {destetes.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Destetes</caption><thead><tr><th scope="col">Cría</th><th scope="col">Madre</th><th scope="col">Fecha</th><th scope="col">Peso (kg)</th><th scope="col">Tipo</th><th scope="col">Estado</th></tr></thead><tbody>{destetes.content.map((destete) => <tr key={destete.id}>
        <td><strong className="parto-madre">{destete.nombreAnimal || destete.codigoAnimal || 'Sin nombre'}</strong>{destete.nombreAnimal && destete.codigoAnimal && <span className="table-secondary">{destete.codigoAnimal}</span>}</td>
        <td><strong className="parto-madre">{catalogs?.animales.find((animal) => animal.id === destete.madreId)?.nombre || 'Madre sin nombre disponible'}</strong><span className="table-secondary">{catalogs?.animales.find((animal) => animal.id === destete.madreId)?.codigo}</span></td>
        <td>{formatDate(destete.fechaDestete)}</td>
        <td>{destete.pesoDesteteKg ?? 'Sin registro'}</td>
        <td>{TIPO_DESTETE_LABELS[destete.tipoDestete]}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(destete.estado)}`}>{ESTADO_REGISTRO_LABELS[destete.estado]}</span></td>
      </tr>)}</tbody></table></div>}
      {destetes.content.length > 0 && <div className="mobile-only">{destetes.content.map((destete) => <div key={destete.id} className="mobile-entity-card">
        <div><strong className="parto-madre">{destete.nombreAnimal || destete.codigoAnimal || 'Sin nombre'}</strong>{destete.nombreAnimal && destete.codigoAnimal && <span className="table-secondary">{destete.codigoAnimal}</span>}<p className="muted">Madre: {catalogs?.animales.find((animal) => animal.id === destete.madreId)?.nombre || catalogs?.animales.find((animal) => animal.id === destete.madreId)?.codigo || 'Sin información'}</p><p className="muted">{formatDate(destete.fechaDestete)} · {TIPO_DESTETE_LABELS[destete.tipoDestete]}</p><p className="muted">{destete.pesoDesteteKg != null ? `${destete.pesoDesteteKg} kg` : 'Peso sin registro'}</p><span className={`status-badge status-badge-${estadoRegistroBadge(destete.estado)}`}>{ESTADO_REGISTRO_LABELS[destete.estado]}</span></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar destete" onClose={() => setShowForm(false)} description="Registra el destete de una cría.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalSearchSelect label="Cría" name="animalCriaId" value={criaId} onChange={setCriaId} />
        <Field label="Madre" hint="Se obtiene automáticamente del parto registrado de la cría."><input readOnly value={!criaId ? 'Selecciona primero la cría' : madre.isFetching ? 'Consultando madre…' : madre.isError ? 'No se pudo identificar la madre' : madre.data ? [madre.data.nombre, madre.data.codigo].filter(Boolean).join(' · ') : 'Sin madre registrada'} /></Field>
        {!!criaId && madre.isError && <div className="form-full"><p role="alert">{normalizeApiError(madre.error).message}</p><Button type="button" variant="ghost" onClick={() => void madre.refetch()}>Reintentar</Button></div>}
        <Field label="Fecha del destete" required><input name="fechaDestete" type="date" required /></Field>
        <Field label="Peso al destete (kg)" required><input name="pesoDesteteKg" type="number" inputMode="decimal" min="0.01" step="0.01" required /></Field>
        <Field label="Tipo de destete" required><select name="tipoDestete" required><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_DESTETE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Motivo"><input name="motivo" maxLength={500} /></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending} disabled={!criaId || !madre.data || madre.isFetching || madre.isError}>Registrar destete</Button></div>
      </form>
    </Modal>
  </div>
}
