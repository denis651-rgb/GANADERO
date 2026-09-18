import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Plus, Play, History, CheckCircle2 } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  activarTratamiento,
  crearTratamiento,
  ESTADO_CASO_LABELS,
  ESTADO_TRATAMIENTO_LABELS,
  finalizarTratamiento,
  listCasos,
  marcarAtrasadas,
  type CrearTratamientoInput,
  type DetalleTratamientoInput,
  type Tratamiento,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { AplicacionesModal } from '@/features/sanidad/components/AplicacionesModal'
import { AnimalSearchSelect } from '@/features/reproduccion/components/AnimalSearchSelect'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface TratamientosPanelProps {
  tratamientos: Tratamiento[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
}

interface DetalleRow {
  key: number
  dosis: string
  unidadDosis: string
  productoTexto: string
  frecuenciaHoras: string
  duracionDias: string
  viaAdministracion: string
  retiroCarneDias: string
  retiroLecheDias: string
}

let rowKey = 0

function fechaBolivia(instante: string): string {
  const partes = new Intl.DateTimeFormat('en-US', { timeZone: 'America/La_Paz', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date(instante))
  const valor = (tipo: string) => partes.find((parte) => parte.type === tipo)?.value ?? '01'
  return `${valor('year')}-${valor('month')}-${valor('day')}`
}

function ultimaDosisDia(fechaInicio: string, horaInicio: string, duracion: number, frecuencia: number): string {
  if (!fechaInicio || duracion <= 0) return ''
  const inicio = new Date(`${fechaInicio}T${horaInicio || '00:00'}:00-04:00`).getTime()
  const limite = inicio + duracion * 86400000
  const paso = Math.max(1, frecuencia) * 3600000
  let ultima = inicio
  for (let momento = inicio; momento < limite; momento += paso) ultima = momento
  return fechaBolivia(new Date(ultima).toISOString())
}

export function TratamientosPanel({ tratamientos, isLoading, error, catalogs, refresh }: TratamientosPanelProps) {
  const { can } = useAuth()
  const canCrear = can('SANIDAD_TRATAMIENTO_CREAR')
  const [showForm, setShowForm] = useState(false)
  const [fechaInicio, setFechaInicio] = useState('')
  const [fechaFinEstimada, setFechaFinEstimada] = useState('')
  const [horaInicio, setHoraInicio] = useState('')
  const [animalId, setAnimalId] = useState('')
  const [casoClinicoId, setCasoClinicoId] = useState('')
  const [detalles, setDetalles] = useState<DetalleRow[]>(() => [{ key: rowKey++, dosis: '', unidadDosis: '', productoTexto: '', frecuenciaHoras: '', duracionDias: '', viaAdministracion: '', retiroCarneDias: '0', retiroLecheDias: '0' }])
  const [aplicacionesDe, setAplicacionesDe] = useState<Tratamiento | null>(null)
  const [finalizando, setFinalizando] = useState<Tratamiento | null>(null)

  const finMinimo = fechaInicio ? detalles.reduce((mayor, fila) => {
    const dia = ultimaDosisDia(fechaInicio, horaInicio, Number(fila.duracionDias) || 0, Number(fila.frecuenciaHoras) || 0)
    return dia > mayor ? dia : mayor
  }, '') : ''
  const finMostrado = finMinimo && (!fechaFinEstimada || fechaFinEstimada < finMinimo) ? finMinimo : fechaFinEstimada

  const resetForm = () => {
    setDetalles([{ key: rowKey++, dosis: '', unidadDosis: '', productoTexto: '', frecuenciaHoras: '', duracionDias: '', viaAdministracion: '', retiroCarneDias: '0', retiroLecheDias: '0' }])
    setFechaInicio('')
    setFechaFinEstimada('')
    setHoraInicio('')
    setAnimalId('')
    setCasoClinicoId('')
  }

  const casos = useQuery({ queryKey: ['sanidad-casos', animalId], queryFn: () => listCasos(animalId), enabled: !!animalId })
  const casosAbiertos = (casos.data ?? []).filter((caso) => caso.estado !== 'CERRADO' && caso.estado !== 'ANULADO')

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearTratamientoInput = {
        animalId: String(data.get('animalId')),
        casoClinicoId: casoClinicoId || undefined,
        fechaInicio: String(data.get('fechaInicio')),
        horaInicio: horaInicio || undefined,
        fechaFinEstimada: String(data.get('fechaFinEstimada')),
        diagnostico: String(data.get('diagnostico') || '') || undefined,
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        detalles: detalles.map((fila): DetalleTratamientoInput => ({
          dosis: Number(fila.dosis),
          unidadDosis: fila.unidadDosis,
          productoTexto: fila.productoTexto || undefined,
          frecuenciaHoras: Number(fila.frecuenciaHoras),
          duracionDias: Number(fila.duracionDias),
          viaAdministracion: fila.viaAdministracion || undefined,
          retiroCarneDias: Number(fila.retiroCarneDias) || 0,
          retiroLecheDias: Number(fila.retiroLecheDias) || 0,
        })),
      }
      return crearTratamiento(input)
    },
    onSuccess: () => { setShowForm(false); resetForm(); refresh() },
  })

  const activar = useMutation({
    mutationFn: (tratamiento: Tratamiento) => activarTratamiento(tratamiento.id),
    onSuccess: () => refresh(),
  })

  const finalizar = useMutation({
    mutationFn: (tratamiento: Tratamiento) => finalizarTratamiento(tratamiento.id),
    onSuccess: () => { setFinalizando(null); refresh() },
  })

  const atrasadas = useMutation({
    mutationFn: () => marcarAtrasadas(),
    onSuccess: () => refresh(),
  })

  const actualizarDetalle = (key: number, campo: keyof DetalleRow, valor: string) => {
    setDetalles((prev) => prev.map((fila) => (fila.key === key ? { ...fila, [campo]: valor } : fila)))
  }

  const errorVisible = error ?? crear.error ?? activar.error ?? finalizar.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Tratamientos</h2>
        <div className="inline-actions">
          <Button variant="secondary" onClick={() => atrasadas.mutate()} loading={atrasadas.isPending}>Marcar atrasadas</Button>
          {canCrear && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Nuevo tratamiento</Button>}
        </div>
      </div>
      {isLoading && <LoadingState message="Cargando tratamientosâ€¦" />}
      {!isLoading && tratamientos.length === 0 && <EmptyState title="No hay tratamientos" description="Registra el primer tratamiento para controlar dosis y retiros." />}
      {tratamientos.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Tratamientos</caption><thead><tr><th scope="col">Animal</th><th scope="col">DiagnÃ³stico</th><th scope="col">Inicio</th><th scope="col">Fin estimado</th><th scope="col">Estado</th><th scope="col">Veterinario</th><th scope="col">Acciones</th></tr></thead><tbody>{tratamientos.map((tratamiento) => <tr key={tratamiento.id}>
        <td><strong>{catalogs?.animalLabel(tratamiento.animalId)}</strong></td>
        <td className="table-secondary">{tratamiento.diagnostico ?? 'â€”'}</td>
        <td>{new Date(tratamiento.fechaInicio).toLocaleDateString('es-BO')}</td>
        <td>{new Date(tratamiento.fechaFinEstimada).toLocaleDateString('es-BO')}</td>
        <td><span className={`status-badge status-badge-${tratamiento.estado === 'ACTIVO' ? 'confirmed' : tratamiento.estado === 'FINALIZADO' ? 'valid' : tratamiento.estado === 'ANULADO' ? 'annulled' : 'pending'}`}>{ESTADO_TRATAMIENTO_LABELS[tratamiento.estado]}</span></td>
        <td className="table-secondary">{tratamiento.veterinarioId ?? '—'}</td>
        <td><div className="inline-actions">
          {tratamiento.estado === 'BORRADOR' && <Button variant="secondary" onClick={() => activar.mutate(tratamiento)} loading={activar.isPending && activar.variables?.id === tratamiento.id}><Play size={16} aria-hidden="true" />Activar</Button>}
          {['ACTIVO', 'BORRADOR', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setAplicacionesDe(tratamiento)}><History size={16} aria-hidden="true" />Aplicaciones</Button>}
          {['ACTIVO', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setFinalizando(tratamiento)}><CheckCircle2 size={16} aria-hidden="true" />Finalizar</Button>}
        </div></td>
      </tr>)}</tbody></table></div>}
      {tratamientos.length > 0 && <div className="mobile-only">{tratamientos.map((tratamiento) => <div key={tratamiento.id} className="mobile-entity-card">
        <div><strong>{catalogs?.animalLabel(tratamiento.animalId)}</strong><p className="muted">{tratamiento.diagnostico ?? 'Sin diagnÃ³stico'}</p><p className="muted">{new Date(tratamiento.fechaInicio).toLocaleDateString('es-BO')} Â· {ESTADO_TRATAMIENTO_LABELS[tratamiento.estado]}{tratamiento.veterinarioId ? ` \u00b7 ${tratamiento.veterinarioId}` : ''}</p></div>
        <div className="inline-actions">
          {tratamiento.estado === 'BORRADOR' && <Button variant="secondary" onClick={() => activar.mutate(tratamiento)} loading={activar.isPending && activar.variables?.id === tratamiento.id}><Play size={16} aria-hidden="true" />Activar</Button>}
          {['ACTIVO', 'BORRADOR', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setAplicacionesDe(tratamiento)}><History size={16} aria-hidden="true" />Aplicaciones</Button>}
          {['ACTIVO', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setFinalizando(tratamiento)}><CheckCircle2 size={16} aria-hidden="true" />Finalizar</Button>}
        </div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Nuevo tratamiento" onClose={() => { setShowForm(false); resetForm() }} wide description="Registra el tratamiento con su protocolo de dosificaciÃ³n.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalSearchSelect label="Animal" name="animalId" onChange={(id) => { setAnimalId(id); setCasoClinicoId('') }} />
        {animalId && <Field label="Caso clínico"><select name="casoClinicoId" value={casoClinicoId} onChange={(event) => setCasoClinicoId(event.target.value)} disabled={casosAbiertos.length === 0}>
          <option value="">Sin caso clínico</option>
          {casosAbiertos.map((caso) => <option key={caso.id} value={caso.id}>{ESTADO_CASO_LABELS[caso.estado]} - {new Date(caso.fechaInicio).toLocaleDateString('es-BO')} - {caso.sintomas.slice(0, 40)}</option>)}
        </select></Field>}
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required value={fechaInicio} onChange={(event) => setFechaInicio(event.target.value)} /></Field>
        <Field label="Hora de inicio"><input name="horaInicio" type="time" value={horaInicio} onChange={(event) => setHoraInicio(event.target.value)} /></Field>
        <Field label="Fecha fin estimada" required><input name="fechaFinEstimada" type="date" required min={finMinimo || undefined} value={finMostrado} onChange={(event) => setFechaFinEstimada(event.target.value)} /></Field>
        <Field label="Veterinario responsable"><input name="veterinarioId" maxLength={200} placeholder="Quien indico el tratamiento" /></Field>
        <div className="form-full"><Field label="Diagnostico"><input name="diagnostico" maxLength={2000} autoComplete="off" /></Field></div>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-full"><div className="form-section-title">Protocolo de dosificacion</div></div>
        {detalles.map((fila) => <div key={fila.key} className="form-grid form-full">
          <Field label="Dosis" required><input type="number" inputMode="decimal" min="0.001" step="0.001" required value={fila.dosis} onChange={(event) => actualizarDetalle(fila.key, 'dosis', event.target.value)} /></Field>
          <Field label="Unidad de dosis" required><input maxLength={30} required placeholder="mL, cc" value={fila.unidadDosis} onChange={(event) => actualizarDetalle(fila.key, 'unidadDosis', event.target.value)} /></Field>
          <Field label="Producto aplicado"><input maxLength={200} placeholder="Ej. Oxitetraciclina LA" value={fila.productoTexto} onChange={(event) => actualizarDetalle(fila.key, 'productoTexto', event.target.value)} /></Field>
          <Field label="Frecuencia (horas)" required><input type="number" inputMode="numeric" min="1" required value={fila.frecuenciaHoras} onChange={(event) => actualizarDetalle(fila.key, 'frecuenciaHoras', event.target.value)} /></Field>
          <Field label="Duracion (dias)" required><input type="number" inputMode="numeric" min="1" required value={fila.duracionDias} onChange={(event) => actualizarDetalle(fila.key, 'duracionDias', event.target.value)} /></Field>
          <Field label="Via de administracion"><input maxLength={60} placeholder="IM, M" value={fila.viaAdministracion} onChange={(event) => actualizarDetalle(fila.key, 'viaAdministracion', event.target.value)} /></Field>
          <Field label="Retiro carne (dias)"><input type="number" inputMode="numeric" min="0" value={fila.retiroCarneDias} onChange={(event) => actualizarDetalle(fila.key, 'retiroCarneDias', event.target.value)} /></Field>
          <Field label="Retiro leche (dias)"><input type="number" inputMode="numeric" min="0" value={fila.retiroLecheDias} onChange={(event) => actualizarDetalle(fila.key, 'retiroLecheDias', event.target.value)} /></Field>
          <div className="form-actions"><Button type="button" variant="ghost" onClick={() => setDetalles((prev) => prev.filter((item) => item.key !== fila.key))}>Quitar protocolo</Button></div>
        </div>)}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={() => setDetalles((prev) => [...prev, { key: rowKey++, dosis: '', unidadDosis: '', productoTexto: '', frecuenciaHoras: '', duracionDias: '', viaAdministracion: '', retiroCarneDias: '0', retiroLecheDias: '0' }])}>Agregar otro protocolo</Button>
          <Button type="submit" loading={crear.isPending}>Crear tratamiento</Button>
        </div>
      </form>
    </Modal>

    {aplicacionesDe && catalogs && <AplicacionesModal tratamiento={aplicacionesDe} catalogs={catalogs} onClose={() => setAplicacionesDe(null)} />}

    <ConfirmDialog
      open={Boolean(finalizando)}
      title="Finalizar tratamiento"
      confirmLabel="Finalizar"
      variant="warning"
      loading={finalizar.isPending}
      error={finalizar.error}
      onClose={() => setFinalizando(null)}
      onConfirm={() => { if (finalizando && !finalizar.isPending) finalizar.mutate(finalizando) }}
    >
      {finalizando && <p className="muted">Se marcarÃ¡ como finalizado el tratamiento de {catalogs?.animalLabel(finalizando.animalId)}.</p>}
    </ConfirmDialog>
  </div>
}
