import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarClock, Edit3, MapPin, RefreshCw } from 'lucide-react'
import { changeAnimalState, getAnimal, getAnimalHistory, listCategorias, listRazas } from '@/features/animales/api'
import { GenealogiaTab } from '@/features/animales/components/GenealogiaTab'
import { IdentificadoresTab } from '@/features/animales/components/IdentificadoresTab'
import type { AnimalState } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']
type Tab = 'historial' | 'identificadores' | 'genealogia'

export function AnimalDetailPage() {
  const { id = '' } = useParams()
  const [tab, setTab] = useState<Tab>('historial')
  const client = useQueryClient()
  const animal = useQuery({ queryKey: ['animal', id], queryFn: () => getAnimal(id), enabled: Boolean(id) })
  const history = useQuery({ queryKey: ['animal-history', id], queryFn: () => getAnimalHistory(id), enabled: Boolean(id) })
  const catalogs = useQuery({ queryKey: ['animal-detail-catalogs'], queryFn: async () => {
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
    return { breeds, categories, properties, paddocks }
  } })
  const stateMutation = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return changeAnimalState(id, String(data.get('estado')) as AnimalState, String(data.get('motivo')), animal.data!.version)
    },
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['animal', id] }),
        client.invalidateQueries({ queryKey: ['animal-history', id] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
      ])
    },
  })
  const error = animal.error ?? history.error ?? catalogs.error ?? stateMutation.error

  if (animal.isPending) return <LoadingState message="Cargando animal…" />
  if (!animal.data) return <Alert tone="danger">No se encontró el animal solicitado.</Alert>
  const value = animal.data
  const location = [catalogs.data?.properties.find((item) => item.id === value.propiedadActualId)?.nombre, catalogs.data?.paddocks.find((item) => item.id === value.potreroActualId)?.nombre].filter(Boolean).join(' / ')

  return <div className="page-stack">
    <PageHeader eyebrow="Ficha animal" title={`${value.codigo}${value.nombre ? ` · ${value.nombre}` : ''}`} description="Información, estado e historial cronológico." actions={<><Link to="/animales"><Button variant="ghost"><ArrowLeft size={18} />Volver</Button></Link><Link to={`/animales/${id}/editar`}><Button><Edit3 size={18} />Editar</Button></Link></>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {stateMutation.isSuccess && <Alert tone="success">Estado actualizado correctamente.</Alert>}
    <div className="two-column-grid">
      <Card><div className="detail-heading"><h3>Datos principales</h3><span className="status-badge">{value.estado}</span></div><dl className="detail-list">
        <div><dt>Sexo</dt><dd>{value.sexo}</dd></div><div><dt>Raza</dt><dd>{catalogs.data?.breeds.find((item) => item.id === value.razaPrincipalId)?.nombre ?? '—'}</dd></div>
        <div><dt>Categoría</dt><dd>{catalogs.data?.categories.find((item) => item.id === value.categoriaActualId)?.nombre ?? '—'}</dd></div><div><dt>Propósito</dt><dd>{value.proposito}</dd></div>
        <div><dt>Nacimiento</dt><dd>{value.fechaNacimiento ?? '—'}{value.fechaNacimientoEstimada ? ' (estimada)' : ''}</dd></div><div><dt>Origen</dt><dd>{value.origen}</dd></div>
        <div><dt>Peso al nacer</dt><dd>{value.pesoNacimientoKg ? `${value.pesoNacimientoKg} kg` : '—'}</dd></div><div><dt>Condición corporal</dt><dd>{value.condicionCorporalActual ?? '—'}</dd></div>
      </dl></Card>
      <Card><h3><MapPin size={19} /> Ubicación y observaciones</h3><p><strong>{location || 'Ubicación no disponible'}</strong></p><p className="muted">{value.observaciones || 'Sin observaciones registradas.'}</p></Card>
    </div>
    <Card><h3><RefreshCw size={19} /> Cambiar estado</h3><form className="state-form" onSubmit={(event) => { event.preventDefault(); stateMutation.mutate(event.currentTarget) }}><select name="estado" defaultValue="" required><option value="" disabled>Selecciona el nuevo estado…</option>{states.filter((state) => state !== value.estado).map((state) => <option key={state}>{state}</option>)}</select><input name="motivo" required maxLength={1000} placeholder="Motivo del cambio" /><Button type="submit" loading={stateMutation.isPending}>Actualizar estado</Button></form></Card>
    <div className="tabs">
      <button type="button" className={`tab-button ${tab === 'historial' ? 'active' : ''}`} onClick={() => setTab('historial')}><CalendarClock size={17} /> Historial</button>
      <button type="button" className={`tab-button ${tab === 'identificadores' ? 'active' : ''}`} onClick={() => setTab('identificadores')}>Identificadores</button>
      <button type="button" className={`tab-button ${tab === 'genealogia' ? 'active' : ''}`} onClick={() => setTab('genealogia')}>Genealogía</button>
    </div>
    {tab === 'identificadores' && <IdentificadoresTab animalId={id} />}
    {tab === 'genealogia' && <GenealogiaTab animalId={id} />}
    {tab === 'historial' && <Card><h3>Historial</h3>{history.isPending && <LoadingState message="Cargando historial…" />}{history.data?.length === 0 && <EmptyState title="Sin eventos" description="Todavía no existen eventos para este animal." />}{history.data && history.data.length > 0 && <ol className="timeline">{history.data.map((event) => <li key={event.id}><span className="timeline-dot" /><div><strong>{event.titulo ?? event.tipo.replaceAll('_', ' ')}</strong><time>{new Date(event.fechaEvento).toLocaleString('es-BO')}</time>{event.moduloOrigen && <span className="table-secondary">Módulo: {event.moduloOrigen}</span>}<p>{event.descripcion ?? event.motivo ?? 'Sin detalle adicional.'}</p>{event.estadoAnterior && event.estadoNuevo && event.estadoAnterior !== event.estadoNuevo && <span className="table-secondary">{event.estadoAnterior} → {event.estadoNuevo}</span>}</div></li>)}</ol>}</Card>}
  </div>
}
