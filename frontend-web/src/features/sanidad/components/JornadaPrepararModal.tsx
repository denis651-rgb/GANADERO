import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import {
  listPlanItems,
  listPlanes,
  obtenerElegibilidadJornada,
  seleccionarAnimales,
  TIPO_ACTIVIDAD_LABELS,
  type JornadaSanitaria,
  type PlanSanitarioItem,
} from '@/features/sanidad/api'
import { datosPlanilla, filasCsvPlanilla, nombreArchivoPlanilla } from '@/features/sanidad/planilla'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'
import { descargarCsv } from '@/shared/utils/csv'
import { useToast } from '@/shared/toast/useToast'

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
  /** Animales de la visita anterior (misma fecha/ubicación), para no volver a elegirlos uno por uno. */
  preseleccionAnimalIds?: string[]
}

export function JornadaPrepararModal({ jornada, catalogs, onClose, onSaved, preseleccionAnimalIds }: JornadaPrepararModalProps) {
  const { showToast } = useToast()
  const [planItemId, setPlanItemId] = useState('')
  const [fechaAplicacion, setFechaAplicacion] = useState(() => new Date().toISOString().slice(0, 10))
  const [manualSelection, setManualSelection] = useState<Set<string> | null>(null)
  const [vista, setVista] = useState<'ELEGIBLES' | 'EXCLUIDOS'>('ELEGIBLES')
  const [exportando, setExportando] = useState(false)

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
    setManualSelection(null)
    setVista('ELEGIBLES')
  }

  function cambiarFecha(value: string) {
    setFechaAplicacion(value)
    setManualSelection(null)
  }

  const elegibles = useMemo(() => elegibilidad.data?.elegibles ?? [], [elegibilidad.data])
  const excluidos = elegibilidad.data?.noElegibles ?? []

  // Mientras el usuario no toque la selección a mano, arranca con los animales de la visita
  // anterior que además sigan elegibles para esta actividad y fecha (si no viene ninguna
  // preselección, esto da un Set vacío y el comportamiento es igual que antes).
  const preseleccion = useMemo(() => new Set(preseleccionAnimalIds ?? []), [preseleccionAnimalIds])
  const defaultSelected = useMemo(
    () => new Set(elegibles.filter((animal) => preseleccion.has(animal.id)).map((animal) => animal.id)),
    [elegibles, preseleccion],
  )
  const selected = manualSelection ?? defaultSelected

  function toggle(id: string) {
    const next = new Set(selected)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setManualSelection(next)
  }

  async function exportarPlanilla() {
    if (!itemSeleccionado) return
    const input = datosPlanilla({
      actividad: itemSeleccionado,
      fechaAplicacion,
      propiedad: catalogs.properties.find((item) => item.id === jornada.propiedadId)?.nombre,
      potrero: jornada.potreroId ? catalogs.paddocks.find((item) => item.id === jornada.potreroId)?.nombre : undefined,
      lote: jornada.loteGanaderoId ? catalogs.lots.find((item) => item.id === jornada.loteGanaderoId)?.nombre : undefined,
      animales: elegibles.filter((animal) => selected.has(animal.id)),
    })

    if (!window.ganadero?.sanidad) {
      descargarCsv(nombreArchivoPlanilla(input, 'csv'), [], filasCsvPlanilla(input))
      return
    }
    setExportando(true)
    try {
      const resultado = await window.ganadero.sanidad.exportarPlanilla(input)
      if (!resultado.cancelado) showToast('Planilla de campo guardada.')
    } catch {
      showToast('No se pudo generar la planilla.', 'danger')
    } finally {
      setExportando(false)
    }
  }

  return <Modal open title={`Preparar jornada · ${TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}`} onClose={onClose} wide description="Selecciona primero la actividad. El sistema verificará automáticamente qué animales pueden participar.">
    <div className="page-stack">
      <form className="form-grid" onSubmit={(event) => event.preventDefault()}>
        <Field label="Actividad del plan" required>
          <select value={planItemId} required disabled={planItems.isPending} onChange={(event) => cambiarActividad(event.target.value)}>
            <option value="">Selecciona una actividad…</option>
            {planItems.data?.map((item) => <option key={item.id} value={item.id}>{TIPO_ACTIVIDAD_LABELS[item.tipoActividad]}{item.productoRecomendadoTexto ? ` · ${item.productoRecomendadoTexto}` : ''}</option>)}
          </select>
        </Field>
        <Field label="Fecha de aplicación" required>
          <input type="date" required max={new Date().toISOString().slice(0, 10)} value={fechaAplicacion} onChange={(event) => cambiarFecha(event.target.value)} />
        </Field>
      </form>

      {planItems.isPending && <LoadingState message="Cargando actividades del plan…" />}
      {planItems.error && <Alert tone="danger">{normalizeApiError(planItems.error).message} <button type="button" className="text-link" onClick={() => void planItems.refetch()}>Reintentar</button></Alert>}
      {planItems.data?.length === 0 && <Alert tone="info">No existe una actividad activa del tipo {TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}. Créala primero en Planes sanitarios.</Alert>}
      {!planItems.isPending && !planItems.error && (planItems.data?.length ?? 0) > 0 && !itemSeleccionado && <Alert tone="info">Selecciona una actividad del plan para verificar los animales elegibles.</Alert>}

      {itemSeleccionado && <div className="eligibility-criteria" aria-label="Criterios de elegibilidad">
        <strong>Criterios aplicados automáticamente</strong>
        <span>Categoría: <b>{categoria}</b></span>
        <span>Sexo: <b>{sexo}</b></span>
        <span>Edad: <b>{rangoEdad}</b></span>
      </div>}

      {elegibilidad.isFetching && <LoadingState message="Verificando animales…" />}
      {elegibilidad.error && <Alert tone="danger">{normalizeApiError(elegibilidad.error).message}</Alert>}

      {elegibilidad.data && <>
        {manualSelection === null && defaultSelected.size > 0 && (
          <Alert tone="info">Se preseleccionaron {defaultSelected.size} animal(es) de la visita anterior que también son elegibles para esta actividad. Podés ajustar la selección abajo.</Alert>
        )}
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
          <td><strong>{animal.codigo}</strong>{animal.nombre ? <span className="table-secondary">{animal.nombre}</span> : null}</td><td>{animal.sexo === 'HEMBRA' ? 'Hembra' : 'Macho'}</td><td>{animal.edadDias != null ? `${animal.edadEstimada ? '≈ ' : ''}${animal.edadDias} días${animal.edadEstimada ? ' (estimada)' : ''}` : 'Sin fecha'}</td>
        </tr>)}</tbody></table></div>}

        {vista === 'EXCLUIDOS' && excluidos.length === 0 && <Alert tone="success">Todos los animales del alcance cumplen los criterios.</Alert>}
        {vista === 'EXCLUIDOS' && excluidos.length > 0 && <div className="eligibility-excluded-list">{excluidos.map((animal) => <div key={animal.id} className="eligibility-excluded-item">
          <strong>{animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</strong>
          <ul>{animal.motivos.map((motivo) => <li key={motivo}>{motivo}</li>)}</ul>
        </div>)}</div>}
      </>}

      <div className="form-actions">
        <span className="muted">{selected.size} animal(es) seleccionados.</span>
        <Button onClick={() => setManualSelection(new Set(elegibles.map((animal) => animal.id)))} variant="secondary" disabled={elegibles.length === 0}>Seleccionar todos los elegibles</Button>
        <Button onClick={() => void exportarPlanilla()} variant="secondary" loading={exportando} disabled={!itemSeleccionado || selected.size === 0}>
          <Download size={16} aria-hidden="true" />Exportar planilla
        </Button>
        <Button onClick={() => guardar.mutate()} loading={guardar.isPending} disabled={!planItemId || selected.size === 0}>Continuar a confirmación</Button>
      </div>
      {guardar.error && <Alert tone="danger">{normalizeApiError(guardar.error).message}</Alert>}
    </div>
  </Modal>
}
