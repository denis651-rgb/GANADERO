import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  estadoRegistroBadge,
  ESTADO_REGISTRO_LABELS,
  registrarAborto,
  type Aborto,
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

interface AbortosPanelProps {
  abortos: PageResponse<Aborto>
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

export function AbortosPanel({ abortos, isLoading, error, catalogs, refresh }: AbortosPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const [showForm, setShowForm] = useState(false)

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarAborto({
        animalId: String(data.get('animalId')),
        fechaEvento: String(data.get('fechaEvento')),
        edadGestacionalEstimada: Number(data.get('edadGestacionalEstimada')) || undefined,
        causa: String(data.get('causa') || '') || undefined,
        diagnostico: String(data.get('diagnostico') || '') || undefined,
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
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
        <h2>Abortos</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar aborto</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando abortos…" />}
      {!isLoading && abortos.content.length === 0 && <EmptyState title="No hay abortos registrados" description="Registra eventos de aborto para el seguimiento reproductivo." />}
      {abortos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Abortos</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha</th><th scope="col">Edad gestacional</th><th scope="col">Causa</th><th scope="col">Estado</th></tr></thead><tbody>{abortos.content.map((aborto) => <tr key={aborto.id}>
        <td><strong>{aborto.codigoAnimal}</strong>{aborto.nombreAnimal ? ` · ${aborto.nombreAnimal}` : ''}</td>
        <td>{new Date(aborto.fechaEvento).toLocaleDateString('es-BO')}</td>
        <td className="table-secondary">{aborto.edadGestacionalEstimada ? `${aborto.edadGestacionalEstimada} días` : '—'}</td>
        <td className="table-secondary">{aborto.causa ?? '—'}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(aborto.estado)}`}>{ESTADO_REGISTRO_LABELS[aborto.estado]}</span></td>
      </tr>)}</tbody></table></div>}
      {abortos.content.length > 0 && <div className="mobile-only">{abortos.content.map((aborto) => <div key={aborto.id} className="mobile-entity-card">
        <div><strong>{aborto.codigoAnimal}</strong><p className="muted">{new Date(aborto.fechaEvento).toLocaleDateString('es-BO')} · {ESTADO_REGISTRO_LABELS[aborto.estado]}</p><p className="muted">{aborto.edadGestacionalEstimada ? `${aborto.edadGestacionalEstimada} días · ` : ''}{aborto.causa ?? 'Sin causa registrada'}</p></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar aborto" onClose={() => setShowForm(false)} description="Registra un evento de aborto y sus posibles causas.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required><select name="animalId" required><option value="">Selecciona…</option>{catalogs?.hembras.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Fecha del evento" required><input name="fechaEvento" type="date" required /></Field>
        <Field label="Edad gestacional estimada (días)"><input name="edadGestacionalEstimada" type="number" inputMode="numeric" min="0" max="400" /></Field>
        <Field label="Causa"><input name="causa" maxLength={300} /></Field>
        <Field label="Diagnóstico"><input name="diagnostico" maxLength={1000} /></Field>
        <Field label="Veterinario"><select name="veterinarioId"><option value="">Sin veterinario</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar aborto</Button></div>
      </form>
    </Modal>
  </div>
}
