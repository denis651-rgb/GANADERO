import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  estadoRegistroBadge,
  ESTADO_REGISTRO_LABELS,
  registrarDestete,
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

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarDestete({
        animalCriaId: String(data.get('animalCriaId')),
        madreId: String(data.get('madreId')),
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

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Destetes</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar destete</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando destetes…" />}
      {!isLoading && destetes.content.length === 0 && <EmptyState title="No hay destetes registrados" description="Registra el primer destete para controlar el peso de las crías." />}
      {destetes.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Destetes</caption><thead><tr><th scope="col">Cría</th><th scope="col">Madre</th><th scope="col">Fecha</th><th scope="col">Peso (kg)</th><th scope="col">Tipo</th><th scope="col">Estado</th></tr></thead><tbody>{destetes.content.map((destete) => <tr key={destete.id}>
        <td><strong>{destete.codigoAnimal}</strong>{destete.nombreAnimal ? ` · ${destete.nombreAnimal}` : ''}</td>
        <td className="table-secondary">{catalogs?.animalLabel(destete.madreId)}</td>
        <td>{new Date(destete.fechaDestete).toLocaleDateString('es-BO')}</td>
        <td className="table-secondary">{destete.pesoDesteteKg}</td>
        <td className="table-secondary">{TIPO_DESTETE_LABELS[destete.tipoDestete]}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(destete.estado)}`}>{ESTADO_REGISTRO_LABELS[destete.estado]}</span></td>
      </tr>)}</tbody></table></div>}
      {destetes.content.length > 0 && <div className="mobile-only">{destetes.content.map((destete) => <div key={destete.id} className="mobile-entity-card">
        <div><strong>{destete.codigoAnimal}</strong><p className="muted">{new Date(destete.fechaDestete).toLocaleDateString('es-BO')} · {TIPO_DESTETE_LABELS[destete.tipoDestete]}</p><p className="muted">{destete.pesoDesteteKg} kg · {ESTADO_REGISTRO_LABELS[destete.estado]}</p></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar destete" onClose={() => setShowForm(false)} description="Registra el destete de una cría.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Cría" required><select name="animalCriaId" required><option value="">Selecciona…</option>{catalogs?.animales.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Madre" required><select name="madreId" required><option value="">Selecciona…</option>{catalogs?.hembras.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Fecha del destete" required><input name="fechaDestete" type="date" required /></Field>
        <Field label="Peso al destete (kg)" required><input name="pesoDesteteKg" type="number" inputMode="decimal" min="0.01" step="0.01" required /></Field>
        <Field label="Tipo de destete" required><select name="tipoDestete" required><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_DESTETE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Motivo"><input name="motivo" maxLength={500} /></Field>
        <Field label="Responsable"><select name="responsableId"><option value="">Sin responsable</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar destete</Button></div>
      </form>
    </Modal>
  </div>
}
