import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, ArrowLeft, ArrowRight, CheckCircle2, ShieldAlert } from 'lucide-react'
import type { Lote, Membresia } from '@/features/lotes/api'
import { listLotes } from '@/features/lotes/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
import {
  cancelarPreparacionMovimientoLote,
  confirmarMovimientoLote,
  prepararMovimientoLote,
} from '@/features/movimientolote/api'
import type {
  AccionLote,
  AutorizacionInput,
  MiembroPreparacionLote,
  PreparacionMovimientoLote,
  ResultadoMovimientoLote,
} from '@/features/movimientolote/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface MoverLoteWizardProps {
  lote: Lote
  miembrosActivos: Membresia[]
  onClose: () => void
  onSuccess: (resultado: ResultadoMovimientoLote) => void
}

const accionLabel: Record<AccionLote, string> = {
  MANTENER_LOTE: 'Mantener el mismo lote',
  CAMBIAR_A_LOTE_EXISTENTE: 'Cambiar a un lote existente',
  CREAR_NUEVO_LOTE: 'Crear un lote nuevo',
  DEJAR_SIN_LOTE: 'Dejar sin lote',
}

function nowLocal() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export function MoverLoteWizard({ lote, miembrosActivos, onClose, onSuccess }: MoverLoteWizardProps) {
  const client = useQueryClient()
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1)
  const [search, setSearch] = useState('')
  const [seleccionInicial, setSeleccionInicial] = useState<Set<string>>(() => new Set(miembrosActivos.map((m) => m.animalId)))
  const [destinoPropiedadId, setDestinoPropiedadId] = useState(lote.propiedadId)
  const [destinoPotreroId, setDestinoPotreroId] = useState('')
  const [accionLote, setAccionLote] = useState<AccionLote>('MANTENER_LOTE')
  const [loteDestinoId, setLoteDestinoId] = useState('')
  const [nuevoLoteNombre, setNuevoLoteNombre] = useState('')
  const [nuevoLoteCodigo, setNuevoLoteCodigo] = useState('')
  const [nuevoLoteDescripcion, setNuevoLoteDescripcion] = useState('')
  const [fechaEfectiva, setFechaEfectiva] = useState(() => nowLocal())
  const [motivo, setMotivo] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [preparacion, setPreparacion] = useState<PreparacionMovimientoLote | null>(null)
  const [seleccionFinal, setSeleccionFinal] = useState<Set<string>>(new Set())
  const [autorizaciones, setAutorizaciones] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const catalogs = useQuery({
    queryKey: ['mover-lote-catalogs'],
    queryFn: async () => {
      const [propiedades, potreros] = await Promise.all([listPropiedades(), listAllPotreros()])
      return { propiedades, potreros }
    },
  })
  const lotesDestino = useQuery({
    queryKey: ['mover-lote-destinos', destinoPropiedadId],
    queryFn: () => listLotes({ estado: 'ACTIVO', page: 0, size: 200 }),
    enabled: accionLote === 'CAMBIAR_A_LOTE_EXISTENTE',
  })

  const miembrosFiltrados = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return miembrosActivos
    return miembrosActivos.filter((m) => (m.animalCodigo ?? '').toLowerCase().includes(q) || (m.animalNombre ?? '').toLowerCase().includes(q))
  }, [miembrosActivos, search])

  const potrerosDestino = catalogs.data?.potreros.filter((p) => p.activo && p.propiedadId === destinoPropiedadId) ?? []
  const lotesDestinoOpciones = (lotesDestino.data?.content ?? [])
    .filter((l) => l.propiedadId === destinoPropiedadId && l.estado === 'ACTIVO' && l.id !== lote.id)

  const toggleInicial = (animalId: string) => setSeleccionInicial((prev) => {
    const next = new Set(prev)
    if (next.has(animalId)) next.delete(animalId); else next.add(animalId)
    return next
  })

  const preparar = useMutation({
    mutationFn: () => prepararMovimientoLote(lote.id, {
      destinoPropiedadId,
      destinoPotreroId,
      accionLote,
      loteDestinoId: accionLote === 'CAMBIAR_A_LOTE_EXISTENTE' ? loteDestinoId : undefined,
      nuevoLote: accionLote === 'CREAR_NUEVO_LOTE'
        ? { nombre: nuevoLoteNombre, codigo: nuevoLoteCodigo || undefined, descripcion: nuevoLoteDescripcion || undefined }
        : undefined,
      fechaEfectiva: new Date(fechaEfectiva).toISOString(),
      motivo: motivo || undefined,
      observaciones: observaciones || undefined,
      modalidadDeclarada: seleccionInicial.size >= miembrosActivos.length ? 'LOTE_COMPLETO' : 'SELECCION_PARCIAL',
    }),
    onSuccess: (prep) => {
      const elegibles = new Set(prep.miembros.filter((m) => m.elegible).map((m) => m.animalId))
      setSeleccionFinal(new Set([...seleccionInicial].filter((id) => elegibles.has(id))))
      setPreparacion(prep)
      setStep(3)
    },
  })

  const confirmar = useMutation({
    mutationFn: () => {
      if (!preparacion) return Promise.reject(new Error('No hay una preparación vigente.'))
      const autorizacionesInput: AutorizacionInput[] = Object.entries(autorizaciones)
        .filter(([, motivoAutorizacion]) => motivoAutorizacion.trim().length > 0)
        .map(([key, motivoAutorizacion]) => {
          const [animalId, tipoRestriccion] = key.split('|')
          return { animalId, tipoRestriccion, motivo: motivoAutorizacion }
        })
      return confirmarMovimientoLote(preparacion.id, {
        version: preparacion.version,
        animalIds: [...seleccionFinal],
        autorizaciones: autorizacionesInput,
      })
    },
    onSuccess: async (resultado) => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['lote-miembros'] }),
        client.invalidateQueries({ queryKey: ['lote'] }),
        client.invalidateQueries({ queryKey: ['lotes'] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
        client.invalidateQueries({ queryKey: ['animal'] }),
        client.invalidateQueries({ queryKey: ['animal-timeline'] }),
        client.invalidateQueries({ queryKey: ['movimientos'] }),
      ])
      onSuccess(resultado)
    },
  })

  function cerrar() {
    if (preparacion && preparacion.estado === 'VIGENTE') void cancelarPreparacionMovimientoLote(preparacion.id).catch(() => {})
    onClose()
  }

  function irAPaso2() {
    setFormError(null)
    if (seleccionInicial.size === 0) { setFormError('Selecciona al menos un animal.'); return }
    setStep(2)
  }

  function irAPreparar() {
    setFormError(null)
    if (!destinoPropiedadId || !destinoPotreroId) { setFormError('Indica la propiedad y el potrero de destino.'); return }
    if (accionLote === 'CAMBIAR_A_LOTE_EXISTENTE' && !loteDestinoId) { setFormError('Selecciona el lote de destino.'); return }
    if (accionLote === 'CREAR_NUEVO_LOTE' && !nuevoLoteNombre.trim()) { setFormError('Indica el nombre del nuevo lote.'); return }
    if (accionLote === 'MANTENER_LOTE' && seleccionInicial.size < miembrosActivos.length) {
      setFormError('No puedes mantener el mismo lote en un movimiento parcial: quedarían integrantes activos en el origen. '
        + 'Elige cambiar a un lote existente, crear uno nuevo o dejar sin lote.')
      return
    }
    preparar.mutate()
  }

  const elegibles = preparacion?.miembros.filter((m) => m.elegible) ?? []
  const excluidos = preparacion?.miembros.filter((m) => !m.elegible) ?? []
  const advertenciasPendientes = elegibles.filter((m) => seleccionFinal.has(m.animalId)
    && m.restricciones.some((r) => r.severidad === 'ADVERTENCIA' && !(autorizaciones[`${m.animalId}|${r.tipo}`]?.trim())))
  const loteQuedaDividido = preparacion != null && (excluidos.length > 0 || seleccionFinal.size < elegibles.length);

  return (
    <Modal open title="Mover lote" onClose={cerrar} wide>
      <div className="page-stack">
        <div className="tabs" role="tablist" aria-label="Pasos de mover lote">
          {(['Integrantes', 'Destino', 'Validación', 'Confirmación'] as const).map((label, i) => (
            <span key={label} className={`tab-button ${step === i + 1 ? 'active' : ''}`} aria-current={step === i + 1}>
              {i + 1}. {label}
            </span>
          ))}
        </div>
        {formError && <Alert tone="danger">{formError}</Alert>}
        {(preparar.error || confirmar.error) && <Alert tone="danger">{normalizeApiError(preparar.error ?? confirmar.error).message}</Alert>}

        {step === 1 && <div className="page-stack">
          <p className="muted">{lote.codigo} · {miembrosActivos.length} miembro(s) activo(s). Selecciona todos o algunos para continuar.</p>
          <div className="filter-heading">
            <input type="search" aria-label="Buscar por código o nombre" placeholder="Buscar por código o nombre…" value={search} onChange={(e) => setSearch(e.target.value)} />
            <span className="muted">{seleccionInicial.size} seleccionado(s)</span>
            <Button type="button" variant="ghost" onClick={() => setSeleccionInicial(new Set(miembrosActivos.map((m) => m.animalId)))}>Seleccionar todos</Button>
            <Button type="button" variant="ghost" onClick={() => setSeleccionInicial(new Set())}>Ninguno</Button>
          </div>
          <div className="table-wrapper"><table><caption className="visually-hidden">Miembros del lote</caption><thead><tr><th scope="col">Selección</th><th scope="col">Animal</th></tr></thead><tbody>
            {miembrosFiltrados.map((m) => <tr key={m.animalId}>
              <td><input type="checkbox" aria-label={`Seleccionar ${m.animalCodigo ?? m.animalId}`} checked={seleccionInicial.has(m.animalId)} onChange={() => toggleInicial(m.animalId)} /></td>
              <td>{m.animalNombre?.trim() || m.animalCodigo || m.animalId}</td>
            </tr>)}
          </tbody></table></div>
          <div className="form-actions"><Button onClick={irAPaso2}>Siguiente<ArrowRight size={16} aria-hidden="true" /></Button></div>
        </div>}

        {step === 2 && <div className="page-stack">
          <div className="form-grid">
            <Field label="Propiedad de destino" required>
              <select value={destinoPropiedadId} onChange={(e) => { setDestinoPropiedadId(e.target.value); setDestinoPotreroId('') }}>
                <option value="">Selecciona…</option>
                {catalogs.data?.propiedades.filter((p) => p.activo).map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </select>
            </Field>
            <Field label="Potrero de destino" required hint="Debe pertenecer a la propiedad de destino.">
              <select value={destinoPotreroId} onChange={(e) => setDestinoPotreroId(e.target.value)}>
                <option value="">Selecciona…</option>
                {potrerosDestino.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </select>
            </Field>
            <Field label="Acción sobre el lote" required>
              <select value={accionLote} onChange={(e) => setAccionLote(e.target.value as AccionLote)}>
                {(Object.keys(accionLabel) as AccionLote[]).map((a) => <option key={a} value={a}>{accionLabel[a]}</option>)}
              </select>
            </Field>
            <Field label="Fecha y hora efectiva" required>
              <input type="datetime-local" value={fechaEfectiva} max={nowLocal()} onChange={(e) => setFechaEfectiva(e.target.value)} />
            </Field>
            {accionLote === 'CAMBIAR_A_LOTE_EXISTENTE' && <Field label="Lote de destino" required>
              <select value={loteDestinoId} onChange={(e) => setLoteDestinoId(e.target.value)}>
                <option value="">Selecciona…</option>
                {lotesDestinoOpciones.map((l) => <option key={l.id} value={l.id}>{l.codigo} · {l.nombre}</option>)}
              </select>
            </Field>}
            {accionLote === 'CREAR_NUEVO_LOTE' && <>
              <Field label="Nombre del nuevo lote" required><input value={nuevoLoteNombre} onChange={(e) => setNuevoLoteNombre(e.target.value)} maxLength={160} /></Field>
              <Field label="Código (opcional)"><input value={nuevoLoteCodigo} onChange={(e) => setNuevoLoteCodigo(e.target.value)} maxLength={60} /></Field>
              <div className="form-full"><Field label="Descripción"><textarea rows={2} value={nuevoLoteDescripcion} onChange={(e) => setNuevoLoteDescripcion(e.target.value)} maxLength={1000} /></Field></div>
            </>}
            <Field label="Motivo"><input value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={1000} /></Field>
            <div className="form-full"><Field label="Observaciones"><textarea rows={2} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} maxLength={2000} /></Field></div>
          </div>
          {accionLote === 'MANTENER_LOTE' && seleccionInicial.size < miembrosActivos.length && <Alert tone="warning">
            Seleccionaste menos animales que el total del lote: el lote quedará dividido y no podrá mantener su identidad completa.
          </Alert>}
          <div className="form-actions">
            <Button variant="ghost" onClick={() => setStep(1)}><ArrowLeft size={16} aria-hidden="true" />Atrás</Button>
            <Button loading={preparar.isPending} onClick={irAPreparar}>Validar<ArrowRight size={16} aria-hidden="true" /></Button>
          </div>
        </div>}

        {step === 3 && preparacion && <div className="page-stack">
          <dl className="detail-list">
            <div><dt>Encontrados</dt><dd>{preparacion.totalEncontrados}</dd></div>
            <div><dt>Elegibles</dt><dd>{preparacion.totalElegibles}</dd></div>
            <div><dt>Excluidos</dt><dd>{preparacion.totalExcluidos}</dd></div>
            <div><dt>Seleccionados a mover</dt><dd>{seleccionFinal.size}</dd></div>
          </dl>
          {loteQuedaDividido && <Alert tone="info">Este movimiento dejará integrantes activos en el lote de origen: el resultado será un movimiento parcial (lote dividido), no un traslado completo.</Alert>}
          {excluidos.length > 0 && <div>
            <h4>Excluidos ({excluidos.length})</h4>
            <div className="table-wrapper"><table><caption className="visually-hidden">Animales excluidos</caption><thead><tr><th scope="col">Animal</th><th scope="col">Motivo</th></tr></thead><tbody>
              {excluidos.map((m) => <tr key={m.animalId}><td>{m.nombre?.trim() || m.codigo}</td><td>{m.motivoExclusion}</td></tr>)}
            </tbody></table></div>
          </div>}
          <div>
            <h4>Elegibles ({elegibles.length})</h4>
            <div className="table-wrapper"><table><caption className="visually-hidden">Animales elegibles</caption><thead><tr><th scope="col">Selección</th><th scope="col">Animal</th><th scope="col">Restricciones</th></tr></thead><tbody>
              {elegibles.map((m) => <tr key={m.animalId}>
                <td><input type="checkbox" aria-label={`Mover a ${m.codigo ?? m.animalId}`} checked={seleccionFinal.has(m.animalId)} onChange={() => setSeleccionFinal((prev) => { const next = new Set(prev); if (next.has(m.animalId)) next.delete(m.animalId); else next.add(m.animalId); return next })} /></td>
                <td>{m.nombre?.trim() || m.codigo}</td>
                <td>{restriccionesCelda(m, seleccionFinal.has(m.animalId), autorizaciones, setAutorizaciones)}</td>
              </tr>)}
            </tbody></table></div>
          </div>
          {advertenciasPendientes.length > 0 && <Alert tone="warning"><ShieldAlert size={16} aria-hidden="true" /> Hay advertencias sanitarias sin autorizar; indica el motivo en cada una para poder continuar.</Alert>}
          <div className="form-actions">
            <Button variant="ghost" onClick={() => setStep(2)}><ArrowLeft size={16} aria-hidden="true" />Atrás</Button>
            <Button disabled={seleccionFinal.size === 0 || advertenciasPendientes.length > 0} onClick={() => setStep(4)}>Continuar<ArrowRight size={16} aria-hidden="true" /></Button>
          </div>
        </div>}

        {step === 4 && preparacion && <div className="page-stack">
          <Alert tone="info">
            <div style={{ display: 'flex', gap: '.5rem', alignItems: 'flex-start' }}>
              <AlertTriangle size={18} aria-hidden="true" />
              <div>Se moverán {seleccionFinal.size} animal(es) de {lote.codigo} hacia el destino indicado. Esta acción queda registrada y auditada.</div>
            </div>
          </Alert>
          <dl className="detail-list">
            <div><dt>Animales a mover</dt><dd>{seleccionFinal.size}</dd></div>
            <div><dt>Permanecen en el origen</dt><dd>{elegibles.length - seleccionFinal.size + excluidos.length}</dd></div>
            <div><dt>Acción sobre el lote</dt><dd>{accionLabel[accionLote]}</dd></div>
          </dl>
          <div className="form-actions">
            <Button variant="ghost" disabled={confirmar.isPending} onClick={() => setStep(3)}><ArrowLeft size={16} aria-hidden="true" />Atrás</Button>
            <Button loading={confirmar.isPending} disabled={confirmar.isPending} onClick={() => confirmar.mutate()}>
              <CheckCircle2 size={16} aria-hidden="true" />Confirmar movimiento
            </Button>
          </div>
        </div>}

        {(preparar.isPending) && <LoadingState message="Preparando el movimiento…" />}
      </div>
    </Modal>
  )
}

function restriccionesCelda(
  m: MiembroPreparacionLote,
  seleccionado: boolean,
  autorizaciones: Record<string, string>,
  setAutorizaciones: (updater: (prev: Record<string, string>) => Record<string, string>) => void,
) {
  if (m.restricciones.length === 0) return <span className="muted">—</span>
  return <div className="page-stack">
    {m.restricciones.map((r) => {
      const key = `${m.animalId}|${r.tipo}`
      return <div key={r.tipo}>
        <span className={`status-badge ${r.severidad === 'ADVERTENCIA' ? 'status-en_desarrollo' : 'status-inactivo'}`}>{r.severidad}</span> {r.mensaje}
        {seleccionado && r.severidad === 'ADVERTENCIA' && <input
          aria-label={`Motivo de autorización para ${m.codigo ?? m.animalId}: ${r.tipo}`}
          placeholder="Motivo de la autorización…"
          value={autorizaciones[key] ?? ''}
          onChange={(e) => setAutorizaciones((prev) => ({ ...prev, [key]: e.target.value }))}
        />}
      </div>
    })}
  </div>
}
