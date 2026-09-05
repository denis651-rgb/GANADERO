import { useState } from 'react'
import { Link } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Eye, Group, Plus } from 'lucide-react'
import { listCompras } from '@/features/compras/api'
import type { EstadoCompra } from '@/features/compras/types'
import { buscarProveedores } from '@/features/proveedores/api'
import { formatDate } from '@/shared/utils/date'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { normalizeApiError } from '@/shared/api/errors'

const modalidadLabel: Record<string, string> = { POR_UNIDAD: 'Por unidad', POR_TROPA: 'Por tropa o punta' }

export function ComprasPage() {
  const [estado, setEstado] = useState<EstadoCompra | ''>('')
  const [page, setPage] = useState(0)
  const size = 10
  const query = useQuery({
    queryKey: ['compras', { estado, page, size }],
    queryFn: () => listCompras({ estado: estado || undefined, page, size }),
    placeholderData: keepPreviousData,
  })
  const proveedores = useQuery({ queryKey: ['compras-proveedores'], queryFn: () => buscarProveedores('', false) })
  const error = query.error ?? proveedores.error
  const proveedorNombre = (id: string) => proveedores.data?.find((item) => item.id === id)?.nombre ?? '—'

  return <div className="page-stack">
    <PageHeader
      eyebrow="Compras"
      title="Compras de animales"
      description="Historial de compras individuales y por lote, con proveedor, precios y estado."
      actions={<>
        <Link className="button button-secondary" to="/animales/ingreso-lote"><Group size={18} aria-hidden="true" />Compra por lote</Link>
        <Link className="button button-primary" to="/animales/nuevo"><Plus size={18} aria-hidden="true" />Compra individual</Link>
      </>}
    />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="filter-heading">
        <span>Filtros</span>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as EstadoCompra | ''); setPage(0) }}>
          <option value="">Todos los estados</option>
          <option value="BORRADOR">Borrador</option>
          <option value="CONFIRMADA">Confirmada</option>
          <option value="ANULADA">Anulada</option>
        </select>
        {query.data && <strong>{query.data.totalElements} compra(s)</strong>}
      </div>
      {query.isPending && <TableSkeleton rows={6} columns={8} />}
      {query.data?.content.length === 0 && <EmptyState title="Sin compras registradas" description="Registra una compra individual o por lote para comenzar." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Compras registradas</caption><thead><tr>
          <th scope="col">Código</th><th scope="col">Fecha</th><th scope="col">Proveedor</th><th scope="col">Cantidad</th>
          <th scope="col">Modalidad</th><th scope="col">Total</th><th scope="col">Estado</th><th scope="col">Acciones</th>
        </tr></thead><tbody>{query.data.content.map((compra) => <tr key={compra.id}>
          <td><strong>{compra.codigo}</strong></td>
          <td>{formatDate(compra.fechaRecepcion)}</td>
          <td>{proveedorNombre(compra.proveedorId)}</td>
          <td>{compra.cantidadAnimales}</td>
          <td>{modalidadLabel[compra.modalidad] ?? compra.modalidad}</td>
          <td>{compra.precioTotal.toLocaleString('es-BO', { style: 'currency', currency: compra.moneda || 'BOB' })}</td>
          <td><span className={`status-badge status-${compra.estado.toLowerCase()}`}>{compra.estado}</span></td>
          <td><Link className="button button-ghost" to={`/compras/${compra.id}`}><Eye size={16} aria-hidden="true" />Ver</Link></td>
        </tr>)}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{query.data.content.map((compra) => <MobileEntityCard
          key={compra.id}
          title={compra.codigo}
          subtitle={proveedorNombre(compra.proveedorId)}
          status={<span className={`status-badge status-${compra.estado.toLowerCase()}`}>{compra.estado}</span>}
          metadata={<><strong>{compra.precioTotal.toLocaleString('es-BO', { style: 'currency', currency: compra.moneda || 'BOB' })}</strong><span>{formatDate(compra.fechaRecepcion)} · {compra.cantidadAnimales} animal(es)</span></>}
          action={<Link className="button button-ghost" to={`/compras/${compra.id}`}>Ver →</Link>}
        />)}</div></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} aria-hidden="true" />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} aria-hidden="true" /></Button></div></div>
      </>}
    </Card>
  </div>
}
