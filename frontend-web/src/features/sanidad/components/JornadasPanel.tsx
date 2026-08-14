import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  crearJornada,
  ESTADO_JORNADA_LABELS,
  TIPO_ACTIVIDAD_LABELS,
  type ConfirmacionJornadaResult,
  type CrearJornadaInput,
  type JornadaSanitaria,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { JornadaConfirmarModal } from '@/features/sanidad/components/JornadaConfirmarModal'
import { JornadaPrepararModal, type PreparacionJornada } from '@/features/sanidad/components/JornadaPrepararModal'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface JornadasPanelProps {
  jornadas: JornadaSanitaria[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
}

export function JornadasPanel({ jornadas, isLoading, error, catalogs, refresh }: JornadasPanelProps) {
  const { can } = useAuth()
  const canCrear = can('SANIDAD_JORNADA_CREAR')
  const canConfirmar = can('SANIDAD_JORNADA_CONFIRMAR')
  const [showForm, setShowForm] = useState(false)
  const [propertyId, setPropertyId] = useState('')
  const [preparando, setPreparando] = useState<JornadaSanitaria | null>(null)
  const [confirmando, setConfirmando] = useState<({ jornada: JornadaSanitaria } & PreparacionJornada) | null>(null)
  const [resultado, setResultado] = useState<ConfirmacionJornadaResult | null>(null)

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearJornadaInput = {
        tipoJornada: String(data.get('tipoJornada')) as CrearJornadaInput['tipoJornada'],
        fechaInicio: String(data.get('fechaInicio')),
        propiedadId: String(data.get('propiedadId')),
        potreroId: String(data.get('potreroId') || '') || undefined,
        loteGanaderoId: String(data.get('loteGanaderoId') || '') || undefined,
        responsableId: String(data.get('responsableId')),
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      }
      return crearJornada(input)
    },
    onSuccess: (jornada) => {
      setShowForm(false)
      refresh()
      if (canConfirmar) setPreparando(jornada)
    },
  })

  const errorVisible = error ?? crear.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Jornadas sanitarias</h2>
        {canCrear && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Nueva jornada</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando jornadas…" />}
      {!isLoading && jornadas.length === 0 && <EmptyState title="No hay jornadas" description="Registra la primera jornada de vacunación o tratamiento de tu hato." />}
      {jornadas.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Jornadas sanitarias</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Tipo</th><th scope="col">Propiedad</th><th scope="col">Responsable</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{jornadas.map((jornada) => <tr key={jornada.id}>
        <td>{new Date(jornada.fechaInicio).toLocaleDateString('es-BO')}</td>
        <td><strong>{TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}</strong></td>
        <td>{catalogs?.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? jornada.propiedadId.slice(0, 8)}</td>
        <td>{catalogs?.userLabel(jornada.responsableId)}</td>
        <td><span className={`status-badge status-badge-${jornada.estado === 'CONFIRMADA' ? 'confirmed' : jornada.estado === 'ANULADA' ? 'annulled' : 'pending'}`}>{ESTADO_JORNADA_LABELS[jornada.estado]}</span></td>
        <td>{(canConfirmar && ['BORRADOR', 'EN_PROCESO'].includes(jornada.estado)) && <Button variant="secondary" onClick={() => setPreparando(jornada)}>Preparar y confirmar</Button>}</td>
      </tr>)}</tbody></table></div>}
      {jornadas.length > 0 && <div className="mobile-only">{jornadas.map((jornada) => <div key={jornada.id} className="mobile-entity-card">
        <div><strong>{TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}</strong><p className="muted">{new Date(jornada.fechaInicio).toLocaleDateString('es-BO')} · {catalogs?.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? 'Propiedad'}</p><p className="muted">{ESTADO_JORNADA_LABELS[jornada.estado]}</p></div>
        {(canConfirmar && ['BORRADOR', 'EN_PROCESO'].includes(jornada.estado)) && <Button variant="secondary" onClick={() => setPreparando(jornada)}>Preparar y confirmar</Button>}
      </div>)}</div>}
    </Card>

    {resultado && <Card><h3>Jornada confirmada</h3><p className="muted">Se registraron <strong>{resultado.totalProcesado}</strong> aplicaciones para la jornada del {new Date(resultado.jornada.fechaInicio).toLocaleDateString('es-BO')}.</p><Button variant="secondary" onClick={() => setResultado(null)}>Entendido</Button></Card>}

    <Modal open={showForm} title="Nueva jornada sanitaria" onClose={() => setShowForm(false)} wide description="Registra una jornada de aplicación a un grupo de animales.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Tipo de jornada" required><select name="tipoJornada" required defaultValue="VACUNACION">{(Object.keys(TIPO_ACTIVIDAD_LABELS) as Array<keyof typeof TIPO_ACTIVIDAD_LABELS>).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}</select></Field>
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required /></Field>
        <Field label="Propiedad" required><select name="propiedadId" required value={propertyId} onChange={(event) => setPropertyId(event.target.value)}><option value="">Selecciona…</option>{catalogs?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId"><option value="">Toda la propiedad</option>{catalogs?.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote"><select name="loteGanaderoId"><option value="">Sin lote</option>{catalogs?.lots.filter((item) => item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Responsable" required><select name="responsableId" required><option value="">Selecciona…</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <Field label="Veterinario"><select name="veterinarioId"><option value="">Sin veterinario</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Crear jornada</Button></div>
      </form>
    </Modal>

    {preparando && catalogs && <JornadaPrepararModal jornada={preparando} catalogs={catalogs} onClose={() => setPreparando(null)} onSaved={(preparacion) => { setPreparando(null); setConfirmando({ jornada: preparando, ...preparacion }) }} />}
    {confirmando && catalogs && <JornadaConfirmarModal jornada={confirmando.jornada} animalesSeleccionados={confirmando.seleccionados} planItem={confirmando.planItem} fechaAplicacion={confirmando.fechaAplicacion} onClose={() => setConfirmando(null)} onConfirmado={(res) => { setConfirmando(null); setResultado(res); refresh() }} />}
  </div>
}
