import { useDeferredValue, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Beef, CircleCheck, ChevronLeft, ChevronRight, Eye, Layers, Mars, Plus, Printer, Search, SlidersHorizontal, Venus } from 'lucide-react'
import { getAnimalesResumen, listAnimals, listCategorias, listIdentificadores, listRazas } from '@/features/animales/api'
import type { AnimalSummary, AnimalState } from '@/features/animales/types'
import { calcularEdadMeses, formatearEdadMeses } from '@/features/animales/edad'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
import { listLotes } from '@/features/lotes/api'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import { printRoute } from '@/features/animales/qr/print-utils'

const UNIDAD_EDAD_LABELS: Record<string, string> = { DIAS: 'días', MESES: 'meses', ANIOS: 'años' }
const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']

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
  const todayStr = new Date().toISOString().slice(0, 10)
  const filters = { search: deferredSearch, estado, sexo, propiedadId: propertyId, potreroId: paddockId, categoria: category, page, size }
  const query = useQuery({ queryKey: ['animals', filters], queryFn: () => listAnimals(filters), placeholderData: keepPreviousData })
  const resumenFilters = { search: deferredSearch, estado, sexo, propiedadId: propertyId, potreroId: paddockId, categoria: category }
  const resumenQuery = useQuery({ queryKey: ['animals-resumen', resumenFilters], queryFn: () => getAnimalesResumen(resumenFilters), staleTime: 30_000 })
  const resumen = resumenQuery.data
  const catalogs = useQuery({ queryKey: ['animal-list-catalogs'], queryFn: async () => {
    const [categories, properties, paddocks, breeds] = await Promise.all([listCategorias(), listPropiedades(), listAllPotreros(), listRazas()])
    return { categories, properties, paddocks, breeds }
  } })
  const lotesQuery = useQuery({ queryKey: ['lotes-select'], queryFn: () => listLotes({ estado: 'ACTIVO', page: 0, size: 500 }), staleTime: 60_000 })
  const cats = catalogs.data
  const displayData = query.data
  const error = query.error ?? catalogs.error
  const resetPage = () => setPage(0)

  const categoryNames = useMemo(() => new Map((catalogs.data?.categories ?? []).map((c) => [c.id, c.nombre])), [catalogs.data])
  const propertyNames = useMemo(() => new Map((catalogs.data?.properties ?? []).map((p) => [p.id, p.nombre])), [catalogs.data])
  const paddockNames = useMemo(() => new Map((catalogs.data?.paddocks ?? []).map((p) => [p.id, p.nombre])), [catalogs.data])
  const breedNames = useMemo(() => new Map((catalogs.data?.breeds ?? []).map((r) => [r.id, r.nombre])), [catalogs.data])
  const loteNames = useMemo(() => new Map((lotesQuery.data?.content ?? []).map((l) => [l.id, l.nombre])), [lotesQuery.data])

  const locationLabel = (a: AnimalSummary) => {
    const parts = [propertyNames.get(a.propiedadActualId), paddockNames.get(a.potreroActualId)]
    if (a.loteActualId) parts.push(loteNames.get(a.loteActualId))
    return parts.filter(Boolean).join(' / ') || '—'
  }

  const edadLabel = (a: AnimalSummary) => {
    if (a.fechaNacimiento) {
      const meses = calcularEdadMeses(a.fechaNacimiento, todayStr)
      if (meses != null) return formatearEdadMeses(meses)
    }
    if (a.edadDeclaradaValor != null) return `${a.edadDeclaradaValor} ${UNIDAD_EDAD_LABELS[a.edadDeclaradaUnidad ?? 'MESES']}`
    return '—'
  }

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
      <Button variant="secondary" loading={printing} disabled={selected.size === 0} onClick={() => void printSelected()}><Printer size={18} />Imprimir QR ({selected.size})</Button>
      <Link className="button button-secondary" to="/animales/ingreso-lote"><Layers size={18} aria-hidden="true" />Ingreso por lote de compra</Link>
      <Link className="button button-primary" to="/animales/nuevo"><Plus size={18} aria-hidden="true" />Nuevo animal</Link>
    </>} />
    <div className="card dp-kpi-grid animales-kpi" aria-label="Resumen del hato según los filtros activos">
      <div className="dp-kpi-card">
        <span className="dp-kpi-icon"><Beef size={17} aria-hidden="true" /></span>
        <span className="dp-kpi-label">Hato</span>
        <strong>{resumen?.total ?? '—'}</strong>
        <small>animales</small>
      </div>
      <div className="dp-kpi-card">
        <span className="dp-kpi-icon"><CircleCheck size={17} aria-hidden="true" /></span>
        <span className="dp-kpi-label">Activos</span>
        <strong>{resumen?.activos ?? '—'}</strong>
        <small>en servicio</small>
      </div>
      <div className="dp-kpi-card is-hembras">
        <span className="dp-kpi-icon"><Venus size={17} aria-hidden="true" /></span>
        <span className="dp-kpi-label">Hembras</span>
        <strong>{resumen?.hembras ?? '—'}</strong>
        <small>del total</small>
      </div>
      <div className="dp-kpi-card is-machos">
        <span className="dp-kpi-icon"><Mars size={17} aria-hidden="true" /></span>
        <span className="dp-kpi-label">Machos</span>
        <strong>{resumen?.machos ?? '—'}</strong>
        <small>del total</small>
      </div>
    </div>
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
      {query.isPending && <TableSkeleton rows={7} columns={8} />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {displayData?.content.length === 0 && <EmptyState title="No hay resultados" description="Cambia los filtros o registra un nuevo animal." />}
      {displayData && displayData.content.length > 0 && <>
        <div className="table-wrapper desktop-only">
          <table className="animales-table">
            <caption className="visually-hidden">Animales que coinciden con los filtros</caption>
            <thead>
              <tr>
                <th scope="col" className="table-select-col"><input type="checkbox" aria-label="Seleccionar todos de la página" checked={displayData.content.every((animal) => selected.has(animal.id))} onChange={(event) => setSelected(event.target.checked ? new Set(displayData.content.map((animal) => animal.id)) : new Set())} /></th>
                <th scope="col">Nombre</th>
                <th scope="col">Sexo</th>
                <th scope="col">Edad</th>
                <th scope="col">Categoría</th>
                <th scope="col">Ubicación</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {displayData.content.map((animal) => {
                const breed = breedNames.get(animal.razaPrincipalId)
                return <tr key={animal.id}>
                  <td><input type="checkbox" aria-label={`Seleccionar ${animal.codigo}`} checked={selected.has(animal.id)} onChange={() => toggleSelected(animal.id)} /></td>
                  <td>
                    <span className="animal-name">{animal.nombre || animal.codigo}</span>
                    {breed && <span className="table-secondary">{breed}</span>}
                  </td>
                  <td>{animal.sexo}</td>
                  <td>{edadLabel(animal)}</td>
                  <td>{categoryNames.get(animal.categoriaActualId) ?? '—'}</td>
                  <td>{locationLabel(animal)}</td>
                  <td><span className={`status-badge status-${animal.estado.toLowerCase()}`}>{animal.estado}</span></td>
                  <td><Link className="button button-ghost" to={`/animales/${animal.id}`} aria-label={`Ver animal ${animal.codigo}${animal.nombre ? `, ${animal.nombre}` : ''}`}><Eye size={16} aria-hidden="true" />Ver</Link></td>
                </tr>
              })}
            </tbody>
          </table>
        </div>
        <div className="mobile-only">
          <div className="mobile-entity-list">
            {displayData.content.map((animal) => {
              const breed = breedNames.get(animal.razaPrincipalId)
              const category = categoryNames.get(animal.categoriaActualId)
              return <MobileEntityCard
                key={animal.id}
                title={animal.nombre || animal.codigo}
                subtitle={<>{breed ?? 'Sin raza'} · {animal.sexo}</>}
                status={<span className={`status-badge status-${animal.estado.toLowerCase()}`}>{animal.estado}</span>}
                metadata={<>{category && <span>{category}</span>}<span>{locationLabel(animal)}</span><span>{edadLabel(animal)}</span></>}
                selection={<input type="checkbox" aria-label={`Seleccionar ${animal.codigo}`} checked={selected.has(animal.id)} onChange={() => toggleSelected(animal.id)} />}
                action={<Link className="button button-ghost" to={`/animales/${animal.id}`}>Ver animal →</Link>}
              />
            })}
          </div>
        </div>
        <div className="pagination">
          <span>Página {displayData.page + 1} de {Math.max(displayData.totalPages, 1)}</span>
          <div>
            <Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button>
            <Button variant="ghost" disabled={page + 1 >= displayData.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button>
          </div>
        </div>
      </>}
    </Card>
  </div>
}
