import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Eye, Plus, Search, SlidersHorizontal } from 'lucide-react'
import { listAnimals, listCategorias } from '@/features/animales/api'
import type { AnimalState } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'

const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']

export function AnimalesPage() {
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [page, setPage] = useState(0)
  const [estado, setEstado] = useState<AnimalState | ''>('')
  const [sexo, setSexo] = useState<'MACHO' | 'HEMBRA' | ''>('')
  const [propertyId, setPropertyId] = useState('')
  const [paddockId, setPaddockId] = useState('')
  const [category, setCategory] = useState('')
  const size = 10
  const filters = { search: deferredSearch, estado, sexo, propiedadId: propertyId, potreroId: paddockId, categoria: category, page, size }
  const query = useQuery({ queryKey: ['animals', filters], queryFn: () => listAnimals(filters), placeholderData: keepPreviousData })
  const catalogs = useQuery({ queryKey: ['animal-list-catalogs'], queryFn: async () => {
    const [categories, properties, paddocks] = await Promise.all([listCategorias(), listPropiedades(), listPotreros()])
    return { categories, properties, paddocks }
  } })
  const error = query.error ?? catalogs.error
  const resetPage = () => setPage(0)

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Animales" description="Consulta, filtra y administra el historial del hato." actions={<Link to="/animales/nuevo"><Button><Plus size={18} />Nuevo animal</Button></Link>} />
    <Card>
      <div className="filter-heading"><span><SlidersHorizontal size={18} />Filtros</span>{query.data && <strong>{query.data.totalElements} animales</strong>}</div>
      <div className="animal-filters">
        <label className="search-box"><Search size={18} /><input value={search} onChange={(event) => { setSearch(event.target.value); resetPage() }} placeholder="Código o nombre" /></label>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as AnimalState | ''); resetPage() }}><option value="">Todos los estados</option>{states.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por sexo" value={sexo} onChange={(event) => { setSexo(event.target.value as typeof sexo); resetPage() }}><option value="">Todos los sexos</option><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select>
        <select aria-label="Filtrar por propiedad" value={propertyId} onChange={(event) => { setPropertyId(event.target.value); setPaddockId(''); resetPage() }}><option value="">Todas las propiedades</option>{catalogs.data?.properties.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        <select aria-label="Filtrar por potrero" value={paddockId} onChange={(event) => { setPaddockId(event.target.value); resetPage() }}><option value="">Todos los potreros</option>{catalogs.data?.paddocks.filter((item) => !propertyId || item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        <select aria-label="Filtrar por categoría" value={category} onChange={(event) => { setCategory(event.target.value); resetPage() }}><option value="">Todas las categorías</option>{catalogs.data?.categories.map((item) => <option key={item.id} value={item.codigo}>{item.nombre}</option>)}</select>
      </div>
      {query.isPending && <LoadingState message="Consultando animales…" />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {query.data?.content.length === 0 && <EmptyState title="No hay resultados" description="Cambia los filtros o registra un nuevo animal." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Código</th><th>Nombre</th><th>Sexo</th><th>Categoría</th><th>Ubicación</th><th>Estado</th><th></th></tr></thead><tbody>{query.data.content.map((animal) => <tr key={animal.id}>
          <td><strong>{animal.codigo}</strong></td><td>{animal.nombre || '—'}</td><td>{animal.sexo}</td><td>{catalogs.data?.categories.find((item) => item.id === animal.categoriaActualId)?.nombre ?? '—'}</td>
          <td>{[catalogs.data?.properties.find((item) => item.id === animal.propiedadActualId)?.nombre, catalogs.data?.paddocks.find((item) => item.id === animal.potreroActualId)?.nombre].filter(Boolean).join(' / ') || '—'}</td><td><span className="status-badge">{animal.estado}</span></td>
          <td><Link to={`/animales/${animal.id}`}><Button variant="ghost"><Eye size={16} />Ver</Button></Link></td>
        </tr>)}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
