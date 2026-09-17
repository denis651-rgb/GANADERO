import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle2, ClipboardCheck, Plus, Trash2 } from 'lucide-react'
import { listActivePlanItems, registrarHistorialDeclaradoLote, TIPO_ACTIVIDAD_LABELS, type TipoActividad } from '@/features/sanidad/api'
import type { AnimalSummary } from '@/features/animales/types'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { PageHeader } from '@/shared/components/PageHeader'
import { todayInBolivia } from '@/shared/utils/date'

interface ActividadRow {
  key: number
  tipoActividad: '' | TipoActividad
  planItemId: string
  fechaAplicacion: string
  productoTexto: string
  observaciones: string
}

let nextKey = 0
const nuevaActividad = (): ActividadRow => ({
  key: nextKey++, tipoActividad: '', planItemId: '', fechaAplicacion: '', productoTexto: '', observaciones: '',
})

export function DeclararHistorialLotePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const animales = (location.state as { animales?: AnimalSummary[] } | null)?.animales ?? []

  const [seleccionados, setSeleccionados] = useState<Set<string>>(() => new Set(animales.map((animal) => animal.id)))
  const [actividades, setActividades] = useState<ActividadRow[]>(() => [nuevaActividad()])
  const [guardados, setGuardados] = useState(0)
  const planItems = useQuery({ queryKey: ['sanidad-plan-items-activos'], queryFn: listActivePlanItems })
  const registrar = useMutation({
    mutationFn: () => registrarHistorialDeclaradoLote({
      animalIds: [...seleccionados],
      actividades: actividades.map(({ tipoActividad, planItemId, fechaAplicacion, productoTexto, observaciones }) => ({
        tipoActividad: tipoActividad as TipoActividad,
        planItemId: planItemId || undefined,
        fechaAplicacion,
        productoTexto: productoTexto.trim() || undefined,
        observaciones: observaciones.trim() || undefined,
      })),
    }),
    onSuccess: (resultado) => setGuardados(resultado.length),
  })
  const normalized = registrar.error ? normalizeApiError(registrar.error) : null
  const total = seleccionados.size * actividades.length

  function toggleAnimal(id: string) {
    setSeleccionados((actuales) => {
      const siguientes = new Set(actuales)
      if (siguientes.has(id)) siguientes.delete(id)
      else siguientes.add(id)
      return siguientes
    })
  }

  function actualizarActividad(key: number, campo: keyof Omit<ActividadRow, 'key'>, valor: string) {
    setActividades((actuales) => actuales.map((actividad) => actividad.key === key
      // Cambiar el tipo invalida el ítem del plan elegido: puede ya no ser del tipo correcto.
      ? { ...actividad, [campo]: valor, ...(campo === 'tipoActividad' ? { planItemId: '' } : {}) }
      : actividad))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (seleccionados.size === 0 || registrar.isPending || guardados > 0) return
    registrar.mutate()
  }

  if (animales.length === 0) {
    return (
      <div className="page-stack">
        <PageHeader eyebrow="Animales" title="Declarar historial sanitario grupal"
          description="Registra antecedentes informados por el proveedor para varios animales de un lote de compra."
actions={<Button variant="ghost" onClick={() => navigate('/animales')}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>} />
        <Card>
          <EmptyState title="Sin animales para declarar"
            description="Esta página se abre desde el ingreso por lote de compra, con los animales recién registrados." />
          <div className="form-actions"><Button onClick={() => navigate('/animales')}>Ir a Animales</Button></div>
        </Card>
      </div>
    )
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Animales" title="Declarar historial sanitario grupal"
        description="Registra antecedentes informados por el proveedor para todos los animales del lote recién ingresado."
        actions={<Button variant="ghost" onClick={() => navigate('/animales')}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>} />

      <Card>
        <form className="bulk-history-form" onSubmit={submit}>
          {normalized ? <Alert tone="danger">{normalized.message}</Alert> : null}
          {guardados > 0 ? <Alert tone="success">Se registraron {guardados} antecedente(s) sanitario(s) correctamente.</Alert> : null}

          <section className="bulk-history-section" aria-labelledby="animales-historial-title">
            <div className="section-heading">
              <div><h3 id="animales-historial-title">Animales</h3><p className="muted">Selecciona los que comparten estos antecedentes.</p></div>
              <Button type="button" variant="ghost" onClick={() => setSeleccionados(seleccionados.size === animales.length ? new Set() : new Set(animales.map((animal) => animal.id)))}>
                {seleccionados.size === animales.length ? 'Quitar todos' : 'Seleccionar todos'}
              </Button>
            </div>
            <div className="bulk-animal-grid">
              {animales.map((animal) => <label key={animal.id} className="bulk-animal-option">
                <input type="checkbox" checked={seleccionados.has(animal.id)} onChange={() => toggleAnimal(animal.id)} />
                <span><strong>{animal.codigo}</strong><small>{animal.nombre || 'Sin nombre'}</small></span>
              </label>)}
            </div>
          </section>

          <section className="bulk-history-section" aria-labelledby="actividades-historial-title">
            <div className="section-heading"><div><h3 id="actividades-historial-title">Actividades declaradas</h3><p className="muted">Cada actividad se aplicará a todos los animales seleccionados.</p></div><Button type="button" variant="secondary" onClick={() => setActividades((actuales) => [...actuales, nuevaActividad()])}><Plus size={16} aria-hidden="true" />Agregar actividad</Button></div>
            <div className="bulk-activity-list">
              {actividades.map((actividad, index) => <div className="bulk-activity-card" key={actividad.key}>
                <div className="bulk-activity-number">Actividad {index + 1}</div>
                <Field label="Tipo" required><select required value={actividad.tipoActividad} onChange={(event) => actualizarActividad(actividad.key, 'tipoActividad', event.target.value)}><option value="" disabled>Selecciona…</option>{(Object.keys(TIPO_ACTIVIDAD_LABELS) as TipoActividad[]).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}</select></Field>
                {actividad.tipoActividad && <Field label="Ítem del plan" hint="Opcional. Actualiza el calendario sanitario y evita alertas de vacuna próxima/vencida para este ítem.">
                  <select value={actividad.planItemId} onChange={(event) => actualizarActividad(actividad.key, 'planItemId', event.target.value)}>
                    <option value="">Sin vincular</option>
                    {(planItems.data ?? []).filter((item) => item.tipoActividad === actividad.tipoActividad).map((item) => <option key={item.id} value={item.id}>{item.nombre}{item.productoRecomendadoTexto ? ` · ${item.productoRecomendadoTexto}` : ''}</option>)}
                  </select>
                </Field>}
                <Field label="Fecha" required><input type="date" required max={todayInBolivia()} value={actividad.fechaAplicacion} onChange={(event) => actualizarActividad(actividad.key, 'fechaAplicacion', event.target.value)} /></Field>
                <Field label="Producto o medicamento"><input placeholder="Nombre informado por el proveedor…" maxLength={300} value={actividad.productoTexto} onChange={(event) => actualizarActividad(actividad.key, 'productoTexto', event.target.value)} /></Field>
                <Field label="Observaciones"><input placeholder="Certificado, fuente o detalle…" maxLength={1000} value={actividad.observaciones} onChange={(event) => actualizarActividad(actividad.key, 'observaciones', event.target.value)} /></Field>
                <Button type="button" variant="ghost" aria-label={`Quitar actividad ${index + 1}`} title="Quitar actividad" disabled={actividades.length === 1} onClick={() => setActividades((actuales) => actuales.filter((item) => item.key !== actividad.key))}><Trash2 size={18} aria-hidden="true" /></Button>
              </div>)}
            </div>
          </section>

          <div className="bulk-history-summary" role="status"><strong>{seleccionados.size} animal(es)</strong><span>×</span><strong>{actividades.length} actividad(es)</strong><span>=</span><strong>{total} registros</strong></div>
          <div className="form-actions">
            {guardados > 0
              ? <Button onClick={() => navigate('/animales')}><CheckCircle2 size={17} aria-hidden="true" />Ir a Animales</Button>
              : <>
                  <Button type="button" variant="secondary" onClick={() => navigate('/animales')}>Cancelar</Button>
                  <Button type="submit" loading={registrar.isPending} disabled={seleccionados.size === 0}><ClipboardCheck size={17} aria-hidden="true" />Confirmar declaración</Button>
                </>}
          </div>
        </form>
      </Card>
    </div>
  )
}