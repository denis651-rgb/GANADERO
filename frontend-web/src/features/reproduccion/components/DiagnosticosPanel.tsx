import { useState } from 'react'
import { AnimalSearchSelect } from './AnimalSearchSelect'
import type { AnimalSummary } from '@/features/animales/types'
import { useMutation, useQuery } from '@tanstack/react-query'
import { getLote } from '@/features/lotes/api'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  METODO_DIAGNOSTICO_LABELS,
  registrarDiagnostico,
  RESULTADO_GESTACION_LABELS,
  toIso,
  type DiagnosticoGestacionResponse,
  type PageResponse,
  type ServicioResponse,
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
import './DiagnosticosPanel.css'

interface DiagnosticosPanelProps {
  diagnosticos: PageResponse<DiagnosticoGestacionResponse>
  servicios: ServicioResponse[]
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

export function DiagnosticosPanel({ diagnosticos, servicios, isLoading, error, catalogs, refresh }: DiagnosticosPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const [showForm, setShowForm] = useState(false)
  const [animalId, setAnimalId] = useState('')
  const [resultado, setResultado] = useState('')
  const [propiedadId, setPropiedadId] = useState('')
  const [potreroId, setPotreroId] = useState('')
  const [loteId, setLoteId] = useState('')
  const loteCatalogo = catalogs?.lots.find((lote) => lote.id === loteId)
  const loteActual = useQuery({
    queryKey: ['lote', loteId], queryFn: () => getLote(loteId),
    enabled: showForm && !!loteId && !loteCatalogo,
  })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarDiagnostico({
        animalId: String(data.get('animalId')),
        servicioId: String(data.get('servicioId') || '') || undefined,
        fechaDiagnostico: toIso(String(data.get('fechaDiagnostico'))),
        resultado: String(data.get('resultado')) as DiagnosticoGestacionResponse['resultado'],
        metodo: (String(data.get('metodo') || '') || undefined) as DiagnosticoGestacionResponse['metodo'],
        diasGestacionEstimados: Number(data.get('diasGestacionEstimados')) || undefined,
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        propiedadId: propiedadId || undefined,
        potreroId: potreroId || undefined,
        loteId: loteId || undefined,
        clienteUuid: crypto.randomUUID(),
      })
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const seleccionarAnimal = (id: string, seleccionado?: AnimalSummary) => {
    setAnimalId(id)
    const animal = seleccionado ?? catalogs?.hembras.find((item) => item.id === id)
    setPropiedadId(animal?.propiedadActualId ?? '')
    setPotreroId(animal?.potreroActualId ?? '')
    setLoteId(animal?.loteActualId ?? '')
  }

  const errorVisible = error ?? crear.error

  return <div className="page-stack diagnosticos-panel">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="diagnosticos-heading">
        <h2>Diagnósticos de gestación</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar diagnóstico</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando diagnósticos…" />}
      {!isLoading && diagnosticos.content.length === 0 && <EmptyState title="No hay diagnósticos registrados" description="Registra el primer diagnóstico de gestación." />}
      {diagnosticos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Diagnósticos de gestación</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha</th><th scope="col">Resultado</th><th scope="col">Método</th><th scope="col">Parto probable</th></tr></thead><tbody>{diagnosticos.content.map((diagnostico) => <tr key={diagnostico.id}>
        <td><strong className="diagnostico-animal">{diagnostico.nombreAnimal || diagnostico.codigoAnimal}</strong>{diagnostico.nombreAnimal && <span className="table-secondary">{diagnostico.codigoAnimal}</span>}</td>
        <td><time dateTime={diagnostico.fechaDiagnostico}>{new Date(diagnostico.fechaDiagnostico).toLocaleDateString('es-BO', { timeZone: 'America/La_Paz' })}<span className="table-secondary">{new Date(diagnostico.fechaDiagnostico).toLocaleTimeString('es-BO', { timeZone: 'America/La_Paz', hour: '2-digit', minute: '2-digit' })}</span></time></td>
        <td><span className={`status-badge status-badge-${diagnostico.resultado === 'POSITIVO' ? 'confirmed' : diagnostico.resultado === 'NEGATIVO' ? 'invalid' : diagnostico.resultado === 'PERDIDA_GESTACION' ? 'danger' : 'warning'}`}>{RESULTADO_GESTACION_LABELS[diagnostico.resultado]}</span></td>
        <td>{diagnostico.metodo ? METODO_DIAGNOSTICO_LABELS[diagnostico.metodo] : 'No registrado'}</td>
        <td>{diagnostico.fechaProbableParto ? formatDate(diagnostico.fechaProbableParto) : 'Sin fecha estimada'}</td>
      </tr>)}</tbody></table></div>}
      {diagnosticos.content.length > 0 && <div className="mobile-only">{diagnosticos.content.map((diagnostico) => <div key={diagnostico.id} className="mobile-entity-card">
        <div><strong className="diagnostico-animal">{diagnostico.nombreAnimal || diagnostico.codigoAnimal}</strong>{diagnostico.nombreAnimal && <span className="table-secondary">{diagnostico.codigoAnimal}</span>}<p className="muted">{new Date(diagnostico.fechaDiagnostico).toLocaleString('es-BO', { timeZone: 'America/La_Paz' })} · {RESULTADO_GESTACION_LABELS[diagnostico.resultado]}</p><p>Método: {diagnostico.metodo ? METODO_DIAGNOSTICO_LABELS[diagnostico.metodo] : 'No registrado'}</p><p>Parto probable: {diagnostico.fechaProbableParto ? formatDate(diagnostico.fechaProbableParto) : 'Sin fecha estimada'}</p></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar diagnóstico" onClose={() => setShowForm(false)} description="Registra el resultado de un diagnóstico de gestación.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalSearchSelect label="Animal" name="animalId" sexo="HEMBRA" value={animalId} onChange={seleccionarAnimal} />
        <Field label="Servicio asociado" hint="Un resultado positivo abre la gestación. Sin servicio, se registra con antecedentes desconocidos, sin inventar una monta."><select name="servicioId" key={animalId}><option value="">Sin servicio conocido</option>{servicios.filter((item) => item.hembraId === animalId && item.estado !== 'ANULADO' && item.estado !== 'FINALIZADO').map((servicio) => <option key={servicio.id} value={servicio.id}>Servicio {new Date(servicio.fechaServicio).toLocaleString('es-BO')} (#{servicio.numeroIntento})</option>)}</select></Field>
        <Field label="Fecha y hora del diagnóstico" required><input name="fechaDiagnostico" type="datetime-local" required /></Field>
        <Field label="Resultado" required><select name="resultado" required value={resultado} onChange={(event) => setResultado(event.target.value)}><option value="" disabled>Selecciona…</option>{Object.entries(RESULTADO_GESTACION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        {resultado === 'POSITIVO' && <>
          <Field label="Método"><select name="metodo"><option value="">Sin especificar</option>{Object.entries(METODO_DIAGNOSTICO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
          <Field label="Días de gestación estimados"><input name="diasGestacionEstimados" type="number" inputMode="numeric" min="0" max="400" /></Field>
        </>}
        <Field label="Propiedad"><select name="propiedadId" value={propiedadId} onChange={(event) => setPropiedadId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId" value={potreroId} onChange={(event) => setPotreroId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.paddocks.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote" hint="Se completa con el lote actual del animal. No cambia su pertenencia al lote."><input readOnly value={!animalId ? 'Selecciona primero el animal' : !loteId ? 'Sin lote asignado' : loteCatalogo?.nombre || loteActual.data?.nombre || (loteActual.isError ? 'Lote asignado (nombre no disponible)' : 'Cargando lote…')} /></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar diagnóstico</Button></div>
      </form>
    </Modal>
  </div>
}
