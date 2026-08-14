import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  estadoServicioBadge,
  ESTADO_SERVICIO_LABELS,
  registrarServicio,
  TIPO_SERVICIO_LABELS,
  toIso,
  type CeloResponse,
  type PageResponse,
  type ServicioResponse,
} from '@/features/reproduccion/api'
import type { ReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface ServiciosPanelProps {
  servicios: PageResponse<ServicioResponse>
  celos: CeloResponse[]
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

export function ServiciosPanel({ servicios, celos, isLoading, error, catalogs, refresh }: ServiciosPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const [showForm, setShowForm] = useState(false)
  const [hembraId, setHembraId] = useState('')
  const [tipoServicio, setTipoServicio] = useState('')
  const [propiedadId, setPropiedadId] = useState('')
  const [potreroId, setPotreroId] = useState('')

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarServicio({
        hembraId: String(data.get('hembraId')),
        celoId: String(data.get('celoId') || '') || undefined,
        fechaServicio: toIso(String(data.get('fechaServicio'))),
        tipoServicio: String(data.get('tipoServicio')) as ServicioResponse['tipoServicio'],
        machoId: String(data.get('machoId') || '') || undefined,
        codigoSemen: String(data.get('codigoSemen') || '') || undefined,
        proveedorSemen: String(data.get('proveedorSemen') || '') || undefined,
        tecnicoId: String(data.get('tecnicoId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        propiedadId: propiedadId || undefined,
        potreroId: potreroId || undefined,
        loteId: String(data.get('loteId') || '') || undefined,
        clienteUuid: crypto.randomUUID(),
      })
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const seleccionarHembra = (id: string) => {
    setHembraId(id)
    const animal = catalogs?.hembras.find((item) => item.id === id)
    setPropiedadId(animal?.propiedadActualId ?? '')
    setPotreroId(animal?.potreroActualId ?? '')
  }

  const errorVisible = error ?? crear.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Servicios</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar servicio</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando servicios…" />}
      {!isLoading && servicios.content.length === 0 && <EmptyState title="No hay servicios registrados" description="Registra el primer servicio (monta o inseminación)." />}
      {servicios.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Servicios</caption><thead><tr><th scope="col">Hembra</th><th scope="col">Fecha</th><th scope="col">Tipo</th><th scope="col">Macho / semen</th><th scope="col">Intento</th><th scope="col">Estado</th></tr></thead><tbody>{servicios.content.map((servicio) => <tr key={servicio.id}>
        <td><strong>{servicio.codigoAnimal}</strong>{servicio.nombreAnimal ? ` · ${servicio.nombreAnimal}` : ''}</td>
        <td>{new Date(servicio.fechaServicio).toLocaleString('es-BO')}</td>
        <td className="table-secondary">{TIPO_SERVICIO_LABELS[servicio.tipoServicio]}</td>
        <td className="table-secondary">{servicio.nombreMacho ?? servicio.codigoSemen ?? '—'}</td>
        <td>#{servicio.numeroIntento}</td>
        <td><span className={`status-badge status-badge-${estadoServicioBadge(servicio.estado)}`}>{ESTADO_SERVICIO_LABELS[servicio.estado]}</span></td>
      </tr>)}</tbody></table></div>}
      {servicios.content.length > 0 && <div className="mobile-only">{servicios.content.map((servicio) => <div key={servicio.id} className="mobile-entity-card">
        <div><strong>{servicio.codigoAnimal}</strong><p className="muted">{new Date(servicio.fechaServicio).toLocaleString('es-BO')} · {TIPO_SERVICIO_LABELS[servicio.tipoServicio]}</p><p className="muted">{servicio.nombreMacho ?? servicio.codigoSemen ?? '—'} · Intento #{servicio.numeroIntento} · {ESTADO_SERVICIO_LABELS[servicio.estado]}</p></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar servicio" onClose={() => setShowForm(false)} description="Registra un servicio de monta o inseminación.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Hembra" required><select name="hembraId" required value={hembraId} onChange={(event) => seleccionarHembra(event.target.value)}><option value="">Selecciona…</option>{catalogs?.hembras.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Celo asociado"><select name="celoId"><option value="">Sin asociar</option>{celos.filter((item) => !hembraId || item.animalId === hembraId).map((celo) => <option key={celo.id} value={celo.id}>Celo {new Date(celo.fechaDeteccion).toLocaleString('es-BO')}</option>)}</select></Field>
        <Field label="Fecha y hora del servicio" required><input name="fechaServicio" type="datetime-local" required /></Field>
        <Field label="Tipo de servicio" required><select name="tipoServicio" required value={tipoServicio} onChange={(event) => setTipoServicio(event.target.value)}><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_SERVICIO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        {tipoServicio === 'MONTA_NATURAL' && <Field label="Macho"><select name="machoId"><option value="">Selecciona…</option>{catalogs?.machos.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>}
        {tipoServicio === 'INSEMINACION_ARTIFICIAL' && <>
          <Field label="Código de semen"><input name="codigoSemen" maxLength={100} autoComplete="off" placeholder="Ej. BAY01-234…" /></Field>
          <Field label="Proveedor de semen"><input name="proveedorSemen" maxLength={160} /></Field>
        </>}
        <Field label="Técnico"><select name="tecnicoId"><option value="">Sin técnico</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <Field label="Propiedad"><select name="propiedadId" value={propiedadId} onChange={(event) => setPropiedadId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Potrero"><select name="potreroId" value={potreroId} onChange={(event) => setPotreroId(event.target.value)}><option value="">Sin especificar</option>{catalogs?.paddocks.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Lote"><select name="loteId"><option value="">Sin especificar</option>{catalogs?.lots.filter((item) => item.propiedadId === propiedadId || !propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Registrar servicio</Button></div>
      </form>
    </Modal>
  </div>
}
