import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus, Play, History, CheckCircle2 } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  activarTratamiento,
  crearTratamiento,
  ESTADO_TRATAMIENTO_LABELS,
  finalizarTratamiento,
  marcarAtrasadas,
  type CrearTratamientoInput,
  type DetalleTratamientoInput,
  type Tratamiento,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { AplicacionesModal } from '@/features/sanidad/components/AplicacionesModal'
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
  frecuenciaHoras: string
  duracionDias: string
  viaAdministracion: string
  retiroCarneDias: string
  retiroLecheDias: string
}

let rowKey = 0

export function TratamientosPanel({ tratamientos, isLoading, error, catalogs, refresh }: TratamientosPanelProps) {
  const { can } = useAuth()
  const canCrear = can('SANIDAD_TRATAMIENTO_CREAR')
  const [showForm, setShowForm] = useState(false)
  const [detalles, setDetalles] = useState<DetalleRow[]>(() => [{ key: rowKey++, dosis: '', unidadDosis: '', frecuenciaHoras: '', duracionDias: '', viaAdministracion: '', retiroCarneDias: '0', retiroLecheDias: '0' }])
  const [aplicacionesDe, setAplicacionesDe] = useState<Tratamiento | null>(null)
  const [finalizando, setFinalizando] = useState<Tratamiento | null>(null)

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearTratamientoInput = {
        animalId: String(data.get('animalId')),
        fechaInicio: String(data.get('fechaInicio')),
        fechaFinEstimada: String(data.get('fechaFinEstimada')),
        diagnostico: String(data.get('diagnostico') || '') || undefined,
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        detalles: detalles.map((fila): DetalleTratamientoInput => ({
          dosis: Number(fila.dosis),
          unidadDosis: fila.unidadDosis,
          frecuenciaHoras: Number(fila.frecuenciaHoras),
          duracionDias: Number(fila.duracionDias),
          viaAdministracion: fila.viaAdministracion || undefined,
          retiroCarneDias: Number(fila.retiroCarneDias) || 0,
          retiroLecheDias: Number(fila.retiroLecheDias) || 0,
        })),
      }
      return crearTratamiento(input)
    },
    onSuccess: () => { setShowForm(false); refresh() },
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
      {tratamientos.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Tratamientos</caption><thead><tr><th scope="col">Animal</th><th scope="col">DiagnÃ³stico</th><th scope="col">Inicio</th><th scope="col">Fin estimado</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{tratamientos.map((tratamiento) => <tr key={tratamiento.id}>
        <td><strong>{catalogs?.animalLabel(tratamiento.animalId)}</strong></td>
        <td className="table-secondary">{tratamiento.diagnostico ?? 'â€”'}</td>
        <td>{new Date(tratamiento.fechaInicio).toLocaleDateString('es-BO')}</td>
        <td>{new Date(tratamiento.fechaFinEstimada).toLocaleDateString('es-BO')}</td>
        <td><span className={`status-badge status-badge-${tratamiento.estado === 'ACTIVO' ? 'confirmed' : tratamiento.estado === 'FINALIZADO' ? 'valid' : tratamiento.estado === 'ANULADO' ? 'annulled' : 'pending'}`}>{ESTADO_TRATAMIENTO_LABELS[tratamiento.estado]}</span></td>
        <td><div className="inline-actions">
          {tratamiento.estado === 'BORRADOR' && <Button variant="secondary" onClick={() => activar.mutate(tratamiento)} loading={activar.isPending && activar.variables?.id === tratamiento.id}><Play size={16} aria-hidden="true" />Activar</Button>}
          {['ACTIVO', 'BORRADOR', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setAplicacionesDe(tratamiento)}><History size={16} aria-hidden="true" />Aplicaciones</Button>}
          {['ACTIVO', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setFinalizando(tratamiento)}><CheckCircle2 size={16} aria-hidden="true" />Finalizar</Button>}
        </div></td>
      </tr>)}</tbody></table></div>}
      {tratamientos.length > 0 && <div className="mobile-only">{tratamientos.map((tratamiento) => <div key={tratamiento.id} className="mobile-entity-card">
        <div><strong>{catalogs?.animalLabel(tratamiento.animalId)}</strong><p className="muted">{tratamiento.diagnostico ?? 'Sin diagnÃ³stico'}</p><p className="muted">{new Date(tratamiento.fechaInicio).toLocaleDateString('es-BO')} Â· {ESTADO_TRATAMIENTO_LABELS[tratamiento.estado]}</p></div>
        <div className="inline-actions">
          {tratamiento.estado === 'BORRADOR' && <Button variant="secondary" onClick={() => activar.mutate(tratamiento)} loading={activar.isPending && activar.variables?.id === tratamiento.id}><Play size={16} aria-hidden="true" />Activar</Button>}
          {['ACTIVO', 'BORRADOR', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setAplicacionesDe(tratamiento)}><History size={16} aria-hidden="true" />Aplicaciones</Button>}
          {['ACTIVO', 'SUSPENDIDO'].includes(tratamiento.estado) && <Button variant="ghost" onClick={() => setFinalizando(tratamiento)}><CheckCircle2 size={16} aria-hidden="true" />Finalizar</Button>}
        </div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Nuevo tratamiento" onClose={() => setShowForm(false)} wide description="Registra el tratamiento con su protocolo de dosificaciÃ³n.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required><select name="animalId" required><option value="">Seleccionaâ€¦</option>{catalogs?.animals.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} Â· ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required /></Field>
        <Field label="Fecha fin estimada" required><input name="fechaFinEstimada" type="date" required /></Field>
        <Field label="Veterinario"><select name="veterinarioId"><option value="">Sin veterinario</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <div className="form-full"><Field label="DiagnÃ³stico"><input name="diagnostico" maxLength={2000} autoComplete="off" /></Field></div>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-full"><div className="form-section-title">Protocolo de dosificaciÃ³n</div></div>
        {detalles.map((fila) => <div key={fila.key} className="form-grid form-full">
          <Field label="Dosis" required><input type="number" inputMode="decimal" min="0.001" step="0.001" required value={fila.dosis} onChange={(event) => actualizarDetalle(fila.key, 'dosis', event.target.value)} /></Field>
          <Field label="Unidad de dosis" required><input maxLength={30} required placeholder="mL, ccâ€¦" value={fila.unidadDosis} onChange={(event) => actualizarDetalle(fila.key, 'unidadDosis', event.target.value)} /></Field>
          <Field label="Frecuencia (horas)" required><input type="number" inputMode="numeric" min="1" required value={fila.frecuenciaHoras} onChange={(event) => actualizarDetalle(fila.key, 'frecuenciaHoras', event.target.value)} /></Field>
          <Field label="DuraciÃ³n (dÃ­as)" required><input type="number" inputMode="numeric" min="1" required value={fila.duracionDias} onChange={(event) => actualizarDetalle(fila.key, 'duracionDias', event.target.value)} /></Field>
          <Field label="VÃ­a de administraciÃ³n"><input maxLength={60} placeholder="IM, SCâ€¦" value={fila.viaAdministracion} onChange={(event) => actualizarDetalle(fila.key, 'viaAdministracion', event.target.value)} /></Field>
          <Field label="Retiro carne (dÃ­as)"><input type="number" inputMode="numeric" min="0" value={fila.retiroCarneDias} onChange={(event) => actualizarDetalle(fila.key, 'retiroCarneDias', event.target.value)} /></Field>
          <Field label="Retiro leche (dÃ­as)"><input type="number" inputMode="numeric" min="0" value={fila.retiroLecheDias} onChange={(event) => actualizarDetalle(fila.key, 'retiroLecheDias', event.target.value)} /></Field>
          <div className="form-actions"><Button type="button" variant="ghost" onClick={() => setDetalles((prev) => prev.filter((item) => item.key !== fila.key))}>Quitar protocolo</Button></div>
        </div>)}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={() => setDetalles((prev) => [...prev, { key: rowKey++, dosis: '', unidadDosis: '', frecuenciaHoras: '', duracionDias: '', viaAdministracion: '', retiroCarneDias: '0', retiroLecheDias: '0' }])}>Agregar otro protocolo</Button>
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
