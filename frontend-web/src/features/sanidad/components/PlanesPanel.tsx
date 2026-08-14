import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDown, ChevronRight, Pencil, Plus, Power } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  cambiarEstadoItem,
  cambiarEstadoPlan,
  crearPlan,
  crearPlanItem,
  listPlanItems,
  TIPO_ACTIVIDAD_LABELS,
  type CrearItemInput,
  type CrearPlanInput,
  type EstadoPlan,
  type PlanSanitario,
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
import { convertirEdadADias, errorRangoEdad, type UnidadEdad } from '@/features/sanidad/ageRange'

interface PlanesPanelProps {
  planes: PlanSanitario[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
}

export function PlanesPanel({ planes, isLoading, error, catalogs, refresh }: PlanesPanelProps) {
  const client = useQueryClient()
  const { can } = useAuth()
  const canAdmin = can('SANIDAD_PLAN_ADMINISTRAR')
  const [showPlanForm, setShowPlanForm] = useState(false)
  const [showItemForm, setShowItemForm] = useState(false)
  const [expanded, setExpanded] = useState<string | null>(null)
  const [edadMinValor, setEdadMinValor] = useState('')
  const [edadMaxValor, setEdadMaxValor] = useState('')
  const [unidadEdad, setUnidadEdad] = useState<UnidadEdad>('MESES')
  const [sinEdadMaxima, setSinEdadMaxima] = useState(false)
  const [stateTarget, setStateTarget] = useState<{ plan: PlanSanitario; estado: EstadoPlan } | null>(null)
  const [itemTarget, setItemTarget] = useState<{ plan: PlanSanitario; item: { id: string; activo: boolean; version: number; nombre: string } } | null>(null)

  const items = useQuery({
    queryKey: ['sanidad-items', expanded],
    queryFn: () => (expanded ? listPlanItems(expanded) : Promise.resolve([])),
    enabled: Boolean(expanded),
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

  const crearItemMut = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearItemInput = {
        tipoActividad: String(data.get('tipoActividad')) as CrearItemInput['tipoActividad'],
        productoRecomendadoTexto: String(data.get('productoRecomendadoTexto') || '') || undefined,
        categoriaAnimalId: String(data.get('categoriaAnimalId') || '') || undefined,
        sexoAplicable: (String(data.get('sexoAplicable') || '') || undefined) as CrearItemInput['sexoAplicable'],
        edadMinDias: Number(data.get('edadMinDias')) || undefined,
        edadMaxDias: Number(data.get('edadMaxDias')) || undefined,
        dosis: Number(data.get('dosis')) || undefined,
        unidadDosis: String(data.get('unidadDosis') || '') || undefined,
        frecuenciaDias: Number(data.get('frecuenciaDias')) || undefined,
        diasAlerta: Number(data.get('diasAlerta')) || 0,
        viaAdministracion: String(data.get('viaAdministracion') || '') || undefined,
        obligatorio: data.get('obligatorio') === 'on',
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
  const equivalenciaEdad = edadMinDias === undefined && edadMaxDias === undefined
    ? 'Sin restricción por edad.'
    : edadMinDias !== undefined && edadMaxDias !== undefined
      ? `Se guardará el rango de ${edadMinDias} a ${edadMaxDias} días.`
      : edadMinDias !== undefined
        ? `Se aplicará desde los ${edadMinDias} días, sin límite máximo.`
        : `Se aplicará hasta los ${edadMaxDias} días.`

  function abrirFormularioItem(planId: string) {
    setExpanded(planId)
    setEdadMinValor('')
    setEdadMaxValor('')
    setUnidadEdad('MESES')
    setSinEdadMaxima(false)
    crearItemMut.reset()
    setShowItemForm(true)
  }

  function cerrarFormularioItem() {
    setShowItemForm(false)
    setEdadMinValor('')
    setEdadMaxValor('')
    setSinEdadMaxima(false)
  }

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Planes sanitarios</h2>
        {canAdmin && <Button onClick={() => setShowPlanForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Nuevo plan</Button>}
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
            {plan.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => abrirFormularioItem(plan.id)} disabled={!catalogs}><Pencil size={16} aria-hidden="true" />Agregar actividad</Button>}
          </span>}
        </li>)}
      </ul>}
    </Card>
    {expanded && <Card>
      <h3>Actividades de {planes.find((plan) => plan.id === expanded)?.nombre}</h3>
      {items.isPending && <LoadingState message="Cargando actividades…" />}
      {items.error && <Alert tone="danger">{normalizeApiError(items.error).message}</Alert>}
      {items.data?.length === 0 && <p className="muted">Este plan todavía no tiene actividades.</p>}
      {items.data && items.data.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Actividades del plan</caption><thead><tr><th scope="col">Actividad</th><th scope="col">Producto</th><th scope="col">Aplicable a</th><th scope="col">Dosis</th><th scope="col">Frecuencia</th><th scope="col">Alerta</th><th scope="col">Obligatorio</th><th scope="col">Estado</th>{canAdmin && <th scope="col">Acciones</th>}</tr></thead><tbody>{items.data.map((item) => <tr key={item.id}>
        <td><strong>{TIPO_ACTIVIDAD_LABELS[item.tipoActividad]}</strong><span className="table-secondary">{item.viaAdministracion ?? '—'}</span></td>
        <td>{item.productoRecomendadoTexto ?? (item.productoId ? item.productoId.slice(0, 8) : '—')}</td>
        <td>{[item.sexoAplicable, catalogs?.categories.find((cat) => cat.id === item.categoriaAnimalId)?.nombre, item.edadMinDias !== undefined || item.edadMaxDias !== undefined ? `${item.edadMinDias ?? 0}–${item.edadMaxDias ?? '∞'} días` : null].filter(Boolean).join(' · ') || 'Todos'}</td>
        <td>{item.dosis !== undefined ? `${item.dosis} ${item.unidadDosis ?? ''}` : '—'}</td>
        <td>{item.frecuenciaDias ? `Cada ${item.frecuenciaDias} días` : '—'}</td>
        <td>{item.diasAlerta > 0 ? `Desde ${item.diasAlerta} días antes` : '—'}</td>
        <td>{item.obligatorio ? 'Sí' : 'No'}</td>
        <td><span className="status-badge">{item.activo ? 'ACTIVO' : 'INACTIVO'}</span></td>
        {canAdmin && <td><Button variant="ghost" onClick={() => setItemTarget({ plan: planes.find((plan) => plan.id === expanded)!, item: { id: item.id, activo: item.activo, version: item.version, nombre: TIPO_ACTIVIDAD_LABELS[item.tipoActividad] } })}><Power size={16} aria-hidden="true" />{item.activo ? 'Desactivar' : 'Activar'}</Button></td>}
      </tr>)}</tbody></table></div>}
      {items.data && items.data.length > 0 && <div className="mobile-only">{items.data.map((item) => <div key={item.id} className="mobile-entity-card"><div><strong>{TIPO_ACTIVIDAD_LABELS[item.tipoActividad]}</strong><p className="muted">{item.productoRecomendadoTexto ?? 'Producto sin especificar'}</p><p className="muted">{item.dosis !== undefined ? `${item.dosis} ${item.unidadDosis ?? ''}` : '—'} · {item.frecuenciaDias ? `cada ${item.frecuenciaDias} días` : '—'}</p></div>{canAdmin && <Button variant="ghost" onClick={() => setItemTarget({ plan: planes.find((plan) => plan.id === expanded)!, item: { id: item.id, activo: item.activo, version: item.version, nombre: TIPO_ACTIVIDAD_LABELS[item.tipoActividad] } })}><Power size={16} aria-hidden="true" />{item.activo ? 'Desactivar' : 'Activar'}</Button>}</div>)}</div>}
    </Card>}

    <Modal open={showPlanForm} title="Nuevo plan sanitario" onClose={() => setShowPlanForm(false)} description="Registra un plan sanitario de la empresa.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crearPlanMut.mutate(event.currentTarget) }}>
        <Field label="Nombre" required><input name="nombre" required maxLength={160} autoComplete="off" /></Field>
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required /></Field>
        <Field label="Fecha de fin"><input name="fechaFin" type="date" /></Field>
        <div className="form-full"><Field label="Descripción"><textarea name="descripcion" rows={3} maxLength={2000} placeholder="Objetivo del plan…" /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crearPlanMut.isPending}>Crear plan</Button></div>
      </form>
    </Modal>

    <Modal open={showItemForm} title="Agregar actividad al plan" onClose={cerrarFormularioItem} wide description="Define una actividad programada dentro del plan sanitario.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); if (!edadError) crearItemMut.mutate(event.currentTarget) }}>
        <Field label="Tipo de actividad" required><select name="tipoActividad" required defaultValue="VACUNACION">{(Object.keys(TIPO_ACTIVIDAD_LABELS) as Array<keyof typeof TIPO_ACTIVIDAD_LABELS>).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}</select></Field>
        <Field label="Producto recomendado"><input name="productoRecomendadoTexto" maxLength={300} placeholder="Ej. BOVISAN 2 mL…" autoComplete="off" /></Field>
        <Field label="Categoría"><select name="categoriaAnimalId"><option value="">Todas</option>{catalogs?.categories.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}</select></Field>
        <Field label="Sexo aplicable"><select name="sexoAplicable"><option value="">Ambos</option><option value="MACHO">Macho</option><option value="HEMBRA">Hembra</option></select></Field>
        <div className="form-full age-range-panel">
          <div className="age-range-heading">
            <div><strong>Edad de aplicación</strong><span>Indica el rango usando la unidad que te resulte más cómoda.</span></div>
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
          <div className={`age-range-feedback ${edadError ? 'age-range-feedback-error' : ''}`} role={edadError ? 'alert' : 'status'}>{edadError ?? equivalenciaEdad}</div>
          <input type="hidden" name="edadMinDias" value={edadMinDias ?? ''} />
          <input type="hidden" name="edadMaxDias" value={edadMaxDias ?? ''} />
        </div>
        <Field label="Dosis"><input name="dosis" type="number" inputMode="decimal" min="0" step="0.001" /></Field>
        <Field label="Unidad de dosis"><input name="unidadDosis" maxLength={30} placeholder="mL, cc…" autoComplete="off" /></Field>
        <Field label="Frecuencia (días)"><input name="frecuenciaDias" type="number" inputMode="numeric" min="1" /></Field>
        <Field label="Días de alerta"><input name="diasAlerta" type="number" inputMode="numeric" min="0" defaultValue="0" /></Field>
        <Field label="Vía de administración"><input name="viaAdministracion" maxLength={60} placeholder="IM, SC…" autoComplete="off" /></Field>
        <label className="checkbox-line"><input name="obligatorio" type="checkbox" /> Actividad obligatoria</label>
        <div className="form-actions"><Button type="submit" loading={crearItemMut.isPending} disabled={Boolean(edadError)}>Agregar actividad</Button></div>
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
    </ConfirmDialog>
  </div>
}
