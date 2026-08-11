import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarClock, Edit3, MapPin, RefreshCw } from 'lucide-react'
import { changeAnimalState, getAnimal, getAnimalTimeline, listCategorias, listRazas } from '@/features/animales/api'
import { GenealogiaTab } from '@/features/animales/components/GenealogiaTab'
import { IdentificadoresTab } from '@/features/animales/components/IdentificadoresTab'
import { FotosTab } from '@/features/animales/components/FotosTab'
import type { AnimalState } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']
const criticalStates = new Set<AnimalState>(['VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO'])
type Tab = 'timeline' | 'identificadores' | 'genealogia' | 'fotos'

export function AnimalDetailPage() {
  const { id = '' } = useParams()
  const [tab, setTab] = useState<Tab>('timeline')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroModulo, setFiltroModulo] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [page, setPage] = useState(0)
  const [pendingStateChange, setPendingStateChange] = useState<{ estado: AnimalState; motivo: string } | null>(null)
  const timelineSize = 10
  const client = useQueryClient()
  const { showToast } = useToast()
  const animal = useQuery({ queryKey: ['animal', id], queryFn: () => getAnimal(id), enabled: Boolean(id) })
  const history = useQuery({
    queryKey: ['animal-timeline', id, { tipo: filtroTipo, modulo: filtroModulo, desde, hasta, page, size: timelineSize }],
    queryFn: () => getAnimalTimeline(id, {
      tipo: filtroTipo || undefined,
      modulo: filtroModulo || undefined,
      desde: desde || undefined,
      hasta: hasta || undefined,
      page,
      size: timelineSize,
    }),
    enabled: Boolean(id),
  })
  const catalogs = useQuery({ queryKey: ['animal-detail-catalogs'], queryFn: async () => {
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
    return { breeds, categories, properties, paddocks }
  } })
  const stateMutation = useMutation({
    mutationFn: ({ estado, motivo }: { estado: AnimalState; motivo: string }) =>
      changeAnimalState(id, estado, motivo, animal.data!.version),
    onSuccess: async () => {
      setPendingStateChange(null)
      showToast('Estado actualizado correctamente.')
      await Promise.all([
        client.invalidateQueries({ queryKey: ['animal', id] }),
        client.invalidateQueries({ queryKey: ['animal-timeline', id] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
      ])
    },
  })
  const error = animal.error ?? history.error ?? catalogs.error ?? stateMutation.error

  if (animal.isPending) return <LoadingState message="Cargando animal…" />
  if (!animal.data) return <Alert tone="danger">No se encontró el animal solicitado.</Alert>
  const value = animal.data
  const location = [catalogs.data?.properties.find((item) => item.id === value.propiedadActualId)?.nombre, catalogs.data?.paddocks.find((item) => item.id === value.potreroActualId)?.nombre].filter(Boolean).join(' / ')

  function requestStateChange(form: HTMLFormElement) {
    const data = new FormData(form)
    const next = { estado: String(data.get('estado')) as AnimalState, motivo: String(data.get('motivo')) }
    if (criticalStates.has(next.estado)) {
      setPendingStateChange(next)
      return
    }
    stateMutation.mutate(next)
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Ficha animal" title={`${value.codigo}${value.nombre ? ` · ${value.nombre}` : ''}`} description="Información, estado e historial cronológico." actions={<><Link to="/animales"><Button variant="ghost"><ArrowLeft size={18} />Volver</Button></Link><Link to={`/animales/${id}/editar`}><Button><Edit3 size={18} />Editar</Button></Link></>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <div className="two-column-grid">
      <Card><div className="detail-heading"><h3>Datos principales</h3><span className={`status-badge status-${value.estado.toLowerCase()}`}>{value.estado}</span></div><dl className="detail-list">
        <div><dt>Sexo</dt><dd>{value.sexo}</dd></div><div><dt>Raza</dt><dd>{catalogs.data?.breeds.find((item) => item.id === value.razaPrincipalId)?.nombre ?? '—'}</dd></div>
        <div><dt>Categoría</dt><dd>{catalogs.data?.categories.find((item) => item.id === value.categoriaActualId)?.nombre ?? '—'}</dd></div><div><dt>Propósito</dt><dd>{value.proposito}</dd></div>
        <div><dt>Nacimiento</dt><dd>{value.fechaNacimiento ?? '—'}{value.fechaNacimientoEstimada ? ' (estimada)' : ''}</dd></div><div><dt>Origen</dt><dd>{value.origen}</dd></div>
        <div><dt>Peso al nacer</dt><dd>{value.pesoNacimientoKg ? `${value.pesoNacimientoKg} kg` : '—'}</dd></div><div><dt>Condición corporal</dt><dd>{value.condicionCorporalActual ?? '—'}</dd></div>
      </dl></Card>
      <Card><h3><MapPin size={19} /> Ubicación y observaciones</h3><p><strong>{location || 'Ubicación no disponible'}</strong></p><p className="muted">{value.observaciones || 'Sin observaciones registradas.'}</p></Card>
    </div>
    <Card><h3><RefreshCw size={19} /> Cambiar estado</h3><form className="state-form" onSubmit={(event) => { event.preventDefault(); requestStateChange(event.currentTarget) }}><select name="estado" defaultValue="" required><option value="" disabled>Selecciona el nuevo estado…</option>{states.filter((state) => state !== value.estado).map((state) => <option key={state}>{state}</option>)}</select><input name="motivo" required maxLength={1000} placeholder="Motivo del cambio…" /><Button type="submit" loading={stateMutation.isPending}>Actualizar estado</Button></form></Card>
    <ConfirmDialog
      open={Boolean(pendingStateChange)}
      title="Confirmar cambio de estado"
      confirmLabel="Confirmar cambio"
      variant={pendingStateChange?.estado === 'MUERTO' ? 'danger' : 'warning'}
      loading={stateMutation.isPending}
      error={stateMutation.error}
      onClose={() => setPendingStateChange(null)}
      onConfirm={() => { if (pendingStateChange && !stateMutation.isPending) stateMutation.mutate(pendingStateChange) }}
    >
      {pendingStateChange && <div className="page-stack">
        <dl className="detail-list">
          <div><dt>Animal</dt><dd>{value.codigo}{value.nombre ? ` · ${value.nombre}` : ''}</dd></div>
          <div><dt>Estado actual</dt><dd>{value.estado}</dd></div>
          <div><dt>Nuevo estado</dt><dd>{pendingStateChange.estado}</dd></div>
          <div><dt>Motivo</dt><dd>{pendingStateChange.motivo}</dd></div>
        </dl>
        <Alert tone={pendingStateChange.estado === 'MUERTO' ? 'danger' : 'info'}>Esta operación afectará el estado operativo del animal y quedará registrada en su historial.</Alert>
      </div>}
    </ConfirmDialog>
    <div className="tabs" role="tablist" aria-label="Secciones del animal">
      <button type="button" role="tab" id="tab-timeline" aria-selected={tab === 'timeline'} aria-controls="panel-timeline" className={`tab-button ${tab === 'timeline' ? 'active' : ''}`} onClick={() => setTab('timeline')}><CalendarClock size={17} /> Línea de tiempo</button>
      <button type="button" role="tab" id="tab-identificadores" aria-selected={tab === 'identificadores'} aria-controls="panel-identificadores" className={`tab-button ${tab === 'identificadores' ? 'active' : ''}`} onClick={() => setTab('identificadores')}>Identificadores</button>
      <button type="button" role="tab" id="tab-fotos" aria-selected={tab === 'fotos'} aria-controls="panel-fotos" className={`tab-button ${tab === 'fotos' ? 'active' : ''}`} onClick={() => setTab('fotos')}>Fotografías</button>
      <button type="button" role="tab" id="tab-genealogia" aria-selected={tab === 'genealogia'} aria-controls="panel-genealogia" className={`tab-button ${tab === 'genealogia' ? 'active' : ''}`} onClick={() => setTab('genealogia')}>Genealogía</button>
    </div>
    {tab === 'identificadores' && <div role="tabpanel" id="panel-identificadores" aria-labelledby="tab-identificadores"><IdentificadoresTab animalId={id} animalCodigo={value.codigo} /></div>}
    {tab === 'fotos' && <div role="tabpanel" id="panel-fotos" aria-labelledby="tab-fotos"><FotosTab animalId={id} /></div>}
    {tab === 'genealogia' && <div role="tabpanel" id="panel-genealogia" aria-labelledby="tab-genealogia"><GenealogiaTab animalId={id} /></div>}
    {tab === 'timeline' && <Card role="tabpanel" id="panel-timeline" aria-labelledby="tab-timeline"><h3>Línea de tiempo</h3>
      <div className="filter-heading" style={{ justifyContent: 'flex-start' }}>
        <select aria-label="Filtrar por tipo" value={filtroTipo} onChange={(event) => { setFiltroTipo(event.target.value); setPage(0) }}><option value="">Todos los tipos</option>{[...new Set(history.data?.content.map((event) => event.tipo) ?? [])].map((tipo) => <option key={tipo}>{tipo}</option>)}</select>
        <select aria-label="Filtrar por módulo" value={filtroModulo} onChange={(event) => { setFiltroModulo(event.target.value); setPage(0) }}><option value="">Todos los módulos</option>{[...new Set(history.data?.content.map((event) => event.moduloOrigen) ?? [])].map((modulo) => <option key={modulo}>{modulo}</option>)}</select>
        <input type="date" aria-label="Desde" value={desde} onChange={(event) => { setDesde(event.target.value); setPage(0) }} />
        <input type="date" aria-label="Hasta" value={hasta} onChange={(event) => { setHasta(event.target.value); setPage(0) }} />
      </div>
      {history.isPending && <LoadingState message="Cargando línea de tiempo…" />}
      {history.data?.content.length === 0 && <EmptyState title="Sin eventos" description="Todavía no existen eventos para este animal." />}
      {history.data && history.data.content.length > 0 && <ol className="timeline">{history.data.content.map((event) => {
        const metadata = Object.entries(event.metadata ?? {})
          .filter(([key, val]) => key !== 'origenSync' && (typeof val === 'string' || typeof val === 'number' || typeof val === 'boolean'))
        return <li key={event.id}><span className="timeline-dot" /><div>
          <strong>{event.titulo ?? event.tipo.replaceAll('_', ' ')}</strong>
          <time>{new Date(event.fechaTecnica ?? event.fechaEvento).toLocaleString('es-BO')}</time>
          <span className="table-secondary">Módulo: {event.moduloOrigen}</span>
          {event.origenSync && <span className="status-badge">Sincronizado</span>}
          <p>{event.descripcion ?? 'Sin detalle adicional.'}</p>
          {event.usuarioNombre && <span className="table-secondary">Registrado por: {event.usuarioNombre}</span>}
          {metadata.map(([key, val]) => <span key={key} className="table-secondary">{key.replaceAll('_', ' ')}: {String(val)}</span>)}
        </div></li>
      })}</ol>}
      {history.data && history.data.content.length > 0 && <div className="pagination"><span>Página {history.data.page + 1} de {Math.max(history.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || history.isFetching} onClick={() => setPage((value) => value - 1)}>Anterior</Button><Button variant="ghost" disabled={page + 1 >= history.data.totalPages || history.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente</Button></div></div>}
    </Card>}
  </div>
}
