import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  listPlanItems,
  listPlanes,
  obtenerElegibilidadJornada,
  seleccionarAnimales,
  TIPO_ACTIVIDAD_LABELS,
  type JornadaSanitaria,
  type PlanSanitarioItem,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

export interface PreparacionJornada {
  seleccionados: number
  planItem: PlanSanitarioItem
  fechaAplicacion: string
}

interface JornadaPrepararModalProps {
  jornada: JornadaSanitaria
  catalogs: SanidadCatalogs
  onClose: () => void
  onSaved: (preparacion: PreparacionJornada) => void
}

export function JornadaPrepararModal({ jornada, catalogs, onClose, onSaved }: JornadaPrepararModalProps) {
  const [planItemId, setPlanItemId] = useState('')
  const [fechaAplicacion, setFechaAplicacion] = useState(() => new Date().toISOString().slice(0, 10))
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [vista, setVista] = useState<'ELEGIBLES' | 'EXCLUIDOS'>('ELEGIBLES')

  const planItems = useQuery({
    queryKey: ['sanidad-plan-items-activos', jornada.tipoJornada],
    queryFn: async () => {
      const planes = (await listPlanes()).filter((plan) => plan.estado === 'ACTIVO')
      const listas = await Promise.all(planes.map((plan) => listPlanItems(plan.id)))
      return listas.flat().filter((item) => item.activo && item.tipoActividad === jornada.tipoJornada)
    },
  })
  const itemSeleccionado = planItems.data?.find((item) => item.id === planItemId)
  const elegibilidad = useQuery({
    queryKey: ['sanidad-elegibilidad-jornada', jornada.id, planItemId, fechaAplicacion],
    queryFn: () => obtenerElegibilidadJornada(jornada.id, planItemId, fechaAplicacion),
    enabled: Boolean(planItemId && fechaAplicacion),
  })
  const guardar = useMutation({
    mutationFn: () => seleccionarAnimales(jornada.id, {
      planItemId,
      fechaAplicacion,
      animalIds: Array.from(selected),
    }),
    onSuccess: (ids) => {
      if (itemSeleccionado) onSaved({ seleccionados: ids.length, planItem: itemSeleccionado, fechaAplicacion })
    },
  })

  const categoria = itemSeleccionado?.categoriaAnimalId
    ? catalogs.categories.find((item) => item.id === itemSeleccionado.categoriaAnimalId)?.nombre ?? 'Categoría específica'
    : 'Todas'
  const sexo = itemSeleccionado?.sexoAplicable === 'MACHO'
    ? 'Macho'
    : itemSeleccionado?.sexoAplicable === 'HEMBRA' ? 'Hembra' : 'Ambos'
  const rangoEdad = itemSeleccionado?.edadMinDias !== undefined || itemSeleccionado?.edadMaxDias !== undefined
    ? `${itemSeleccionado.edadMinDias ?? 0}–${itemSeleccionado.edadMaxDias ?? 'sin límite'} días`
    : 'Sin restricción'

  function cambiarActividad(value: string) {
    setPlanItemId(value)
    setSelected(new Set())
    setVista('ELEGIBLES')
  }

  function cambiarFecha(value: string) {
    setFechaAplicacion(value)
    setSelected(new Set())
  }

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const elegibles = elegibilidad.data?.elegibles ?? []
  const excluidos = elegibilidad.data?.noElegibles ?? []

  return <Modal open title={`Preparar jornada · ${TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}`} onClose={onClose} wide description="Selecciona primero la actividad. El sistema verificará automáticamente qué animales pueden participar.">
    <div className="page-stack">
      <form className="form-grid" onSubmit={(event) => event.preventDefault()}>
        <Field label="Actividad del plan" required>
          <select value={planItemId} required onChange={(event) => cambiarActividad(event.target.value)}>
            <option value="">Selecciona una actividad…</option>
            {planItems.data?.map((item) => <option key={item.id} value={item.id}>{TIPO_ACTIVIDAD_LABELS[item.tipoActividad]}{item.productoRecomendadoTexto ? ` · ${item.productoRecomendadoTexto}` : ''}</option>)}
          </select>
        </Field>
        <Field label="Fecha de aplicación" required>
          <input type="date" required max={new Date().toISOString().slice(0, 10)} value={fechaAplicacion} onChange={(event) => cambiarFecha(event.target.value)} />
        </Field>
      </form>

      {planItems.isPending && <LoadingState message="Cargando actividades del plan…" />}
      {planItems.data?.length === 0 && <Alert tone="info">No existe una actividad activa del tipo {TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}. Créala primero en Planes sanitarios.</Alert>}

      {itemSeleccionado && <div className="eligibility-criteria" aria-label="Criterios de elegibilidad">
        <strong>Criterios aplicados automáticamente</strong>
        <span>Categoría: <b>{categoria}</b></span>
        <span>Sexo: <b>{sexo}</b></span>
        <span>Edad: <b>{rangoEdad}</b></span>
      </div>}

      {elegibilidad.isPending && <LoadingState message="Verificando animales…" />}
      {elegibilidad.error && <Alert tone="danger">{normalizeApiError(elegibilidad.error).message}</Alert>}

      {elegibilidad.data && <>
        <div className="eligibility-summary" aria-live="polite">
          <span><strong>{elegibles.length}</strong> elegibles</span>
          <span className={excluidos.length ? 'eligibility-excluded-count' : undefined}><strong>{excluidos.length}</strong> excluidos</span>
        </div>
        <div className="tabs" role="tablist" aria-label="Resultado de elegibilidad">
          <button type="button" role="tab" aria-selected={vista === 'ELEGIBLES'} className={`tab-button ${vista === 'ELEGIBLES' ? 'active' : ''}`} onClick={() => setVista('ELEGIBLES')}>Elegibles ({elegibles.length})</button>
          <button type="button" role="tab" aria-selected={vista === 'EXCLUIDOS'} className={`tab-button ${vista === 'EXCLUIDOS' ? 'active' : ''}`} onClick={() => setVista('EXCLUIDOS')}>Excluidos ({excluidos.length})</button>
        </div>

        {vista === 'ELEGIBLES' && elegibles.length === 0 && <Alert tone="info">Ningún animal cumple todos los criterios. Revisa la pestaña “Excluidos” para conocer los motivos.</Alert>}
        {vista === 'ELEGIBLES' && elegibles.length > 0 && <div className="table-wrapper"><table><caption className="visually-hidden">Animales elegibles</caption><thead><tr><th scope="col"><span className="visually-hidden">Seleccionar</span></th><th scope="col">Animal</th><th scope="col">Sexo</th><th scope="col">Edad</th></tr></thead><tbody>{elegibles.map((animal) => <tr key={animal.id} className={selected.has(animal.id) ? 'selected-row' : undefined}>
          <td><label className="checkbox-line"><input type="checkbox" checked={selected.has(animal.id)} onChange={() => toggle(animal.id)} aria-label={`Seleccionar ${animal.codigo}`} /></label></td>
          <td><strong>{animal.codigo}</strong>{animal.nombre ? <span className="table-secondary">{animal.nombre}</span> : null}</td><td>{animal.sexo === 'HEMBRA' ? 'Hembra' : 'Macho'}</td><td>{animal.edadDias != null ? `${animal.edadDias} días` : 'Sin fecha'}</td>
        </tr>)}</tbody></table></div>}

        {vista === 'EXCLUIDOS' && excluidos.length === 0 && <Alert tone="success">Todos los animales del alcance cumplen los criterios.</Alert>}
        {vista === 'EXCLUIDOS' && excluidos.length > 0 && <div className="eligibility-excluded-list">{excluidos.map((animal) => <div key={animal.id} className="eligibility-excluded-item">
          <strong>{animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</strong>
          <ul>{animal.motivos.map((motivo) => <li key={motivo}>{motivo}</li>)}</ul>
        </div>)}</div>}
      </>}

      <div className="form-actions">
        <span className="muted">{selected.size} animal(es) seleccionados.</span>
        <Button onClick={() => setSelected(new Set(elegibles.map((animal) => animal.id)))} variant="secondary" disabled={elegibles.length === 0}>Seleccionar todos los elegibles</Button>
        <Button onClick={() => guardar.mutate()} loading={guardar.isPending} disabled={!planItemId || selected.size === 0}>Continuar a confirmación</Button>
      </div>
      {guardar.error && <Alert tone="danger">{normalizeApiError(guardar.error).message}</Alert>}
    </div>
  </Modal>
}
