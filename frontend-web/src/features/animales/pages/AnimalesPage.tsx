import { useDeferredValue, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useLiveQuery } from 'dexie-react-hooks'
import { ChevronLeft, ChevronRight, Eye, Plus, Printer, ScanLine, Search, SlidersHorizontal } from 'lucide-react'
import { listAnimals, listCategorias, listIdentificadores } from '@/features/animales/api'
import type { AnimalState, AnimalSummary } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { offlineFormCatalogs } from '@/offline/catalogs'
import { db } from '@/offline/db'
import type { LocalAnimalSummary } from '@/offline/offline.types'
import type { Page } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import { printRoute } from '@/features/animales/qr/print-utils'

const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']

interface OfflineAnimalFilters {
  search: string
  estado: AnimalState | ''
  sexo: 'MACHO' | 'HEMBRA' | ''
  propiedadId: string
  potreroId: string
  categoria: string
}

function buildOfflinePage(animals: LocalAnimalSummary[], filters: OfflineAnimalFilters, page: number, size: number): Page<AnimalSummary> {
  const q = filters.search.trim().toLowerCase()
  const filtered = animals.filter((animal) => {
    if (filters.estado && animal.status !== filters.estado) return false
    if (filters.sexo && animal.sex !== filters.sexo) return false
    if (filters.propiedadId && animal.propertyId !== filters.propiedadId) return false
    if (filters.potreroId && animal.paddockId !== filters.potreroId) return false
    if (filters.categoria && animal.category !== filters.categoria) return false
    if (q && !`${animal.code} ${animal.name ?? ''} ${animal.primaryIdentifier ?? ''}`.toLowerCase().includes(q)) return false
    return true
  })
  const start = page * size
  const content = filtered.slice(start, start + size).map<AnimalSummary>((animal) => ({
    id: animal.id,
    codigo: animal.code,
    nombre: animal.name,
    sexo: (animal.sex as AnimalSummary['sexo']) || 'HEMBRA',
    categoriaActualId: animal.category ?? '',
    fechaNacimiento: undefined,
    fechaNacimientoEstimada: false,
    razaPrincipalId: '',
    proposito: 'CARNE',
    origen: 'NACIDO',
    estado: (animal.status as AnimalState) || 'ACTIVO',
    propiedadActualId: animal.propertyId,
    potreroActualId: animal.paddockId,
    loteActualId: animal.lotId,
    fechaIngreso: '',
    version: animal.version,
  }))
  return { content, page, size, totalElements: filtered.length, totalPages: Math.max(1, Math.ceil(filtered.length / size)) }
}

interface DisplayCats {
  categories?: Array<{ id: string; codigo?: string; nombre: string }>
  properties?: Array<{ id: string; nombre: string }>
  paddocks?: Array<{ id: string; nombre: string; propiedadId: string }>
}

export function AnimalesPage() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [page, setPage] = useState(0)
  const [estado, setEstado] = useState<AnimalState | ''>('')
  const [sexo, setSexo] = useState<'MACHO' | 'HEMBRA' | ''>('')
  const [propertyId, setPropertyId] = useState('')
  const [paddockId, setPaddockId] = useState('')
  const [category, setCategory] = useState('')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [printing, setPrinting] = useState(false)
  const [printError, setPrintError] = useState('')
  const size = 10
  const filters = { search: deferredSearch, estado, sexo, propiedadId: propertyId, potreroId: paddockId, categoria: category, page, size }
  const query = useQuery({ queryKey: ['animals', filters], queryFn: () => listAnimals(filters), placeholderData: keepPreviousData })
  const catalogs = useQuery({ queryKey: ['animal-list-catalogs'], queryFn: async () => {
    const [categories, properties, paddocks] = await Promise.all([listCategorias(), listPropiedades(), listPotreros()])
    return { categories, properties, paddocks }
  } })
  const animalsError = query.error ? normalizeApiError(query.error) : null
  const offline = animalsError?.code === 'NETWORK_ERROR'
  const localAnimals = useLiveQuery(() => (offline ? db.animalesResumen.toArray() : Promise.resolve([] as LocalAnimalSummary[])), [offline], [] as LocalAnimalSummary[])
  const localCatalogs = useLiveQuery(() => (offline ? offlineFormCatalogs() : Promise.resolve(undefined)), [offline])
  const cats: DisplayCats | undefined = offline
    ? {
        categories: localCatalogs?.categories.map((item) => ({ id: item.id, codigo: item.id, nombre: item.nombre })),
        properties: localCatalogs?.properties,
        paddocks: localCatalogs?.paddocks,
      }
    : catalogs.data
  const displayData = offline
    ? buildOfflinePage(localAnimals ?? [], { search: deferredSearch, estado, sexo, propiedadId: propertyId, potreroId: paddockId, categoria: category }, page, size)
    : query.data
  const error = query.error ?? catalogs.error
  const resetPage = () => setPage(0)
  const toggleSelected = (id: string) => {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const printSelected = async () => {
    setPrinting(true)
    setPrintError('')
    try {
      const items = (
        await Promise.all([...selected].map(async (animalId) => {
          const identifiers = await listIdentificadores(animalId)
          const qr = identifiers.find((item) => item.tipo === 'QR' && item.estado === 'ACTIVO')
          if (!qr) return null
          const animal = query.data?.content.find((item) => item.id === animalId)
          return { animalId, identifierId: qr.id, codigo: animal?.codigo ?? qr.valor }
        }))
      ).filter((item): item is { animalId: string; identifierId: string; codigo: string } => item !== null)
      if (items.length === 0) {
        setPrintError('Ninguno de los animales seleccionados tiene un QR activo. Genera los QRs desde la ficha del animal.')
        return
      }
      navigate(printRoute(items))
    } finally {
      setPrinting(false)
    }
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Animales" description="Consulta, filtra y administra el historial del hato." actions={<>
      <Link className="button button-secondary" to="/qr/escanear"><ScanLine size={18} aria-hidden="true" />Escanear QR</Link>
      <Button variant="secondary" loading={printing} disabled={selected.size === 0} onClick={() => void printSelected()}><Printer size={18} />Imprimir QR ({selected.size})</Button>
      <Link className="button button-primary" to="/animales/nuevo"><Plus size={18} aria-hidden="true" />Nuevo animal</Link>
    </>} />
    <Card>
      <div className="filter-heading"><span><SlidersHorizontal size={18} />Filtros</span>{displayData && <strong>{displayData.totalElements} animales</strong>}</div>
      {printError && <Alert tone="danger">{printError}</Alert>}
      <div className="animal-filters">
        <label className="search-box"><Search size={18} aria-hidden="true" /><input type="search" aria-label="Buscar animales" value={search} onChange={(event) => { setSearch(event.target.value); resetPage() }} placeholder="Código o nombre…" /></label>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as AnimalState | ''); resetPage() }}><option value="">Todos los estados</option>{states.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por sexo" value={sexo} onChange={(event) => { setSexo(event.target.value as typeof sexo); resetPage() }}><option value="">Todos los sexos</option><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select>
        <select aria-label="Filtrar por propiedad" value={propertyId} onChange={(event) => { setPropertyId(event.target.value); setPaddockId(''); resetPage() }}><option value="">Todas las propiedades</option>{cats?.properties?.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        <select aria-label="Filtrar por potrero" value={paddockId} onChange={(event) => { setPaddockId(event.target.value); resetPage() }}><option value="">Todos los potreros</option>{cats?.paddocks?.filter((item) => !propertyId || item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        <select aria-label="Filtrar por categoría" value={category} onChange={(event) => { setCategory(event.target.value); resetPage() }}><option value="">Todas las categorías</option>{cats?.categories?.map((item) => <option key={item.id} value={item.codigo ?? ''}>{item.nombre}</option>)}</select>
      </div>
      {offline && <Alert tone="info" title="Mostrando datos locales">Sin conexión. Se muestran los animales guardados en este dispositivo (última sincronización); conéctate para ver la información más reciente.</Alert>}
      {query.isPending && <TableSkeleton rows={7} columns={8} />}
      {error && !offline && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {displayData?.content.length === 0 && <EmptyState title="No hay resultados" description="Cambia los filtros o registra un nuevo animal." />}
      {displayData && displayData.content.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Animales que coinciden con los filtros</caption><thead><tr><th scope="col" className="table-select-col"><input type="checkbox" aria-label="Seleccionar todos de la página" checked={displayData.content.every((animal) => selected.has(animal.id))} onChange={(event) => setSelected(event.target.checked ? new Set(displayData.content.map((animal) => animal.id)) : new Set())} /></th><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Sexo</th><th scope="col">Categoría</th><th scope="col">Ubicación</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{displayData.content.map((animal) => <tr key={animal.id}>
          <td><input type="checkbox" aria-label={`Seleccionar ${animal.codigo}`} checked={selected.has(animal.id)} onChange={() => toggleSelected(animal.id)} /></td><td><strong>{animal.codigo}</strong></td><td>{animal.nombre || '—'}</td><td>{animal.sexo}</td><td>{cats?.categories?.find((item) => item.id === animal.categoriaActualId)?.nombre ?? '—'}</td>
          <td>{[cats?.properties?.find((item) => item.id === animal.propiedadActualId)?.nombre, cats?.paddocks?.find((item) => item.id === animal.potreroActualId)?.nombre].filter(Boolean).join(' / ') || '—'}</td><td><span className={`status-badge status-${animal.estado.toLowerCase()}`}>{animal.estado}</span></td>
          <td><Link className="button button-ghost" to={`/animales/${animal.id}`} aria-label={`Ver animal ${animal.codigo}${animal.nombre ? `, ${animal.nombre}` : ''}`}><Eye size={16} aria-hidden="true" />Ver</Link></td>
        </tr>)}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{displayData.content.map((animal) => {
          const category = cats?.categories?.find((item) => item.id === animal.categoriaActualId)?.nombre
          const location = [cats?.properties?.find((item) => item.id === animal.propiedadActualId)?.nombre, cats?.paddocks?.find((item) => item.id === animal.potreroActualId)?.nombre].filter(Boolean).join(' / ')
          return <MobileEntityCard
            key={animal.id}
            title={<>{animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</>}
            status={<span className={`status-badge status-${animal.estado.toLowerCase()}`}>{animal.estado}</span>}
            subtitle={`${category ?? 'Sin categoría'} · ${animal.sexo}`}
            metadata={location ? <span>Ubicación: {location}</span> : <span>Ubicación no disponible</span>}
            selection={<input type="checkbox" aria-label={`Seleccionar ${animal.codigo}`} checked={selected.has(animal.id)} onChange={() => toggleSelected(animal.id)} />}
            action={<Link className="button button-ghost" to={`/animales/${animal.id}`}>Ver animal →</Link>}
          />
        })}</div></div>
        <div className="pagination"><span>Página {displayData.page + 1} de {Math.max(displayData.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= displayData.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
