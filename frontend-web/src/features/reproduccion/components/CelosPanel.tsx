import { useState } from 'react'
import { AnimalSearchSelect } from './AnimalSearchSelect'
import type { AnimalSummary } from '@/features/animales/types'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  anularCelo,
  listCelos,
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
import './CelosPanel.css'

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
  const [fecha, setFecha] = useState('')
  const historial = useQuery({
    queryKey: ['celos-validar-intervalo', animalId], enabled: showForm && !!animalId,
    queryFn: async () => {
      const todos: CeloResponse[] = []
      let page = 0
      while (true) {
        const result = await listCelos({ animalId, estado: 'ACTIVO', page, size: 100 })
        todos.push(...result.content)
        if (++page >= result.totalPages) return todos
      }
    },
  })
  const cercanos = (historial.data ?? []).filter((celo) => Math.abs(Date.parse(celo.fechaDeteccion) - Date.parse(fecha)) < 18 * 86400000)
    .sort((a, b) => Math.abs(Date.parse(a.fechaDeteccion) - Date.parse(fecha)) - Math.abs(Date.parse(b.fechaDeteccion) - Date.parse(fecha)))
  const cercano = cercanos[0]
  const duplicado = cercano && Math.abs(Date.parse(cercano.fechaDeteccion) - Date.parse(fecha)) < 86400000

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarCelo({
        confirmarIntervaloCorto: data.get('confirmarIntervaloCorto') === 'on',
        justificacion: String(data.get('justificacion') || '') || undefined,
        agregarObservacionCeloId: data.get('accionCelo') === 'observacion' ? cercano?.id : undefined,
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
    onSuccess: () => { setShowForm(false); void historial.refetch(); refresh() },
  })

  const anular = useMutation({
    mutationFn: () => anularCelo(anulando!.id, { motivo: anularMotivo, version: anulando!.version }),
    onSuccess: () => { setAnulando(null); setAnularMotivo(''); refresh() },
  })

  const seleccionarAnimal = (id: string, seleccionado?: AnimalSummary) => {
    setAnimalId(id)
    const animal = seleccionado ?? catalogs?.hembras.find((item) => item.id === id)
    setPropiedadId(animal?.propiedadActualId ?? '')
    setPotreroId(animal?.potreroActualId ?? '')
  }

  const errorVisible = error ?? crear.error

  return <div className="page-stack celos-panel">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="celos-heading">
        <h2>Detección de celo</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar celo</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando celos…" />}
      {!isLoading && celos.content.length === 0 && <EmptyState title="No hay celos registrados" description="Registra la primera detección de celo para dar seguimiento reproductivo." />}
      {celos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Celos</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha de detección</th><th scope="col">Tipo</th><th scope="col">Intensidad</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{celos.content.map((celo) => <tr key={celo.id}>
        <td><strong className="celo-animal">{celo.nombreAnimal || celo.codigoAnimal}</strong>{celo.nombreAnimal && <span className="table-secondary">{celo.codigoAnimal}</span>}</td>
        <td><time dateTime={celo.fechaDeteccion}>{new Date(celo.fechaDeteccion).toLocaleDateString('es-BO', { timeZone: 'America/La_Paz' })}<span className="table-secondary">{new Date(celo.fechaDeteccion).toLocaleTimeString('es-BO', { timeZone: 'America/La_Paz', hour: '2-digit', minute: '2-digit' })}</span></time></td>
        <td>{TIPO_CELO_LABELS[celo.tipoDeteccion]}</td>
        <td>{celo.intensidad ? INTENSIDAD_CELO_LABELS[celo.intensidad] : 'Sin especificar'}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(celo.estado)}`}>{ESTADO_REGISTRO_LABELS[celo.estado]}</span></td>
        <td>{canAnular && celo.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => { setAnulando(celo); setAnularMotivo('') }}>Anular</Button>}</td>
      </tr>)}</tbody></table></div>}
      {celos.content.length > 0 && <div className="mobile-only">{celos.content.map((celo) => <div key={celo.id} className="mobile-entity-card">
        <div><strong className="celo-animal">{celo.nombreAnimal || celo.codigoAnimal}</strong>{celo.nombreAnimal && <span className="table-secondary">{celo.codigoAnimal}</span>}<p className="muted">{new Date(celo.fechaDeteccion).toLocaleString('es-BO', { timeZone: 'America/La_Paz' })} · {TIPO_CELO_LABELS[celo.tipoDeteccion]}</p><p>Intensidad: {celo.intensidad ? INTENSIDAD_CELO_LABELS[celo.intensidad] : 'Sin especificar'}</p><span className={`status-badge status-badge-${estadoRegistroBadge(celo.estado)}`}>{ESTADO_REGISTRO_LABELS[celo.estado]}</span></div>
        {canAnular && celo.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => { setAnulando(celo); setAnularMotivo('') }}>Anular</Button>}
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar celo" onClose={() => setShowForm(false)} description="Registra la detección de celo de una hembra.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalSearchSelect label="Animal" name="animalId" sexo="HEMBRA" value={animalId} onChange={seleccionarAnimal} />
        <Field label="Fecha y hora de detección" required><input name="fechaDeteccion" type="datetime-local" required value={fecha} onChange={(event) => setFecha(event.target.value)} /></Field>
        {cercano && <div className="form-full" key={animalId + fecha + cercano.id} style={{ background: '#fff5dd', padding: 16, borderRadius: 8 }}>
          <p>{duplicado ? 'Posible duplicado: existe un celo a menos de 24 horas.' : 'Intervalo corto: existe un celo a menos de 18 días.'} Detección registrada: {new Date(cercano.fechaDeteccion).toLocaleString('es-BO')}.</p>
          {cercano.observaciones && <p>Observaciones existentes: {cercano.observaciones}</p>}
          {duplicado && <Field label="Cómo deseas continuar"><select name="accionCelo" defaultValue="observacion">
            <option value="observacion">Agregar observación al celo existente (sin crear otro)</option>
            <option value="nuevo">Registrar un celo separado con justificación</option>
          </select></Field>}
          <Field label="Justificación para un registro separado" hint={duplicado ? 'Obligatoria si eliges registrar un celo separado.' : 'Opcional; quedará guardada con el registro.'}><input name="justificacion" maxLength={500} /></Field>
          <label><input type="checkbox" name="confirmarIntervaloCorto" /> He revisado el celo existente y confirmo el intervalo si registro uno separado.</label>
          <p>Para agregar una observación, escríbela en el campo Observaciones de abajo.</p>
        </div>}
        {animalId && historial.isError && <p className="form-full" role="alert">No se pudo verificar el historial. Cierra y vuelve a abrir el formulario para reintentar.</p>}
        <Field label="Tipo de detección" required><select name="tipoDeteccion" required><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_CELO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Intensidad"><select name="intensidad"><option value="">Sin especificar</option>{Object.entries(INTENSIDAD_CELO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Propiedad"><select name="propiedadId" value={propiedadId} onChange={(event) => setPropiedadId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId" value={potreroId} onChange={(event) => setPotreroId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.paddocks.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote"><select name="loteId"><option value="">Sin especificar</option>{catalogs?.lots.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending} disabled={!!animalId && (historial.isFetching || historial.isError)}>Guardar</Button></div>
      </form>
    </Modal>

    <ConfirmDialog
      open={Boolean(anulando)}
      title="Anular celo"
      confirmLabel="Anular celo"
      variant="warning"
      loading={anular.isPending}
      error={anular.error}
      onClose={() => { setAnulando(null); anular.reset() }}
      onConfirm={() => { if (anulando && anularMotivo.trim() && !anular.isPending) anular.mutate() }}
    >
      <p className="muted">El registro de celo de {anulando ? `${anulando.codigoAnimal} · ${new Date(anulando.fechaDeteccion).toLocaleString('es-BO')}` : ''} dejará de contar para el seguimiento reproductivo.</p>
      <Field label="Motivo de anulación" required><input autoFocus value={anularMotivo} onChange={(event) => setAnularMotivo(event.target.value)} maxLength={1000} placeholder="Motivo…" /></Field>
    </ConfirmDialog>
  </div>
}
