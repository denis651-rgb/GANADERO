import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Ban, Check, Pencil, Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  actualizarJornada,
  anularJornada,
  crearJornada,
  ESTADO_JORNADA_LABELS,
  TIPO_ACTIVIDAD_LABELS,
  type ActualizarJornadaInput,
  type ConfirmacionJornadaResult,
  type CrearJornadaInput,
  type JornadaSanitaria,
  type TipoActividad,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { JornadaConfirmarModal } from '@/features/sanidad/components/JornadaConfirmarModal'
import { JornadaPrepararModal, type PreparacionJornada } from '@/features/sanidad/components/JornadaPrepararModal'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'

interface JornadasPanelProps {
  jornadas: JornadaSanitaria[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
  /** Atajo desde "Registrar prueba diagnóstica" en el diálogo de validación de movimientos. */
  tipoJornadaSugerida?: TipoActividad
}

interface JornadaFormProps {
  catalogs?: SanidadCatalogs
  jornada: JornadaSanitaria | null
  tipoSugerido?: TipoActividad
  propertyId: string
  setPropertyId: (value: string) => void
  loading: boolean
  onSubmit: (form: HTMLFormElement) => void
}

function JornadaForm({ catalogs, jornada, tipoSugerido, propertyId, setPropertyId, loading, onSubmit }: JornadaFormProps) {
  return <form className="form-grid" onSubmit={(event) => { event.preventDefault(); onSubmit(event.currentTarget) }}>
    <Field label="Tipo de jornada" required><select name="tipoJornada" required defaultValue={jornada?.tipoJornada ?? tipoSugerido ?? 'VACUNACION'}>{(Object.keys(TIPO_ACTIVIDAD_LABELS) as Array<keyof typeof TIPO_ACTIVIDAD_LABELS>).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}</select></Field>
    <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required defaultValue={jornada?.fechaInicio ?? ''} /></Field>
    <Field label="Propiedad" required><select name="propiedadId" required value={propertyId} onChange={(event) => setPropertyId(event.target.value)}><option value="">Selecciona…</option>{catalogs?.properties.filter((item) => item.activo || item.id === jornada?.propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
    <Field label="Potrero"><select name="potreroId" defaultValue={jornada?.potreroId ?? ''}><option value="">Toda la propiedad</option>{catalogs?.paddocks.filter((item) => (item.activo || item.id === jornada?.potreroId) && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
    <Field label="Lote"><select name="loteGanaderoId" defaultValue={jornada?.loteGanaderoId ?? ''}><option value="">Sin lote</option>{catalogs?.lots.filter((item) => item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
    <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} defaultValue={jornada?.observaciones ?? ''} /></Field></div>
    <div className="form-actions"><Button type="submit" loading={loading}>{jornada ? 'Guardar cambios' : 'Crear jornada'}</Button></div>
  </form>
}

function inputFromForm(form: HTMLFormElement, responsableId: string): CrearJornadaInput {
  const data = new FormData(form)
  return {
    tipoJornada: String(data.get('tipoJornada')) as CrearJornadaInput['tipoJornada'],
    fechaInicio: String(data.get('fechaInicio')),
    propiedadId: String(data.get('propiedadId')),
    potreroId: String(data.get('potreroId') || '') || undefined,
    loteGanaderoId: String(data.get('loteGanaderoId') || '') || undefined,
    responsableId,
    observaciones: String(data.get('observaciones') || '') || undefined,
  }
}

export function JornadasPanel({ jornadas, isLoading, error, catalogs, refresh, tipoJornadaSugerida }: JornadasPanelProps) {
  const { can, user } = useAuth()
  const canCrear = can('SANIDAD_JORNADA_CREAR')
  const canConfirmar = can('SANIDAD_JORNADA_CONFIRMAR')
  const [showForm, setShowForm] = useState(Boolean(tipoJornadaSugerida))
  const [editando, setEditando] = useState<JornadaSanitaria | null>(null)
  const [anulando, setAnulando] = useState<JornadaSanitaria | null>(null)
  const [propertyId, setPropertyId] = useState('')
  const [preparando, setPreparando] = useState<JornadaSanitaria | null>(null)
  const [confirmando, setConfirmando] = useState<({ jornada: JornadaSanitaria } & PreparacionJornada) | null>(null)
  const [resultado, setResultado] = useState<ConfirmacionJornadaResult | null>(null)

  const guardar = useMutation({
    mutationFn: ({ form, jornada }: { form: HTMLFormElement; jornada: JornadaSanitaria | null }) => {
      const input = inputFromForm(form, jornada?.responsableId ?? user.id)
      return jornada
        ? actualizarJornada(jornada.id, { ...input, veterinarioId: jornada.veterinarioId, version: jornada.version } as ActualizarJornadaInput)
        : crearJornada(input)
    },
    onSuccess: (jornada, variables) => {
      setShowForm(false)
      setEditando(null)
      refresh()
      if (!variables.jornada && canConfirmar) setPreparando(jornada)
    },
  })
  const anular = useMutation({
    mutationFn: ({ jornada, motivo }: { jornada: JornadaSanitaria; motivo: string }) => anularJornada(jornada.id, { motivo, version: jornada.version }),
    onSuccess: () => { setAnulando(null); refresh() },
  })

  const openNew = () => { setEditando(null); setPropertyId(''); setShowForm(true) }
  const openEdit = (jornada: JornadaSanitaria) => { setEditando(jornada); setPropertyId(jornada.propiedadId); setShowForm(true) }
  const closeForm = () => { if (!guardar.isPending) { setShowForm(false); setEditando(null) } }
  const errorVisible = error ?? guardar.error ?? anular.error

  const actions = (jornada: JornadaSanitaria) => jornada.estado === 'BORRADOR' ? <div className="inline-actions">
    {canCrear && <Button className="jornada-icon-action" variant="ghost" aria-label="Editar jornada" title="Editar jornada" onClick={() => openEdit(jornada)}><Pencil size={19} aria-hidden="true" /></Button>}
    {canConfirmar && <Button className="jornada-icon-action" variant="secondary" aria-label="Preparar y confirmar jornada" title="Preparar y confirmar jornada" onClick={() => setPreparando(jornada)}><Check size={20} strokeWidth={2.5} aria-hidden="true" /></Button>}
    {canCrear && <Button className="jornada-icon-action" variant="danger" aria-label="Cancelar jornada" title="Cancelar jornada" onClick={() => setAnulando(jornada)}><Ban size={19} aria-hidden="true" /></Button>}
  </div> : <span className="muted">Sin acciones</span>

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}><h2>Jornadas sanitarias</h2>{canCrear && <Button className="jornada-icon-action jornada-icon-action-primary" aria-label="Nueva jornada" title="Nueva jornada" onClick={openNew} disabled={!catalogs}><Plus size={22} aria-hidden="true" /></Button>}</div>
      {isLoading && <LoadingState message="Cargando jornadas…" />}
      {!isLoading && jornadas.length === 0 && <EmptyState title="No hay jornadas" description="Registra la primera jornada de vacunación o tratamiento de tu hato." />}
      {jornadas.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Jornadas sanitarias</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Tipo</th><th scope="col">Propiedad</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{jornadas.map((jornada) => <tr key={jornada.id}><td>{new Date(`${jornada.fechaInicio}T00:00:00`).toLocaleDateString('es-BO')}</td><td><strong>{TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}</strong></td><td>{catalogs?.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? jornada.propiedadId.slice(0, 8)}</td><td><span className={`status-badge status-badge-${jornada.estado === 'CONFIRMADA' ? 'confirmed' : jornada.estado === 'ANULADA' ? 'annulled' : 'pending'}`}>{ESTADO_JORNADA_LABELS[jornada.estado]}</span></td><td>{actions(jornada)}</td></tr>)}</tbody></table></div>}
      {jornadas.length > 0 && <div className="mobile-only">{jornadas.map((jornada) => <div key={jornada.id} className="mobile-entity-card"><div><strong>{TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}</strong><p className="muted">{new Date(`${jornada.fechaInicio}T00:00:00`).toLocaleDateString('es-BO')} · {catalogs?.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? 'Propiedad'}</p><p className="muted">{ESTADO_JORNADA_LABELS[jornada.estado]}</p></div>{actions(jornada)}</div>)}</div>}
    </Card>

    {resultado && <Card><h3>Jornada confirmada</h3><p className="muted">Se registraron <strong>{resultado.totalProcesado}</strong> aplicaciones.</p><Button variant="secondary" onClick={() => setResultado(null)}>Entendido</Button></Card>}

    <Modal open={showForm} title={editando ? 'Editar jornada sanitaria' : 'Nueva jornada sanitaria'} onClose={closeForm} wide description={editando ? 'Los cambios eliminan la selección anterior de animales para volver a validar su elegibilidad.' : 'Registra una jornada de aplicación a un grupo de animales.'}>
      <JornadaForm key={editando?.id ?? 'new'} catalogs={catalogs} jornada={editando} tipoSugerido={tipoJornadaSugerida} propertyId={propertyId} setPropertyId={setPropertyId} loading={guardar.isPending} onSubmit={(form) => guardar.mutate({ form, jornada: editando })} />
    </Modal>

    <Modal open={Boolean(anulando)} title="Cancelar jornada sanitaria" onClose={() => { if (!anular.isPending) setAnulando(null) }} description="La jornada quedará anulada y ya no podrá prepararse ni editarse.">
      <form onSubmit={(event) => { event.preventDefault(); if (anulando) anular.mutate({ jornada: anulando, motivo: String(new FormData(event.currentTarget).get('motivo')) }) }}>
        <Field label="Motivo de cancelación" required><textarea name="motivo" rows={4} maxLength={500} required autoFocus /></Field>
        {anular.error && <Alert tone="danger">{normalizeApiError(anular.error).message}</Alert>}
        <div className="form-actions"><Button type="button" variant="secondary" disabled={anular.isPending} onClick={() => setAnulando(null)}>Volver</Button><Button type="submit" variant="danger" loading={anular.isPending}>Cancelar jornada</Button></div>
      </form>
    </Modal>

    {preparando && catalogs && <JornadaPrepararModal jornada={preparando} catalogs={catalogs} onClose={() => setPreparando(null)} onSaved={(preparacion) => { setPreparando(null); setConfirmando({ jornada: preparando, ...preparacion }) }} />}
    {confirmando && catalogs && <JornadaConfirmarModal jornada={confirmando.jornada} animalesSeleccionados={confirmando.seleccionados} planItem={confirmando.planItem} fechaAplicacion={confirmando.fechaAplicacion} onClose={() => setConfirmando(null)} onConfirmado={(res) => { setConfirmando(null); setResultado(res); refresh() }} />}
  </div>
}
