import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { listMovimientos } from '@/features/movimientos/api'
import { MovimientoStatusBadge } from '@/features/movimientos/components/MovimientoStatusBadge'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'
import { formatDate } from '@/shared/utils/date'

export function LoteMovimientosPanel({ loteId }: { loteId: string }) {
  const { can } = useAuth()
  const [page, setPage] = useState(0)
  const allowed = can('MOVIMIENTO_VER')
  const movimientos = useQuery({
    queryKey: ['movimientos', 'lote', loteId, page],
    queryFn: () => listMovimientos({ loteId, page, size: 10 }),
    enabled: allowed,
  })
  if (!allowed) return null

  return <Card>
    <h3>Movimientos del lote</h3>
    <p className="muted">Consulta los traslados y los animales afectados, incluso cuando conservan su pertenencia al lote.</p>
    {movimientos.isPending && <LoadingState message="Cargando movimientos del lote…" />}
    {movimientos.error && <Alert tone="danger">{normalizeApiError(movimientos.error).message}</Alert>}
    {movimientos.data && <>
      {movimientos.data.content.length === 0 ? <p className="muted">No hay movimientos registrados para este lote.</p> : <div className="table-wrapper"><table>
        <caption className="visually-hidden">Movimientos ganaderos del lote</caption>
        <thead><tr><th scope="col">Fecha</th><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Motivo</th><th scope="col">Detalle</th></tr></thead>
        <tbody>{movimientos.data.content.map((movimiento) => <tr key={movimiento.id}>
          <td>{formatDate(movimiento.fechaMovimiento)}</td>
          <td>{movimiento.tipo.replaceAll('_', ' ')}</td>
          <td><MovimientoStatusBadge estado={movimiento.estado} /></td>
          <td>{movimiento.motivo || '—'}</td>
          <td><Link to={`/movimientos?movimientoId=${encodeURIComponent(movimiento.id)}`}>Ver movimiento y animales</Link></td>
        </tr>)}</tbody>
      </table></div>}
      {movimientos.data.totalPages > 1 && <div className="inline-actions">
        <Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</Button>
        <span>Página {page + 1} de {movimientos.data.totalPages}</span>
        <Button variant="ghost" disabled={page + 1 >= movimientos.data.totalPages} onClick={() => setPage(page + 1)}>Siguiente</Button>
      </div>}
    </>}
  </Card>
}
