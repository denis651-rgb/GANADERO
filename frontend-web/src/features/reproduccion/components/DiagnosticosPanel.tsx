import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
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
        loteId: String(data.get('loteId') || '') || undefined,
        clienteUuid: crypto.randomUUID(),
      })
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const seleccionarAnimal = (id: string) => {
    setAnimalId(id)
    const animal = catalogs?.hembras.find((item) => item.id === id)
    setPropiedadId(animal?.propiedadActualId ?? '')
    setPotreroId(animal?.potreroActualId ?? '')
  }

  const errorVisible = error ?? crear.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Diagnósticos de gestación</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar diagnóstico</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando diagnósticos…" />}
      {!isLoading && diagnosticos.content.length === 0 && <EmptyState title="No hay diagnósticos registrados" description="Registra el primer diagnóstico de gestación." />}
      {diagnosticos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Diagnósticos de gestación</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha</th><th scope="col">Resultado</th><th scope="col">Método</th><th scope="col">Parto probable</th></tr></thead><tbody>{diagnosticos.content.map((diagnostico) => <tr key={diagnostico.id}>
        <td><strong>{diagnostico.codigoAnimal}</strong>{diagnostico.nombreAnimal ? ` · ${diagnostico.nombreAnimal}` : ''}</td>
        <td>{new Date(diagnostico.fechaDiagnostico).toLocaleString('es-BO')}</td>
        <td><span className={`status-badge status-badge-${diagnostico.resultado === 'POSITIVO' ? 'confirmed' : diagnostico.resultado === 'NEGATIVO' ? 'invalid' : diagnostico.resultado === 'PERDIDA_GESTACION' ? 'danger' : 'warning'}`}>{RESULTADO_GESTACION_LABELS[diagnostico.resultado]}</span></td>
        <td className="table-secondary">{diagnostico.metodo ? METODO_DIAGNOSTICO_LABELS[diagnostico.metodo] : '—'}</td>
        <td className="table-secondary">{diagnostico.fechaProbableParto ? new Date(diagnostico.fechaProbableParto).toLocaleDateString('es-BO') : '—'}</td>
      </tr>)}</tbody></table></div>}
      {diagnosticos.content.length > 0 && <div className="mobile-only">{diagnosticos.content.map((diagnostico) => <div key={diagnostico.id} className="mobile-entity-card">
        <div><strong>{diagnostico.codigoAnimal}</strong><p className="muted">{new Date(diagnostico.fechaDiagnostico).toLocaleString('es-BO')} · {RESULTADO_GESTACION_LABELS[diagnostico.resultado]}</p><p className="muted">{diagnostico.metodo ? METODO_DIAGNOSTICO_LABELS[diagnostico.metodo] : '—'}{diagnostico.fechaProbableParto ? ` · Parto probable: ${new Date(diagnostico.fechaProbableParto).toLocaleDateString('es-BO')}` : ''}</p></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar diagnóstico" onClose={() => setShowForm(false)} description="Registra el resultado de un diagnóstico de gestación.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required><select name="animalId" required value={animalId} onChange={(event) => seleccionarAnimal(event.target.value)}><option value="">Selecciona…</option>{catalogs?.hembras.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Servicio asociado"><select name="servicioId"><option value="">Sin asociar</option>{servicios.filter((item) => !animalId || item.hembraId === animalId).map((servicio) => <option key={servicio.id} value={servicio.id}>Servicio {new Date(servicio.fechaServicio).toLocaleString('es-BO')} (#{servicio.numeroIntento})</option>)}</select></Field>
        <Field label="Fecha y hora del diagnóstico" required><input name="fechaDiagnostico" type="datetime-local" required /></Field>
        <Field label="Resultado" required><select name="resultado" required value={resultado} onChange={(event) => setResultado(event.target.value)}><option value="" disabled>Selecciona…</option>{Object.entries(RESULTADO_GESTACION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        {resultado === 'POSITIVO' && <>
          <Field label="Método"><select name="metodo"><option value="">Sin especificar</option>{Object.entries(METODO_DIAGNOSTICO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
          <Field label="Días de gestación estimados"><input name="diasGestacionEstimados" type="number" inputMode="numeric" min="0" max="400" /></Field>
        </>}
        <Field label="Veterinario"><select name="veterinarioId"><option value="">Sin veterinario</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <Field label="Propiedad"><select name="propiedadId" value={propiedadId} onChange={(event) => setPropiedadId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId" value={potreroId} onChange={(event) => setPotreroId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.paddocks.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote"><select name="loteId"><option value="">Sin especificar</option>{catalogs?.lots.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar diagnóstico</Button></div>
      </form>
    </Modal>
  </div>
}
