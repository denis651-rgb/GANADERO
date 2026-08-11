import { useDeferredValue, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Plus, Search, Trash2 } from 'lucide-react'
import { addAnimales, cerrarLote, getLote, listMembresias, retirarAnimales } from '@/features/lotes/api'
import type { ModoIngreso } from '@/features/lotes/api'
import { listAnimals } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

export function LoteDetailPage() {
  const { id = '' } = useParams()
  const { showToast } = useToast()
  const client = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [addSearch, setAddSearch] = useState('')
  const deferredAddSearch = useDeferredValue(addSearch)
  const [addSelected, setAddSelected] = useState<Set<string>>(new Set())
  const [addFechaIngreso, setAddFechaIngreso] = useState('')
  const [addMotivo, setAddMotivo] = useState('')
  const [addObservacion, setAddObservacion] = useState('')
  const [addModo, setAddModo] = useState<ModoIngreso>('PARCIAL')
  const [showRetiro, setShowRetiro] = useState(false)
  const [retiroSelected, setRetiroSelected] = useState<Set<string>>(new Set())
  const [retiroFechaSalida, setRetiroFechaSalida] = useState('')
  const [retiroMotivo, setRetiroMotivo] = useState('')
  const [showClose, setShowClose] = useState(false)
  const [closeMotivo, setCloseMotivo] = useState('')

  const lote = useQuery({ queryKey: ['lote', id], queryFn: () => getLote(id), enabled: Boolean(id) })
  const miembros = useQuery({ queryKey: ['lote-miembros', id, true], queryFn: () => listMembresias(id, true), enabled: Boolean(id) })
  const historicos = useQuery({ queryKey: ['lote-miembros', id, false], queryFn: () => listMembresias(id, false), enabled: Boolean(id) })
  const catalogs = useQuery({ queryKey: ['lote-catalogs'], queryFn: async () => {
    const [propiedades, animales] = await Promise.all([listPropiedades(), listAnimals({ search: undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 500 })])
    return { propiedades, animales: animales.content }
  } })
  const disponiblesQuery = useQuery({
    queryKey: ['lote-animales-disponibles', deferredAddSearch],
    queryFn: () => listAnimals({ search: deferredAddSearch || undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 200 }),
    enabled: showAdd,
  })

  const add = useMutation({
    mutationFn: () => {
      if (addSelected.size === 0) return Promise.reject(new Error('Selecciona al menos un animal.'))
      return addAnimales(id, {
        animalIds: Array.from(addSelected),
        modo: addModo,
        fechaIngreso: addFechaIngreso ? new Date(addFechaIngreso).toISOString() : undefined,
        motivo: addMotivo || undefined,
        observacion: addObservacion || undefined,
      })
    },
    onSuccess: (result) => {
      if (result.ok) {
        setShowAdd(false)
        setAddSelected(new Set())
        setAddSearch('')
        setAddFechaIngreso('')
        setAddMotivo('')
        setAddObservacion('')
        showToast(`${result.ingresados} animal(es) ingresado(s) al lote.`)
      }
      void client.invalidateQueries({ queryKey: ['lote-miembros', id] })
      void client.invalidateQueries({ queryKey: ['animals'] })
    },
  })
  const retirar = useMutation({
    mutationFn: () => {
      if (retiroSelected.size === 0) return Promise.reject(new Error('Selecciona al menos un animal.'))
      return retirarAnimales(id, {
        animalIds: Array.from(retiroSelected),
        fechaSalida: retiroFechaSalida ? new Date(retiroFechaSalida).toISOString() : undefined,
        motivo: retiroMotivo || undefined,
      })
    },
    onSuccess: (result) => {
      if (result.ok) {
        setShowRetiro(false)
        setRetiroSelected(new Set())
        setRetiroFechaSalida('')
        setRetiroMotivo('')
        showToast(`${result.retirados} animal(es) retirado(s) del lote.`)
      }
      void client.invalidateQueries({ queryKey: ['lote-miembros', id] })
      void client.invalidateQueries({ queryKey: ['animals'] })
    },
  })
  const close = useMutation({
    mutationFn: () => cerrarLote(id, lote.data!.version, closeMotivo || undefined),
    onSuccess: async () => {
      setShowClose(false)
      await client.invalidateQueries({ queryKey: ['lote', id] })
      await client.invalidateQueries({ queryKey: ['lotes'] })
    },
  })
  const error = lote.error ?? miembros.error ?? catalogs.error ?? add.error ?? retirar.error ?? close.error

  const candidatos = useMemo(() => {
    const asignados = new Set(miembros.data?.map((item) => item.animalId) ?? [])
    const property = lote.data?.propiedadId
    return (disponiblesQuery.data?.content ?? []).filter((animal) => !asignados.has(animal.id) && animal.propiedadActualId === property)
  }, [disponiblesQuery.data, miembros.data, lote.data?.propiedadId])

  const toggleAdd = (animalId: string) => setAddSelected((prev) => {
    const next = new Set(prev)
    if (next.has(animalId)) next.delete(animalId)
    else next.add(animalId)
    return next
  })

  const toggleRetiro = (animalId: string) => setRetiroSelected((prev) => {
    const next = new Set(prev)
    if (next.has(animalId)) next.delete(animalId)
    else next.add(animalId)
    return next
  })

  if (lote.isPending) return <LoadingState message="Cargando lote…" />
  if (!lote.data) return <Alert tone="danger">No se encontró el lote solicitado.</Alert>
  const value = lote.data

  return <div className="page-stack">
    <PageHeader eyebrow="Lotes" title={`${value.codigo} · ${value.nombre}`} description={value.descripcion || 'Sin descripción.'} actions={<><Link className="button button-ghost" to="/lotes"><ArrowLeft size={18} aria-hidden="true" />Volver</Link>{value.estado === 'ACTIVO' && <Button variant="danger" onClick={() => setShowClose(true)}>Cerrar lote</Button>}</>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <div className="two-column-grid">
      <Card><dl className="detail-list"><div><dt>Propiedad</dt><dd>{catalogs.data?.propiedades.find((item) => item.id === value.propiedadId)?.nombre ?? '—'}</dd></div><div><dt>Estado</dt><dd>{value.estado}</dd></div><div><dt>Apertura</dt><dd>{new Date(value.fechaApertura).toLocaleDateString('es-BO')}</dd></div><div><dt>Cierre</dt><dd>{value.fechaCierre ? new Date(value.fechaCierre).toLocaleDateString('es-BO') : '—'}</dd></div></dl></Card>
      <Card><h3>Miembros activos</h3>{miembros.isPending && <LoadingState message="Cargando animales…" />}{miembros.data && <p className="muted">{miembros.data.length} animal(es) en el lote.</p>}</Card>
    </div>
    <Card>
      <div className="filter-heading"><span><Plus size={18} />Animales del lote</span>{value.estado === 'ACTIVO' && <div className="row-actions">{retiroSelected.size > 0 && <Button variant="danger" onClick={() => setShowRetiro(true)}>Retirar seleccionados ({retiroSelected.size})</Button>}<Button variant="secondary" onClick={() => setShowAdd(true)}><Plus size={16} />Agregar animales</Button></div>}</div>
      {miembros.isPending && <LoadingState message="Cargando miembros…" />}
      {miembros.data?.length === 0 && <EmptyState title="Lote vacío" description="Agrega animales al lote para empezar a trabajar con ellos." />}
      {miembros.data && miembros.data.length > 0 && <div className="table-wrapper"><table><caption className="visually-hidden">Animales integrantes del lote</caption><thead><tr>{value.estado === 'ACTIVO' && <th scope="col">Selección</th>}<th scope="col">Animal</th><th scope="col">Ingreso</th><th scope="col">Motivo</th><th scope="col">Acciones</th></tr></thead><tbody>{miembros.data.map((item) => {
        const animal = catalogs.data?.animales.find((a) => a.id === item.animalId)
        return <tr key={item.id}>{value.estado === 'ACTIVO' && <td><input type="checkbox" aria-label="Seleccionar animal" checked={retiroSelected.has(item.animalId)} onChange={() => toggleRetiro(item.animalId)} /></td>}<td><strong>{animal?.codigo ?? '—'}</strong>{animal?.nombre ? ` · ${animal.nombre}` : ''}</td><td>{new Date(item.fechaIngreso).toLocaleString('es-BO')}</td><td>{item.motivoIngreso ?? '—'}</td><td>{value.estado === 'ACTIVO' && <Button variant="ghost" aria-label="Retirar animal" onClick={() => { setRetiroSelected(new Set([item.animalId])); setShowRetiro(true) }}><Trash2 size={16} /></Button>}</td></tr>
      })}</tbody></table></div>}
    </Card>
    {historicos.data && historicos.data.length > 0 && <Card><h3>Historial de membresías</h3><div className="table-wrapper"><table><caption className="visually-hidden">Historial de animales del lote</caption><thead><tr><th scope="col">Animal</th><th scope="col">Ingreso</th><th scope="col">Salida</th><th scope="col">Motivo de ingreso</th><th scope="col">Motivo de salida</th></tr></thead><tbody>{historicos.data.filter((item) => item.fechaSalida).map((item) => { const animal = catalogs.data?.animales.find((a) => a.id === item.animalId); return <tr key={item.id}><td><strong>{animal?.codigo ?? '—'}</strong></td><td>{new Date(item.fechaIngreso).toLocaleString('es-BO')}</td><td>{item.fechaSalida ? new Date(item.fechaSalida).toLocaleString('es-BO') : '—'}</td><td>{item.motivoIngreso ?? '—'}</td><td>{item.motivoSalida ?? '—'}</td></tr> })}</tbody></table></div></Card>}
    {value.estado === 'CERRADO' && <Alert tone="info">Este lote está cerrado y no admite más animales.</Alert>}

    <Modal open={showAdd} title="Agregar animales al lote" onClose={() => { if (!add.isPending) setShowAdd(false) }} wide>
      <div className="page-stack">
        <div className="filter-heading"><span className="search-box"><Search size={18} aria-hidden="true" /><input type="search" aria-label="Buscar animales para agregar al lote" value={addSearch} onChange={(event) => { setAddSearch(event.target.value); setAddSelected(new Set()) }} placeholder="Buscar por código o nombre…" /></span><span className="muted">{addSelected.size} seleccionado(s)</span></div>
        {add.isSuccess && !add.data.ok && <Alert tone="danger">{add.data.resultados.filter((r) => r.estado === 'ERROR').map((r) => `${r.mensaje} (${r.animalId})`).join(' · ')}</Alert>}
        {add.error && <Alert tone="danger">{normalizeApiError(add.error).message}</Alert>}
        {disponiblesQuery.isPending && <LoadingState message="Buscando animales…" />}
        {candidatos.length === 0 && !disponiblesQuery.isPending && <EmptyState title="Sin animales disponibles" description="No hay animales activos de esta propiedad fuera del lote." />}
        {candidatos.length > 0 && <div className="checkbox-stack">{candidatos.map((animal) => <label key={animal.id}><input type="checkbox" checked={addSelected.has(animal.id)} onChange={() => toggleAdd(animal.id)} /> {animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</label>)}</div>}
        <div className="form-grid">
          <Field label="Modo"><select value={addModo} onChange={(event) => setAddModo(event.target.value as ModoIngreso)}><option value="PARCIAL">Parcial (procesa el resto)</option><option value="ATOMICO">Atómico (todo o nada)</option></select></Field>
          <Field label="Fecha de ingreso"><input type="datetime-local" value={addFechaIngreso} onChange={(event) => setAddFechaIngreso(event.target.value)} /></Field>
          <Field label="Motivo"><input value={addMotivo} onChange={(event) => setAddMotivo(event.target.value)} maxLength={1000} /></Field>
          <Field label="Observación"><textarea value={addObservacion} onChange={(event) => setAddObservacion(event.target.value)} maxLength={2000} /></Field>
        </div>
        <div className="form-actions"><Button type="button" loading={add.isPending} disabled={addSelected.size === 0} onClick={() => add.mutate()}>Agregar {addSelected.size} animal(es)</Button></div>
      </div>
    </Modal>

    <Modal open={showRetiro} title="Retirar animales del lote" onClose={() => { if (!retirar.isPending) setShowRetiro(false) }}>
      <div className="page-stack">
        <p className="muted">{retiroSelected.size} animal(es) seleccionado(s).</p>
        {retirar.isSuccess && !retirar.data.ok && <Alert tone="danger">{retirar.data.resultados.filter((r) => r.estado === 'ERROR').map((r) => `${r.mensaje} (${r.animalId})`).join(' · ')}</Alert>}
        {retirar.error && <Alert tone="danger">{normalizeApiError(retirar.error).message}</Alert>}
        <div className="form-grid">
          <Field label="Fecha de salida"><input type="datetime-local" value={retiroFechaSalida} onChange={(event) => setRetiroFechaSalida(event.target.value)} /></Field>
          <Field label="Motivo"><input value={retiroMotivo} onChange={(event) => setRetiroMotivo(event.target.value)} maxLength={1000} /></Field>
        </div>
        <div className="form-actions"><Button type="button" variant="danger" loading={retirar.isPending} onClick={() => retirar.mutate()}>Retirar {retiroSelected.size} animal(es)</Button></div>
      </div>
    </Modal>

    <Modal open={showClose} title="Cerrar lote" onClose={() => { if (!close.isPending) setShowClose(false) }}>
      <div className="page-stack">
        {close.error && <Alert tone="danger">{normalizeApiError(close.error).message}</Alert>}
        <Field label="Motivo de cierre"><input value={closeMotivo} onChange={(event) => setCloseMotivo(event.target.value)} maxLength={1000} /></Field>
        <div className="form-actions"><Button type="button" variant="danger" loading={close.isPending} onClick={() => close.mutate()}>Confirmar cierre</Button></div>
      </div>
    </Modal>
  </div>
}
