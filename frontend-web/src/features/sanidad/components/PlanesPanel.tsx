import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDown, ChevronRight, Pencil, Plus, Power } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  actualizarPlanItem,
  cambiarEstadoItem,
  cambiarEstadoPlan,
  crearPlan,
  crearPlanItem,
  listConflictosActivacion,
  listPlanItems,
  LUGAR_APLICACION_LABELS,
  MODALIDAD_ACTIVIDAD_LABELS,
  ORIGEN_REGULATORIO_BADGE_CLASS,
  ORIGEN_REGULATORIO_LABELS,
  REFERENCIA_CALCULO_PERIODICA_LABELS,
  TIPO_ACTIVIDAD_LABELS,
  TIPO_CALCULO_DOSIS_LABELS,
  UNIDAD_DOSIS_LABELS,
  VIA_ADMINISTRACION_LABELS,
  type CrearItemInput,
  type CrearPlanInput,
  type EstadoPlan,
  type FechaProgramadaConfig,
  type LugarAplicacion,
  type ModalidadActividad,
  type ModalidadConfig,
  type OrigenRegulatorio,
  type PlanSanitario,
  type PeriodicaConfig,
  type PlanSanitarioItem,
  type PorEdadConfig,
  type PorHallazgoConfig,
  type ReferenciaCalculoPeriodica,
  type TipoCalculoDosis,
  type UnidadDosis,
  type UnidadEdadActividad,
  type UnidadFrecuencia,
  type ViaAdministracion,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'
import { convertirEdadADias, errorEdadObjetivoFueraDeRango, errorRangoEdad, rangoEdadDesdeDias, type UnidadEdad } from '@/features/sanidad/ageRange'
import { pesoReferenciaFijo } from '@/features/sanidad/dosisReferencia'
import {
  errorFechaFin, errorHallazgos, erroresDosis, erroresViaLugar, fechaHoraLocal,
} from '@/features/sanidad/validacionesActividad'

interface PlanesPanelProps {
  planes: PlanSanitario[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
}

const TIPOS_HALLAZGO_CATALOGO = [
  { valor: 'CASO_CLINICO_ABIERTO', label: 'Caso clínico abierto' },
  { valor: 'CONTROL_ECTOPARASITARIO_POSITIVO', label: 'Control ectoparasitario con carga alta' },
  { valor: 'EXAMEN_REPRODUCTIVO_NO_APTO', label: 'Examen reproductivo no apto' },
  { valor: 'CONTROL_NEONATAL_ALERTA', label: 'Control neonatal con alerta' },
]

/** Configuración de la modalidad de una actividad existente, para precargar el formulario al editar. */
function configDe<T extends ModalidadConfig>(item: PlanSanitarioItem | null | undefined, modalidad: ModalidadActividad, campo: string): T | undefined {
  return item?.modalidad === modalidad && campo in item.modalidadConfig ? item.modalidadConfig as T : undefined
}
const configPorEdad = (item: PlanSanitarioItem | null | undefined) => configDe<PorEdadConfig>(item, 'POR_EDAD', 'edadObjetivoValor')

export function PlanesPanel({ planes, isLoading, error, catalogs, refresh }: PlanesPanelProps) {
  const client = useQueryClient()
  const { can } = useAuth()
  const canAdmin = can('SANIDAD_PLAN_ADMINISTRAR')
  const [showPlanForm, setShowPlanForm] = useState(false)
  const [showItemForm, setShowItemForm] = useState(false)
  const [editingItem, setEditingItem] = useState<PlanSanitarioItem | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)
  const [edadMinValor, setEdadMinValor] = useState('')
  const [edadMaxValor, setEdadMaxValor] = useState('')
  const [unidadEdad, setUnidadEdad] = useState<UnidadEdad>('MESES')
  const [sinEdadMaxima, setSinEdadMaxima] = useState(false)
  const [modalidad, setModalidad] = useState<ModalidadActividad>('MANUAL')
  const [edadObjetivoValor, setEdadObjetivoValor] = useState('')
  const [edadObjetivoUnidad, setEdadObjetivoUnidad] = useState<UnidadEdad>('MESES')
  const [unaVezEnLaVida, setUnaVezEnLaVida] = useState(true)
  const [origen, setOrigen] = useState<OrigenRegulatorio | ''>('')
  const [obligatorio, setObligatorio] = useState(false)
  const [categoriasSeleccionadas, setCategoriasSeleccionadas] = useState<string[]>([])
  const [dosisTipoCalculo, setDosisTipoCalculo] = useState<TipoCalculoDosis>('NO_APLICA')
  const [dosisUnidad, setDosisUnidad] = useState<UnidadDosis | ''>('')
  const [pesoReferenciaValor, setPesoReferenciaValor] = useState('')
  const [viaCodigo, setViaCodigo] = useState<ViaAdministracion | ''>('')
  const [lugar, setLugar] = useState<LugarAplicacion | ''>('')
  const [viaDetalle, setViaDetalle] = useState('')
  const [lugarDetalle, setLugarDetalle] = useState('')
  const [dosisCantidadValor, setDosisCantidadValor] = useState('')
  const [dosisMinimaValor, setDosisMinimaValor] = useState('')
  const [dosisMaximaValor, setDosisMaximaValor] = useState('')
  const [planFechaInicio, setPlanFechaInicio] = useState('')
  const [planFechaFin, setPlanFechaFin] = useState('')
  const [tiposHallazgoSeleccionados, setTiposHallazgoSeleccionados] = useState<string[]>([])
  const [stateTarget, setStateTarget] = useState<{ plan: PlanSanitario; estado: EstadoPlan } | null>(null)
  const [itemTarget, setItemTarget] = useState<{ plan: PlanSanitario; item: { id: string; activo: boolean; version: number; nombre: string } } | null>(null)

  const configEdadItem = configPorEdad(editingItem)
  // Una actividad obligatoria por SENASAG lo es siempre: no se deja contradecir con el checkbox.
  const obligatorioPorOrigen = origen === 'OBLIGATORIO_SENASAG'
  const obligatorioEfectivo = obligatorioPorOrigen || obligatorio
  // Las unidades «por kg / por 10 kg / por 50 kg» ya fijan la referencia; el resto la pide al usuario.
  const pesoReferenciaFija = dosisTipoCalculo === 'POR_PESO' ? pesoReferenciaFijo(dosisUnidad) : undefined
  const pesoReferenciaEfectivo = pesoReferenciaFija !== undefined ? String(pesoReferenciaFija) : pesoReferenciaValor

  const items = useQuery({
    queryKey: ['sanidad-items', expanded],
    queryFn: () => (expanded ? listPlanItems(expanded) : Promise.resolve([])),
    enabled: Boolean(expanded),
  })

  // Aviso no bloqueante: otros planes activos en el mismo alcance con actividades del mismo tipo.
  const conflictos = useQuery({
    queryKey: ['sanidad-conflictos', stateTarget?.plan.id],
    queryFn: () => listConflictosActivacion(stateTarget!.plan.id),
    enabled: stateTarget?.estado === 'ACTIVO',
  })

  const crearPlanMut = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearPlanInput = {
        nombre: String(data.get('nombre')),
        descripcion: String(data.get('descripcion') || '') || undefined,
        fechaInicio: String(data.get('fechaInicio')),
        fechaFin: String(data.get('fechaFin') || '') || undefined,
      }
      return crearPlan(input)
    },
    onSuccess: () => { setShowPlanForm(false); refresh() },
  })

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado, version }: { id: string; estado: EstadoPlan; version: number }) => cambiarEstadoPlan(id, estado, version),
    onSuccess: () => { setStateTarget(null); refresh() },
  })

  function construirModalidadConfig(data: FormData): ModalidadConfig {
    switch (modalidad) {
      case 'POR_EDAD':
        return {
          edadObjetivoValor: Number(edadObjetivoValor) || 0,
          edadUnidad: edadObjetivoUnidad as UnidadEdadActividad,
          ventanaAnticipadaDias: Number(data.get('ventanaAnticipadaDias')) || 0,
          ventanaPosteriorDias: Number(data.get('ventanaPosteriorDias')) || 0,
          politicaEdadEstimada: data.get('excluirEdadEstimada') === 'on' ? 'EXCLUIR' : 'PERMITIR',
          politicaEdadDesconocida: 'EXCLUIR',
          unaVezEnLaVida,
        }
      case 'PERIODICA':
        return {
          frecuenciaValor: Number(data.get('frecuenciaValor')) || 1,
          frecuenciaUnidad: String(data.get('frecuenciaUnidad') || 'DIAS') as UnidadFrecuencia,
          referenciaCalculo: String(data.get('referenciaCalculo') || 'ULTIMA_APLICACION') as ReferenciaCalculoPeriodica,
          toleranciaAnticipadaDias: Number(data.get('toleranciaAnticipadaDias')) || 0,
          toleranciaPosteriorDias: Number(data.get('toleranciaPosteriorDias')) || 0,
        }
      case 'FECHA_PROGRAMADA':
        return {
          fechaProgramada: new Date(String(data.get('fechaProgramada'))).toISOString(),
          unicaVez: true,
        }
      case 'POR_HALLAZGO':
        return {
          tiposHallazgo: tiposHallazgoSeleccionados,
          plazoDias: Number(data.get('plazoDias')) || undefined,
          requiereValidacionVeterinaria: data.get('requiereValidacionVeterinaria') === 'on',
        }
      default:
        return {}
    }
  }

  const crearItemMut = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearItemInput = {
        nombre: String(data.get('nombre')),
        descripcion: String(data.get('descripcion') || '') || undefined,
        tipoActividad: String(data.get('tipoActividad')) as CrearItemInput['tipoActividad'],
        modalidad,
        modalidadConfig: construirModalidadConfig(data),
        productoRecomendadoTexto: String(data.get('productoRecomendadoTexto') || '') || undefined,
        principioActivo: String(data.get('principioActivo') || '') || undefined,
        instruccionesVeterinario: String(data.get('instruccionesVeterinario') || '') || undefined,
        dosisTipoCalculo,
        dosisCantidad: dosisTipoCalculo === 'NO_APLICA' ? undefined : Number(dosisCantidadValor) || undefined,
        dosisUnidad: dosisTipoCalculo === 'NO_APLICA' ? undefined : dosisUnidad || undefined,
        dosisUnidadDetalle: String(data.get('dosisUnidadDetalle') || '') || undefined,
        dosisPesoReferenciaKg: dosisTipoCalculo === 'POR_PESO' ? Number(pesoReferenciaEfectivo) || undefined : undefined,
        dosisMinima: dosisTipoCalculo === 'POR_PESO' ? Number(dosisMinimaValor) || undefined : undefined,
        dosisMaxima: dosisTipoCalculo === 'POR_PESO' ? Number(dosisMaximaValor) || undefined : undefined,
        viaAdministracionCodigo: viaCodigo || undefined,
        viaAdministracionDetalle: viaCodigo === 'OTRA' ? viaDetalle.trim() || undefined : undefined,
        lugarAplicacion: lugar || undefined,
        lugarAplicacionDetalle: lugar === 'OTRO' ? lugarDetalle.trim() || undefined : undefined,
        categoriasAplicables: categoriasSeleccionadas,
        sexoAplicable: (String(data.get('sexoAplicable') || '') || undefined) as CrearItemInput['sexoAplicable'],
        edadMinDias: Number(data.get('edadMinDias')) || undefined,
        edadMaxDias: Number(data.get('edadMaxDias')) || undefined,
        edadUnidad: unidadEdad,
        permiteEdadDesconocida: data.get('permiteEdadDesconocida') === 'on',
        diasAlerta: Number(data.get('diasAlerta')) || 0,
        horaEjecucion: String(data.get('horaEjecucion') || '08:00'),
        horariosAviso: String(data.get('horariosAviso') || '')
          .split(',')
          .map((value) => value.trim())
          .filter(Boolean),
        obligatorio: obligatorioEfectivo,
        origenRegulatorio: origen as OrigenRegulatorio,
        especieAplicable: String(data.get('especieAplicable') || '') || undefined,
      }
      if (editingItem) {
        return actualizarPlanItem(expanded!, editingItem.id, editingItem.version, {
          ...input,
          motivoVersion: String(data.get('motivoVersion') || '') || undefined,
          fechaVigencia: data.get('fechaVigencia') ? new Date(String(data.get('fechaVigencia'))).toISOString() : undefined,
        })
      }
      return crearPlanItem(expanded!, input)
    },
    onSuccess: () => { cerrarFormularioItem(); void client.invalidateQueries({ queryKey: ['sanidad-items'] }) },
  })

  const cambiarItem = useMutation({
    mutationFn: (input: { planId: string; itemId: string; activo: boolean; version: number }) => cambiarEstadoItem(input.planId, input.itemId, input.activo, input.version),
    onSuccess: () => { setItemTarget(null); void client.invalidateQueries({ queryKey: ['sanidad-items'] }) },
  })

  const errorVisible = error ?? crearPlanMut.error ?? cambiarEstado.error ?? items.error ?? crearItemMut.error ?? cambiarItem.error
  const edadMinDias = convertirEdadADias(edadMinValor, unidadEdad)
  const edadMaxDias = sinEdadMaxima ? undefined : convertirEdadADias(edadMaxValor, unidadEdad)
  const edadMinInvalida = edadMinValor !== '' && edadMinDias === undefined
  const edadMaxInvalida = !sinEdadMaxima && edadMaxValor !== '' && edadMaxDias === undefined
  const edadError = edadMinInvalida || edadMaxInvalida
    ? 'La edad debe ser un número entero igual o mayor que cero.'
    : errorRangoEdad(edadMinDias, edadMaxDias)
  // Solo POR_EDAD: la edad objetivo tiene que caer dentro del rango de animales elegibles.
  const edadObjetivoError = modalidad === 'POR_EDAD'
    ? errorEdadObjetivoFueraDeRango(convertirEdadADias(edadObjetivoValor, edadObjetivoUnidad), edadMinDias, edadMaxDias)
    : undefined
  const erroresVia = erroresViaLugar(viaCodigo, lugar, viaDetalle, lugarDetalle)
  const erroresDosisForm = erroresDosis(dosisTipoCalculo, dosisCantidadValor, dosisMinimaValor, dosisMaximaValor)
  const hallazgoError = errorHallazgos(modalidad, tiposHallazgoSeleccionados)
  const formularioInvalido = Boolean(edadError || edadObjetivoError || hallazgoError
    || Object.values(erroresVia).some(Boolean) || Object.values(erroresDosisForm).some(Boolean))
  // El backend solo sabe si la actividad ya se usó: cuando lo avisa, motivo y vigencia pasan a ser obligatorios.
  const versionRequerida = Boolean(crearItemMut.error)
    && normalizeApiError(crearItemMut.error).code === 'SANIDAD_VERSION_MOTIVO_REQUERIDO'
  const errorFin = errorFechaFin(planFechaInicio, planFechaFin)
  const planExpandido = planes.find((plan) => plan.id === expanded)
  // Un plan finalizado o anulado ya no admite cambios en sus actividades (el backend también lo rechaza).
  const planAbierto = planExpandido ? ['BORRADOR', 'ACTIVO'].includes(planExpandido.estado) : false
  const puedeEditarActividades = canAdmin && planAbierto
  const configPeriodicaItem = configDe<PeriodicaConfig>(editingItem, 'PERIODICA', 'frecuenciaValor')
  const configFechaItem = configDe<FechaProgramadaConfig>(editingItem, 'FECHA_PROGRAMADA', 'fechaProgramada')
  const configHallazgoItem = configDe<PorHallazgoConfig>(editingItem, 'POR_HALLAZGO', 'tiposHallazgo')
  const equivalenciaEdad = edadMinDias === undefined && edadMaxDias === undefined
    ? 'Sin restricción por edad.'
    : edadMinDias !== undefined && edadMaxDias !== undefined
      ? `Se guardará el rango de ${edadMinDias} a ${edadMaxDias} días.`
      : edadMinDias !== undefined
        ? `Se aplicará desde los ${edadMinDias} días, sin límite máximo.`
        : `Se aplicará hasta los ${edadMaxDias} días.`

  function abrirFormularioItem(planId: string, item?: PlanSanitarioItem) {
    setExpanded(planId)
    setEditingItem(item ?? null)
    // La edad se guarda en días: se muestra en la unidad del item solo si divide exacto.
    const rango = rangoEdadDesdeDias(item?.edadMinDias, item?.edadMaxDias, item?.edadUnidad)
    setEdadMinValor(rango.minimo)
    setEdadMaxValor(rango.maximo)
    setUnidadEdad(rango.unidad)
    setSinEdadMaxima(false)
    setModalidad(item?.modalidad ?? 'MANUAL')
    const configEdad = configPorEdad(item)
    setEdadObjetivoValor(configEdad ? String(configEdad.edadObjetivoValor) : '')
    setEdadObjetivoUnidad(configEdad?.edadUnidad ?? 'MESES')
    setUnaVezEnLaVida(configEdad?.unaVezEnLaVida ?? true)
    setOrigen(item?.origenRegulatorio ?? '')
    setObligatorio(item?.obligatorio ?? false)
    setCategoriasSeleccionadas(item?.categoriasAplicables ?? [])
    setDosisTipoCalculo(item?.dosisTipoCalculo ?? 'NO_APLICA')
    setDosisUnidad(item?.dosisUnidad ?? '')
    setPesoReferenciaValor(item?.dosisPesoReferenciaKg ? String(item.dosisPesoReferenciaKg) : '')
    setViaCodigo(item?.viaAdministracionCodigo ?? '')
    setLugar(item?.lugarAplicacion ?? '')
    setViaDetalle(item?.viaAdministracionDetalle ?? '')
    setLugarDetalle(item?.lugarAplicacionDetalle ?? '')
    setDosisCantidadValor(item?.dosisCantidad != null ? String(item.dosisCantidad) : '')
    setDosisMinimaValor(item?.dosisMinima != null ? String(item.dosisMinima) : '')
    setDosisMaximaValor(item?.dosisMaxima != null ? String(item.dosisMaxima) : '')
    setTiposHallazgoSeleccionados(item?.modalidad === 'POR_HALLAZGO' && 'tiposHallazgo' in item.modalidadConfig ? item.modalidadConfig.tiposHallazgo : [])
    crearItemMut.reset()
    setShowItemForm(true)
  }

  function abrirFormularioPlan() {
    setPlanFechaInicio('')
    setPlanFechaFin('')
    crearPlanMut.reset()
    setShowPlanForm(true)
  }

  function cerrarFormularioItem() {
    setShowItemForm(false)
    setEditingItem(null)
    setEdadMinValor('')
    setEdadMaxValor('')
    setSinEdadMaxima(false)
  }

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Planes sanitarios</h2>
        {canAdmin && <Button onClick={abrirFormularioPlan} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Nuevo plan</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando planes…" />}
      {!isLoading && planes.length === 0 && <EmptyState title="No hay planes sanitarios" description="Crea un plan para agrupar vacunaciones, desparasitaciones y controles." />}
      {planes.length > 0 && <ul className="detail-list">
        {planes.map((plan) => <li key={plan.id}>
          <button type="button" className="button button-ghost button-small" onClick={() => setExpanded(expanded === plan.id ? null : plan.id)} aria-expanded={expanded === plan.id} aria-label={`${expanded === plan.id ? 'Ocultar' : 'Mostrar'} actividades del plan ${plan.nombre}`}>
            {expanded === plan.id ? <ChevronDown size={16} aria-hidden="true" /> : <ChevronRight size={16} aria-hidden="true" />}
          </button>
          <span className="plan-summary"><strong>{plan.nombre}</strong><span className="table-secondary">{plan.descripcion ?? 'Sin descripción'} · {new Date(plan.fechaInicio).toLocaleDateString('es-BO')}{plan.fechaFin ? ` → ${new Date(plan.fechaFin).toLocaleDateString('es-BO')}` : ''}</span></span>
          <span className={`status-badge status-badge-${plan.estado === 'ACTIVO' ? 'confirmed' : plan.estado === 'BORRADOR' ? 'pending' : 'annulled'}`}>{plan.estado}</span>
          {canAdmin && <span className="inline-actions">
            {plan.estado === 'BORRADOR' && <Button variant="ghost" onClick={() => setStateTarget({ plan, estado: 'ACTIVO' })}>Activar</Button>}
            {plan.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => setStateTarget({ plan, estado: 'FINALIZADO' })}>Finalizar</Button>}
            {['BORRADOR', 'ACTIVO'].includes(plan.estado) && <Button variant="ghost" onClick={() => setStateTarget({ plan, estado: 'ANULADO' })}>Anular</Button>}
            {['BORRADOR', 'ACTIVO'].includes(plan.estado) && <Button variant="ghost" onClick={() => abrirFormularioItem(plan.id)} disabled={!catalogs}><Pencil size={16} aria-hidden="true" />Agregar actividad</Button>}
          </span>}
        </li>)}
      </ul>}
    </Card>
    {expanded && <Card>
      <h3>Actividades de {planExpandido?.nombre}</h3>
      {planExpandido && !planAbierto && <p className="muted">Este plan está {planExpandido.estado.toLowerCase()}: sus actividades ya no se pueden modificar.</p>}
      {items.isPending && <LoadingState message="Cargando actividades…" />}
      {items.error && <Alert tone="danger">{normalizeApiError(items.error).message}</Alert>}
      {items.data?.length === 0 && <p className="muted">Este plan todavía no tiene actividades.</p>}
      {items.data && items.data.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Actividades del plan</caption><thead><tr><th scope="col">Actividad</th><th scope="col">Modalidad</th><th scope="col">Medicamento</th><th scope="col">Dosis</th><th scope="col">Vía / lugar</th><th scope="col">Alerta</th><th scope="col">Versión</th><th scope="col">Estado</th>{puedeEditarActividades && <th scope="col">Acciones</th>}</tr></thead><tbody>{items.data.map((item) => <tr key={item.id}>
        <td><strong>{item.nombre}</strong> <span className="table-secondary">{TIPO_ACTIVIDAD_LABELS[item.tipoActividad]}</span> <span className={`status-badge ${ORIGEN_REGULATORIO_BADGE_CLASS[item.origenRegulatorio]}`}>{ORIGEN_REGULATORIO_LABELS[item.origenRegulatorio]}</span>{item.requiereRevision && <span className="status-badge status-badge-warning">Requiere revisión</span>}</td>
        <td>{MODALIDAD_ACTIVIDAD_LABELS[item.modalidad]}</td>
        <td>{item.productoRecomendadoTexto ?? '—'}{item.principioActivo ? ` (${item.principioActivo})` : ''}</td>
        <td>{item.dosisTipoCalculo === 'NO_APLICA' ? '—' : `${item.dosisCantidad ?? ''} ${item.dosisUnidad ? UNIDAD_DOSIS_LABELS[item.dosisUnidad] : ''}`.trim()}</td>
        <td>{[item.viaAdministracionCodigo ? VIA_ADMINISTRACION_LABELS[item.viaAdministracionCodigo] : null, item.lugarAplicacion ? LUGAR_APLICACION_LABELS[item.lugarAplicacion] : null].filter(Boolean).join(' · ') || '—'}</td>
        <td>{item.horariosAviso?.length ? `${item.diasAlerta} día(s) antes · ${item.horariosAviso.join(', ')}` : '—'}</td>
        <td>v{item.numeroVersion}</td>
        <td><span className="status-badge">{item.activo ? 'ACTIVO' : 'INACTIVO'}</span></td>
        {puedeEditarActividades && <td className="inline-actions">
          <Button variant="ghost" className="jornada-icon-action" title="Editar actividad" aria-label={`Editar ${item.nombre}`} onClick={() => abrirFormularioItem(expanded, item)}><Pencil size={16} aria-hidden="true" /></Button>
          <Button variant="ghost" className={`jornada-icon-action${item.activo ? ' icon-action-danger' : ''}`} title={item.activo ? 'Desactivar actividad' : 'Activar actividad'} aria-label={`${item.activo ? 'Desactivar' : 'Activar'} ${item.nombre}`} onClick={() => setItemTarget({ plan: planes.find((plan) => plan.id === expanded)!, item: { id: item.id, activo: item.activo, version: item.version, nombre: item.nombre } })}><Power size={16} aria-hidden="true" /></Button>
        </td>}
      </tr>)}</tbody></table></div>}
    </Card>}

    <Modal open={showPlanForm} title="Nuevo plan sanitario" onClose={() => setShowPlanForm(false)} description="Registra un plan sanitario de la empresa.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); if (!errorFin) crearPlanMut.mutate(event.currentTarget) }}>
        <Field label="Nombre" required><input name="nombre" required maxLength={160} autoComplete="off" /></Field>
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required value={planFechaInicio} onChange={(event) => setPlanFechaInicio(event.target.value)} /></Field>
        <Field label="Fecha de fin" error={errorFin}><input name="fechaFin" type="date" min={planFechaInicio || undefined} value={planFechaFin} onChange={(event) => setPlanFechaFin(event.target.value)} /></Field>
        <div className="form-full"><Field label="Descripción"><textarea name="descripcion" rows={3} maxLength={2000} placeholder="Objetivo del plan…" /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crearPlanMut.isPending} disabled={Boolean(errorFin)}>Crear plan</Button></div>
      </form>
    </Modal>

    <Modal open={showItemForm} title={editingItem ? `Editar «${editingItem.nombre}»` : 'Agregar actividad al plan'} onClose={cerrarFormularioItem} wide description="Define qué debe hacerse, a quién, cuándo y con qué instrucciones — sin inventario de medicamentos.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); if (!formularioInvalido) crearItemMut.mutate(event.currentTarget) }}>
        <h4 className="form-full">Datos generales</h4>
        <Field label="Nombre de la actividad" required><input name="nombre" required maxLength={200} defaultValue={editingItem?.nombre} autoComplete="off" /></Field>
        <Field label="Tipo de actividad" required><select name="tipoActividad" required defaultValue={editingItem?.tipoActividad ?? 'VACUNACION'}>{(Object.keys(TIPO_ACTIVIDAD_LABELS) as Array<keyof typeof TIPO_ACTIVIDAD_LABELS>).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}</select></Field>
        <Field label="Clasificación regulatoria" required hint="Por qué existe esta actividad en el plan"><select name="origenRegulatorio" required value={origen} onChange={(event) => setOrigen(event.target.value as OrigenRegulatorio | '')}><option value="" disabled>Selecciona…</option>{(Object.keys(ORIGEN_REGULATORIO_LABELS) as OrigenRegulatorio[]).map((o) => <option key={o} value={o}>{ORIGEN_REGULATORIO_LABELS[o]}</option>)}</select></Field>
        <div className="checkbox-with-hint">
          <label className="checkbox-line"><input type="checkbox" checked={obligatorioEfectivo} disabled={obligatorioPorOrigen} onChange={(event) => setObligatorio(event.target.checked)} /> Actividad obligatoria</label>
          {obligatorioPorOrigen && <span className="field-hint">Siempre obligatoria por ser exigida por SENASAG.</span>}
        </div>
        <div className="form-full"><Field label="Descripción"><textarea name="descripcion" rows={2} maxLength={2000} defaultValue={editingItem?.descripcion} /></Field></div>

        <h4 className="form-full">Cuándo se programa</h4>
        <Field label="Modalidad" required>
          <select value={modalidad} onChange={(event) => setModalidad(event.target.value as ModalidadActividad)}>
            {(Object.keys(MODALIDAD_ACTIVIDAD_LABELS) as ModalidadActividad[]).map((m) => <option key={m} value={m}>{MODALIDAD_ACTIVIDAD_LABELS[m]}</option>)}
          </select>
        </Field>
        {modalidad === 'POR_EDAD' && <>
          <Field label="Edad objetivo" required error={edadObjetivoError} hint="Se programa cuando el animal cumple esta edad, contada desde su nacimiento."><input type="number" inputMode="numeric" min="1" step="1" required value={edadObjetivoValor} onChange={(event) => setEdadObjetivoValor(event.target.value)} /></Field>
          <Field label="Unidad de la edad objetivo">
            <select value={edadObjetivoUnidad} onChange={(event) => setEdadObjetivoUnidad(event.target.value as UnidadEdad)}>
              <option value="DIAS">Días</option>
              <option value="MESES">Meses</option>
              <option value="ANIOS">Años</option>
            </select>
          </Field>
          <Field label="Ventana anticipada (días)"><input name="ventanaAnticipadaDias" type="number" inputMode="numeric" min="0" defaultValue={configEdadItem?.ventanaAnticipadaDias ?? 0} /></Field>
          <Field label="Ventana posterior (días)"><input name="ventanaPosteriorDias" type="number" inputMode="numeric" min="0" defaultValue={configEdadItem?.ventanaPosteriorDias ?? 0} /></Field>
          <div className="form-full checkbox-with-hint">
            <label className="checkbox-line"><input type="checkbox" checked={unaVezEnLaVida} onChange={(event) => setUnaVezEnLaVida(event.target.checked)} /> Una sola vez en la vida del animal</label>
            <span className="field-hint">{unaVezEnLaVida
              ? 'Si el animal ya recibió esta actividad (incluso en una versión anterior o declarada por un proveedor), no se vuelve a programar.'
              : 'Se programa aunque el animal ya la haya recibido antes, por ejemplo al crear una nueva versión de la actividad.'}</span>
          </div>
          <label className="checkbox-line"><input name="excluirEdadEstimada" type="checkbox" defaultChecked={configEdadItem?.politicaEdadEstimada === 'EXCLUIR'} /> Excluir animales con fecha de nacimiento estimada (no confirmada)</label>
        </>}
        {modalidad === 'PERIODICA' && <>
          <Field label="Frecuencia" required><input name="frecuenciaValor" type="number" inputMode="numeric" min="1" required defaultValue={configPeriodicaItem?.frecuenciaValor ?? 90} /></Field>
          <Field label="Unidad de frecuencia"><select name="frecuenciaUnidad" defaultValue={configPeriodicaItem?.frecuenciaUnidad ?? 'DIAS'}><option value="DIAS">Días</option><option value="SEMANAS">Semanas</option><option value="MESES">Meses</option><option value="ANIOS">Años</option></select></Field>
          <Field label="Se calcula desde"><select name="referenciaCalculo" defaultValue={configPeriodicaItem?.referenciaCalculo ?? 'ULTIMA_APLICACION'}>{(Object.keys(REFERENCIA_CALCULO_PERIODICA_LABELS) as Array<keyof typeof REFERENCIA_CALCULO_PERIODICA_LABELS>).map((r) => <option key={r} value={r}>{REFERENCIA_CALCULO_PERIODICA_LABELS[r]}</option>)}</select></Field>
          <Field label="Tolerancia anticipada (días)"><input name="toleranciaAnticipadaDias" type="number" inputMode="numeric" min="0" defaultValue={configPeriodicaItem?.toleranciaAnticipadaDias ?? 0} /></Field>
          <Field label="Tolerancia posterior (días)"><input name="toleranciaPosteriorDias" type="number" inputMode="numeric" min="0" defaultValue={configPeriodicaItem?.toleranciaPosteriorDias ?? 0} /></Field>
        </>}
        {modalidad === 'FECHA_PROGRAMADA' && <>
          <Field label="Fecha y hora programada" required hint="Se programa una sola vez, en esa fecha. Para repetirla usa la modalidad «Periódica»."><input name="fechaProgramada" type="datetime-local" required defaultValue={configFechaItem ? fechaHoraLocal(configFechaItem.fechaProgramada) : ''} /></Field>
        </>}
        {modalidad === 'POR_HALLAZGO' && <div className="form-full">
          <fieldset>
            <legend>Hallazgos que activan esta actividad</legend>
            {TIPOS_HALLAZGO_CATALOGO.map((tipo) => <label key={tipo.valor} className="checkbox-line">
              <input type="checkbox" checked={tiposHallazgoSeleccionados.includes(tipo.valor)}
                onChange={(event) => setTiposHallazgoSeleccionados((prev) => event.target.checked ? [...prev, tipo.valor] : prev.filter((v) => v !== tipo.valor))} />
              {tipo.label}
            </label>)}
          </fieldset>
          {hallazgoError && <span className="field-error" role="alert">{hallazgoError}</span>}
          <Field label="Plazo para resolverlo (días)"><input name="plazoDias" type="number" inputMode="numeric" min="0" defaultValue={configHallazgoItem?.plazoDias ?? undefined} /></Field>
          <label className="checkbox-line"><input name="requiereValidacionVeterinaria" type="checkbox" defaultChecked={configHallazgoItem?.requiereValidacionVeterinaria} /> Requiere validación veterinaria</label>
        </div>}

        <h4 className="form-full">A quién aplica</h4>
        <fieldset className="form-full">
          <legend>Categorías</legend>
          <span className="field-hint">{categoriasSeleccionadas.length === 0 ? 'Sin selección: aplica a todas las categorías.' : `${categoriasSeleccionadas.length} seleccionada(s).`}</span>
          <div className="check-grid">
            {catalogs?.categories.map((categoria) => <label key={categoria.id} className="checkbox-line">
              <input type="checkbox" checked={categoriasSeleccionadas.includes(categoria.id)}
                onChange={(event) => setCategoriasSeleccionadas((prev) => event.target.checked ? [...prev, categoria.id] : prev.filter((id) => id !== categoria.id))} />
              {categoria.nombre}
            </label>)}
          </div>
        </fieldset>
        <Field label="Sexo aplicable"><select name="sexoAplicable" defaultValue={editingItem?.sexoAplicable ?? ''}><option value="">Ambos</option><option value="MACHO">Macho</option><option value="HEMBRA">Hembra</option></select></Field>
        <div className="form-full age-range-panel">
          <div className="age-range-heading">
            <div><strong>Edad de los animales elegibles</strong><span>Limita a qué animales corresponde según su edad en la fecha de la actividad. Usa la unidad que te resulte más cómoda.</span></div>
            <Field label="Unidad de edad">
              <select value={unidadEdad} onChange={(event) => setUnidadEdad(event.target.value as UnidadEdad)}>
                <option value="DIAS">Días</option>
                <option value="MESES">Meses</option>
                <option value="ANIOS">Años</option>
              </select>
            </Field>
          </div>
          <div className="age-range-fields">
            <Field label="Desde" error={edadMinInvalida ? edadError : undefined} hint={`Ej. 2 ${unidadEdad === 'DIAS' ? 'días' : unidadEdad === 'MESES' ? 'meses' : 'años'}`}>
              <input type="number" inputMode="numeric" min="0" step="1" value={edadMinValor} onChange={(event) => setEdadMinValor(event.target.value)} placeholder="Sin mínimo" />
            </Field>
            <Field label="Hasta" error={edadMaxInvalida || Boolean(errorRangoEdad(edadMinDias, edadMaxDias)) ? edadError : undefined} hint={sinEdadMaxima ? 'La actividad no tendrá edad máxima.' : 'Debe ser igual o mayor que la edad inicial.'}>
              <input type="number" inputMode="numeric" min="0" step="1" value={edadMaxValor} disabled={sinEdadMaxima} onChange={(event) => setEdadMaxValor(event.target.value)} placeholder="Sin máximo" />
            </Field>
          </div>
          <label className="checkbox-line"><input type="checkbox" checked={sinEdadMaxima} onChange={(event) => setSinEdadMaxima(event.target.checked)} /> Sin límite máximo</label>
          <label className="checkbox-line"><input name="permiteEdadDesconocida" type="checkbox" defaultChecked={editingItem?.permiteEdadDesconocida} /> Incluir animales con edad desconocida (no se validará el rango de edad para ellos)</label>
          <div className={`age-range-feedback ${edadError || edadObjetivoError ? 'age-range-feedback-error' : ''}`} role={edadError || edadObjetivoError ? 'alert' : 'status'}>{edadError ?? edadObjetivoError ?? equivalenciaEdad}</div>
          <input type="hidden" name="edadMinDias" value={edadMinDias ?? ''} />
          <input type="hidden" name="edadMaxDias" value={edadMaxDias ?? ''} />
        </div>

        <h4 className="form-full">Medicamento recomendado (informativo, no es inventario)</h4>
        <Field label="Producto recomendado"><input name="productoRecomendadoTexto" maxLength={300} placeholder="Ej. Ivermectina 1%…" defaultValue={editingItem?.productoRecomendadoTexto} autoComplete="off" /></Field>
        <Field label="Principio activo"><input name="principioActivo" maxLength={200} defaultValue={editingItem?.principioActivo} autoComplete="off" /></Field>
        <div className="form-full"><Field label="Instrucciones del veterinario"><textarea name="instruccionesVeterinario" rows={2} maxLength={2000} defaultValue={editingItem?.instruccionesVeterinario} placeholder="Ej. Pesar al animal o usar el último peso medido vigente." /></Field></div>

        <h4 className="form-full">Dosis</h4>
        <Field label="Tipo de cálculo" required>
          <select value={dosisTipoCalculo} onChange={(event) => setDosisTipoCalculo(event.target.value as TipoCalculoDosis)}>
            {(Object.keys(TIPO_CALCULO_DOSIS_LABELS) as TipoCalculoDosis[]).map((tipo) => <option key={tipo} value={tipo}>{TIPO_CALCULO_DOSIS_LABELS[tipo]}</option>)}
          </select>
        </Field>
        {dosisTipoCalculo !== 'NO_APLICA' && <>
          <Field label="Cantidad" required={dosisTipoCalculo === 'POR_PESO'} error={erroresDosisForm.cantidad}><input name="dosisCantidad" type="number" inputMode="decimal" min="0.001" step="0.001" value={dosisCantidadValor} onChange={(event) => setDosisCantidadValor(event.target.value)} /></Field>
          <Field label="Unidad"><select name="dosisUnidad" value={dosisUnidad} onChange={(event) => setDosisUnidad(event.target.value as UnidadDosis | '')}><option value="">Selecciona…</option>{(Object.keys(UNIDAD_DOSIS_LABELS) as UnidadDosis[]).map((u) => <option key={u} value={u}>{UNIDAD_DOSIS_LABELS[u]}</option>)}</select></Field>
          <Field label="Detalle de unidad (si elegiste «otra»)"><input name="dosisUnidadDetalle" maxLength={100} defaultValue={editingItem?.dosisUnidadDetalle} /></Field>
        </>}
        {dosisTipoCalculo === 'POR_PESO' && <>
          <Field label="Peso de referencia (kg)" required hint={pesoReferenciaFija !== undefined ? `Lo fija la unidad «${UNIDAD_DOSIS_LABELS[dosisUnidad as UnidadDosis]}».` : 'Peso al que corresponde la cantidad. Ej.: 1 ml cada 50 kg → cantidad 1 y peso de referencia 50.'}><input name="dosisPesoReferenciaKg" type="number" inputMode="decimal" min="0.1" step="0.1" required readOnly={pesoReferenciaFija !== undefined} value={pesoReferenciaEfectivo} onChange={(event) => setPesoReferenciaValor(event.target.value)} /></Field>
          <Field label="Dosis mínima (opcional)" error={erroresDosisForm.minima}><input name="dosisMinima" type="number" inputMode="decimal" min="0.001" step="0.001" value={dosisMinimaValor} onChange={(event) => setDosisMinimaValor(event.target.value)} /></Field>
          <Field label="Dosis máxima (opcional)" error={erroresDosisForm.maxima}><input name="dosisMaxima" type="number" inputMode="decimal" min="0.001" step="0.001" value={dosisMaximaValor} onChange={(event) => setDosisMaximaValor(event.target.value)} /></Field>
        </>}

        <h4 className="form-full">Vía y lugar de aplicación</h4>
        <Field label="Vía de administración">
          <select value={viaCodigo} onChange={(event) => setViaCodigo(event.target.value as ViaAdministracion | '')}>
            <option value="">Selecciona…</option>
            {(Object.keys(VIA_ADMINISTRACION_LABELS) as ViaAdministracion[]).map((via) => <option key={via} value={via}>{VIA_ADMINISTRACION_LABELS[via]}</option>)}
          </select>
        </Field>
        {viaCodigo === 'OTRA' && <Field label="Detalle de la vía" required error={erroresVia.detalleVia}><input name="viaAdministracionDetalle" maxLength={100} value={viaDetalle} onChange={(event) => setViaDetalle(event.target.value)} /></Field>}
        <Field label="Lugar anatómico" error={erroresVia.lugar}>
          <select value={lugar} onChange={(event) => setLugar(event.target.value as LugarAplicacion | '')}>
            <option value="">Selecciona…</option>
            {(Object.keys(LUGAR_APLICACION_LABELS) as LugarAplicacion[]).map((l) => <option key={l} value={l}>{LUGAR_APLICACION_LABELS[l]}</option>)}
          </select>
        </Field>
        {lugar === 'OTRO' && <Field label="Detalle del lugar" required error={erroresVia.detalleLugar}><input name="lugarAplicacionDetalle" maxLength={100} value={lugarDetalle} onChange={(event) => setLugarDetalle(event.target.value)} /></Field>}

        <h4 className="form-full">Alertas</h4>
        <Field label="Hora prevista de ejecución" hint="Hora de la actividad en America/La_Paz." required>
          <input name="horaEjecucion" type="time" required defaultValue={editingItem?.horaEjecucion ?? '08:00'} />
        </Field>
        <Field label="Días de alerta"><input name="diasAlerta" type="number" inputMode="numeric" min="0" defaultValue={editingItem?.diasAlerta ?? 0} /></Field>
        <Field label="Horarios de aviso" hint="Hasta cinco horas, separadas por coma. Ejemplo: 06:00, 07:00, 08:00" required>
          <input name="horariosAviso" required placeholder="06:00, 07:00, 08:00" defaultValue={editingItem?.horariosAviso?.join(', ') ?? '08:00'} />
        </Field>

        {editingItem && <div className="form-full">
          <Field label="Motivo del cambio (si la actividad ya fue usada, se creará una nueva versión)" required={versionRequerida} error={versionRequerida ? 'Esta actividad ya se usó: indica el motivo del cambio.' : undefined}><input name="motivoVersion" maxLength={500} /></Field>
          <Field label="Vigente desde" required={versionRequerida} error={versionRequerida ? 'Indica desde cuándo rige la nueva versión.' : undefined}><input name="fechaVigencia" type="datetime-local" /></Field>
        </div>}

        <div className="form-actions"><Button type="submit" loading={crearItemMut.isPending} disabled={formularioInvalido}>{editingItem ? 'Guardar cambios' : 'Agregar actividad'}</Button></div>
      </form>
    </Modal>

    <ConfirmDialog
      open={Boolean(stateTarget)}
      title={`${stateTarget?.estado === 'ANULADO' ? 'Anular' : stateTarget?.estado === 'FINALIZADO' ? 'Finalizar' : 'Activar'} plan sanitario`}
      confirmLabel="Confirmar"
      variant={stateTarget?.estado === 'ANULADO' ? 'danger' : 'warning'}
      loading={cambiarEstado.isPending}
      error={cambiarEstado.error}
      onClose={() => setStateTarget(null)}
      onConfirm={() => { if (stateTarget && !cambiarEstado.isPending) cambiarEstado.mutate({ id: stateTarget.plan.id, estado: stateTarget.estado, version: stateTarget.plan.version }) }}
    >
      {stateTarget && <p className="muted">El plan «{stateTarget.plan.nombre}» pasará al estado <strong>{stateTarget.estado}</strong>.</p>}
      {stateTarget && ['FINALIZADO', 'ANULADO'].includes(stateTarget.estado) && <p className="muted">Se cancelarán los eventos pendientes de sus actividades y el plan ya no admitirá cambios.</p>}
      {stateTarget?.estado === 'ACTIVO' && (conflictos.data?.length ?? 0) > 0 && <div className="alert alert-warning" role="status">
        Ya {conflictos.data!.length === 1 ? 'hay un plan activo' : `hay ${conflictos.data!.length} planes activos`} con actividades del mismo tipo en este alcance: <strong>{conflictos.data!.map((plan) => plan.nombre).join(', ')}</strong>. Puedes activar este plan igualmente, pero las actividades podrían solaparse.
      </div>}
    </ConfirmDialog>

    <ConfirmDialog
      open={Boolean(itemTarget)}
      title={itemTarget?.item.activo ? 'Desactivar actividad' : 'Activar actividad'}
      confirmLabel={itemTarget?.item.activo ? 'Desactivar' : 'Activar'}
      variant={itemTarget?.item.activo ? 'danger' : 'warning'}
      loading={cambiarItem.isPending}
      error={cambiarItem.error}
      onClose={() => setItemTarget(null)}
      onConfirm={() => { if (itemTarget && !cambiarItem.isPending) cambiarItem.mutate({ planId: itemTarget.plan.id, itemId: itemTarget.item.id, activo: !itemTarget.item.activo, version: itemTarget.item.version }) }}
    >
      {itemTarget && <p className="muted">La actividad «{itemTarget.item.nombre}» pasará a {itemTarget.item.activo ? 'inactiva' : 'activa'}.</p>}
      {itemTarget && <p className="muted">{itemTarget.item.activo ? 'Se cancelarán sus eventos pendientes del calendario.' : 'Se restaurarán sus eventos pendientes que todavía no hayan vencido.'}</p>}
    </ConfirmDialog>
  </div>
}
