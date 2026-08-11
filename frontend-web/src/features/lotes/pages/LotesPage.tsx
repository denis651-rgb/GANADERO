import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Eye, Plus, Search } from 'lucide-react'
import { createLote, listLotes } from '@/features/lotes/api'
import type { EstadoLote } from '@/features/lotes/api'
import { listPropiedades } from '@/features/propiedades/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

export function LotesPage() {
  const client = useQueryClient()
  const { showToast } = useToast()
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [page, setPage] = useState(0)
  const [estado, setEstado] = useState<EstadoLote | ''>('')
  const [showForm, setShowForm] = useState(false)
  const size = 10
  const query = useQuery({
    queryKey: ['lotes', { search: deferredSearch, estado, page, size }],
    queryFn: () => listLotes({ search: deferredSearch, estado, page, size }),
    placeholderData: keepPreviousData,
  })
  const propiedades = useQuery({ queryKey: ['lotes-propiedades'], queryFn: listPropiedades })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createLote({
        propiedadId: String(data.get('propiedadId')),
        codigo: String(data.get('codigo')),
        nombre: String(data.get('nombre')),
        descripcion: String(data.get('descripcion') ?? '') || undefined,
        fechaApertura: String(data.get('fechaApertura') ?? '') || undefined,
      })
    },
    onSuccess: async () => { setShowForm(false); showToast('Lote creado correctamente.'); await client.invalidateQueries({ queryKey: ['lotes'] }) },
  })
  const error = query.error ?? propiedades.error ?? create.error

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Lotes" description="Agrupación operativa de animales y membresías históricas." actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} />Nuevo lote</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {showForm && <Card><form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="Propiedad"><select name="propiedadId" required><option value="">Selecciona una propiedad…</option>{propiedades.data?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Código"><input name="codigo" required maxLength={60} placeholder="Ej. L-2026-01" /></Field>
      <Field label="Nombre"><input name="nombre" required maxLength={160} placeholder="Ej. Lote de engorde A" /></Field>
      <Field label="Descripción"><input name="descripcion" maxLength={1000} /></Field>
      <Field label="Fecha de apertura"><input name="fechaApertura" type="date" /></Field>
      <div className="form-actions"><Button type="submit" loading={create.isPending}>Crear lote</Button></div>
    </form></Card>}
    <Card>
      <div className="filter-heading"><span className="search-box"><Search size={18} /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Buscar por código o nombre" /></span>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as EstadoLote | ''); setPage(0) }}><option value="">Todos los estados</option><option value="ACTIVO">Activo</option><option value="CERRADO">Cerrado</option></select></div>
      {query.isPending && <TableSkeleton rows={7} columns={6} />}
      {query.data?.content.length === 0 && <EmptyState title="No hay lotes" description="Crea el primer lote para agrupar animales." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Código</th><th>Nombre</th><th>Propiedad</th><th>Estado</th><th>Apertura</th><th>Cierre</th><th></th></tr></thead><tbody>{query.data.content.map((lote) => <tr key={lote.id}>
          <td><strong>{lote.codigo}</strong></td><td>{lote.nombre}</td><td>{propiedades.data?.find((item) => item.id === lote.propiedadId)?.nombre ?? '—'}</td>
          <td><span className="status-badge">{lote.estado}</span></td><td>{new Date(lote.fechaApertura).toLocaleDateString('es-BO')}</td><td>{lote.fechaCierre ? new Date(lote.fechaCierre).toLocaleDateString('es-BO') : '—'}</td>
          <td><Link to={`/lotes/${lote.id}`}><Button variant="ghost"><Eye size={16} />Ver</Button></Link></td>
        </tr>)}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
