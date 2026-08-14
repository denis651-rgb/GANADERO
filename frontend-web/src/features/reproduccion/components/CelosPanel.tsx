import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  anularCelo,
  estadoRegistroBadge,
  ESTADO_REGISTRO_LABELS,
  INTENSIDAD_CELO_LABELS,
  registrarCelo,
  TIPO_CELO_LABELS,
  toIso,
  type CeloResponse,
  type PageResponse,
} from '@/features/reproduccion/api'
import type { ReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface CelosPanelProps {
  celos: PageResponse<CeloResponse>
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

export function CelosPanel({ celos, isLoading, error, catalogs, refresh }: CelosPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const canAnular = can('REPRODUCCION_ANULAR')
  const [showForm, setShowForm] = useState(false)
  const [anulando, setAnulando] = useState<CeloResponse | null>(null)
  const [anularMotivo, setAnularMotivo] = useState('')
  const [animalId, setAnimalId] = useState('')
  const [propiedadId, setPropiedadId] = useState('')
  const [potreroId, setPotreroId] = useState('')

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarCelo({
        animalId: String(data.get('animalId')),
        fechaDeteccion: toIso(String(data.get('fechaDeteccion'))),
        tipoDeteccion: String(data.get('tipoDeteccion')) as CeloResponse['tipoDeteccion'],
        intensidad: (String(data.get('intensidad') || '') || undefined) as CeloResponse['intensidad'],
        observaciones: String(data.get('observaciones') || '') || undefined,
        propiedadId: propiedadId || undefined,
        potreroId: potreroId || undefined,
        loteId: String(data.get('loteId') || '') || undefined,
        clienteUuid: crypto.randomUUID(),
      })
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const anular = useMutation({
    mutationFn: () => anularCelo(anulando!.id, { motivo: anularMotivo, version: anulando!.version }),
    onSuccess: () => { setAnulando(null); setAnularMotivo(''); refresh() },
  })

  const seleccionarAnimal = (id: string) => {
    setAnimalId(id)
    const animal = catalogs?.hembras.find((item) => item.id === id)
    setPropiedadId(animal?.propiedadActualId ?? '')
    setPotreroId(animal?.potreroActualId ?? '')
  }

  const errorVisible = error ?? crear.error ?? anular.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Detección de celo</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar celo</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando celos…" />}
      {!isLoading && celos.content.length === 0 && <EmptyState title="No hay celos registrados" description="Registra la primera detección de celo para dar seguimiento reproductivo." />}
      {celos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Celos</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha de detección</th><th scope="col">Tipo</th><th scope="col">Intensidad</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{celos.content.map((celo) => <tr key={celo.id}>
        <td><strong>{celo.codigoAnimal}</strong>{celo.nombreAnimal ? ` · ${celo.nombreAnimal}` : ''}</td>
        <td>{new Date(celo.fechaDeteccion).toLocaleString('es-BO')}</td>
        <td className="table-secondary">{TIPO_CELO_LABELS[celo.tipoDeteccion]}</td>
        <td className="table-secondary">{celo.intensidad ? INTENSIDAD_CELO_LABELS[celo.intensidad] : '—'}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(celo.estado)}`}>{ESTADO_REGISTRO_LABELS[celo.estado]}</span></td>
        <td>{canAnular && celo.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => { setAnulando(celo); setAnularMotivo('') }}>Anular</Button>}</td>
      </tr>)}</tbody></table></div>}
      {celos.content.length > 0 && <div className="mobile-only">{celos.content.map((celo) => <div key={celo.id} className="mobile-entity-card">
        <div><strong>{celo.codigoAnimal}</strong><p className="muted">{new Date(celo.fechaDeteccion).toLocaleString('es-BO')} · {TIPO_CELO_LABELS[celo.tipoDeteccion]}</p><p className="muted">{celo.intensidad ? INTENSIDAD_CELO_LABELS[celo.intensidad] : '—'} · {ESTADO_REGISTRO_LABELS[celo.estado]}</p></div>
        {canAnular && celo.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => { setAnulando(celo); setAnularMotivo('') }}>Anular</Button>}
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar celo" onClose={() => setShowForm(false)} description="Registra la detección de celo de una hembra.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required><select name="animalId" required value={animalId} onChange={(event) => seleccionarAnimal(event.target.value)}><option value="">Selecciona…</option>{catalogs?.hembras.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Fecha y hora de detección" required><input name="fechaDeteccion" type="datetime-local" required /></Field>
        <Field label="Tipo de detección" required><select name="tipoDeteccion" required><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_CELO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Intensidad"><select name="intensidad"><option value="">Sin especificar</option>{Object.entries(INTENSIDAD_CELO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Propiedad"><select name="propiedadId" value={propiedadId} onChange={(event) => setPropiedadId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId" value={potreroId} onChange={(event) => setPotreroId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.paddocks.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote"><select name="loteId"><option value="">Sin especificar</option>{catalogs?.lots.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar celo</Button></div>
      </form>
    </Modal>

    <ConfirmDialog
      open={Boolean(anulando)}
      title="Anular celo"
      confirmLabel="Anular celo"
      variant="warning"
      loading={anular.isPending}
      error={anular.error}
      onClose={() => setAnulando(null)}
      onConfirm={() => { if (anulando && anularMotivo.trim() && !anular.isPending) anular.mutate() }}
    >
      <p className="muted">El registro de celo de {anulando ? `${anulando.codigoAnimal} · ${new Date(anulando.fechaDeteccion).toLocaleString('es-BO')}` : ''} dejará de contar para el seguimiento reproductivo.</p>
      <Field label="Motivo de anulación" required><input autoFocus value={anularMotivo} onChange={(event) => setAnularMotivo(event.target.value)} maxLength={1000} placeholder="Motivo…" /></Field>
    </ConfirmDialog>
  </div>
}
