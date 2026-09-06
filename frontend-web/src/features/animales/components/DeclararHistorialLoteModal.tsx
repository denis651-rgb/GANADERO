import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { ClipboardCheck, Plus, Trash2 } from 'lucide-react'
import { registrarHistorialDeclaradoLote, TIPO_ACTIVIDAD_LABELS, type TipoActividad } from '@/features/sanidad/api'
import type { AnimalSummary } from '@/features/animales/types'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'
import { todayInBolivia } from '@/shared/utils/date'

interface Props {
  animales: AnimalSummary[]
  onClose: () => void
  onSuccess: (animalIds: string[], aplicaciones: number) => void
}

interface ActividadRow {
  key: number
  tipoActividad: '' | TipoActividad
  fechaAplicacion: string
  productoTexto: string
  observaciones: string
}

let nextKey = 0
const nuevaActividad = (): ActividadRow => ({
  key: nextKey++, tipoActividad: '', fechaAplicacion: '', productoTexto: '', observaciones: '',
})

export function DeclararHistorialLoteModal({ animales, onClose, onSuccess }: Props) {
  const [seleccionados, setSeleccionados] = useState<Set<string>>(() => new Set(animales.map((animal) => animal.id)))
  const [actividades, setActividades] = useState<ActividadRow[]>(() => [nuevaActividad()])
  const registrar = useMutation({
    mutationFn: () => registrarHistorialDeclaradoLote({
      animalIds: [...seleccionados],
      actividades: actividades.map(({ tipoActividad, fechaAplicacion, productoTexto, observaciones }) => ({
        tipoActividad: tipoActividad as TipoActividad,
        fechaAplicacion,
        productoTexto: productoTexto.trim() || undefined,
        observaciones: observaciones.trim() || undefined,
      })),
    }),
    onSuccess: (resultado) => onSuccess([...seleccionados], resultado.length),
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
      ? { ...actividad, [campo]: valor }
      : actividad))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (seleccionados.size === 0 || registrar.isPending) return
    registrar.mutate()
  }

  return <Modal open wide title="Declarar historial sanitario grupal" description="Registra antecedentes informados por el proveedor para varios animales." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized ? <Alert tone="danger">{normalized.message}</Alert> : null}
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
            <Field label="Fecha" required><input type="date" required max={todayInBolivia()} value={actividad.fechaAplicacion} onChange={(event) => actualizarActividad(actividad.key, 'fechaAplicacion', event.target.value)} /></Field>
            <Field label="Producto o medicamento"><input placeholder="Nombre informado por el proveedor…" maxLength={300} value={actividad.productoTexto} onChange={(event) => actualizarActividad(actividad.key, 'productoTexto', event.target.value)} /></Field>
            <Field label="Observaciones"><input placeholder="Certificado, fuente o detalle…" maxLength={1000} value={actividad.observaciones} onChange={(event) => actualizarActividad(actividad.key, 'observaciones', event.target.value)} /></Field>
            <Button type="button" variant="ghost" aria-label={`Quitar actividad ${index + 1}`} title="Quitar actividad" disabled={actividades.length === 1} onClick={() => setActividades((actuales) => actuales.filter((item) => item.key !== actividad.key))}><Trash2 size={18} aria-hidden="true" /></Button>
          </div>)}
        </div>
      </section>

      <div className="bulk-history-summary" role="status"><strong>{seleccionados.size} animal(es)</strong><span>×</span><strong>{actividades.length} actividad(es)</strong><span>=</span><strong>{total} registros</strong></div>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button><Button type="submit" loading={registrar.isPending} disabled={seleccionados.size === 0}><ClipboardCheck size={17} aria-hidden="true" />Confirmar declaración</Button></div>
    </form>
  </Modal>
}
