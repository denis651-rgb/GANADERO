import { Fragment, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, ClipboardList, ChevronDown, ChevronUp } from 'lucide-react'
import { listAuditoria } from '@/features/auditoria/api'
import { listUsuarios } from '@/features/usuarios/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function AuditoriaPage() {
  const [page, setPage] = useState(0)
  const [modulo, setModulo] = useState('')
  const [accion, setAccion] = useState('')
  const [entidad, setEntidad] = useState('')
  const [usuarioId, setUsuarioId] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [expanded, setExpanded] = useState<string | null>(null)
  const size = 15

  const query = useQuery({
    queryKey: ['auditoria', { modulo, accion, entidad, usuarioId, desde, hasta, page, size }],
    queryFn: () => listAuditoria({ modulo, accion, entidad, usuarioId, desde: desde || undefined, hasta: hasta || undefined, page, size }),
    placeholderData: keepPreviousData,
  })
  const usuarios = useQuery({ queryKey: ['auditoria-usuarios'], queryFn: listUsuarios })
  const error = query.error ?? usuarios.error

  return <div className="page-stack">
    <PageHeader eyebrow="Seguridad" title="Auditoría" description="Trazabilidad de acciones, cambios y correlación de solicitudes." />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="filter-heading"><span><ClipboardList size={18} />Filtros</span>{query.data && <strong>{query.data.totalElements} registros</strong>}</div>
      <div className="animal-filters">
        <select aria-label="Filtrar por módulo" value={modulo} onChange={(event) => { setModulo(event.target.value); setPage(0) }}><option value="">Todos los módulos</option>{['ANIMALES', 'PROPIEDADES', 'POTREROS', 'LOTES', 'MOVIMIENTOS', 'SEGURIDAD', 'AUDITORIA'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por acción" value={accion} onChange={(event) => { setAccion(event.target.value); setPage(0) }}><option value="">Todas las acciones</option>{['CREAR', 'ACTUALIZAR', 'ELIMINAR', 'CERRAR', 'ASIGNAR', 'CONFIRMAR', 'ANULAR', 'BLOQUEAR', 'ACTIVAR'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por entidad" value={entidad} onChange={(event) => { setEntidad(event.target.value); setPage(0) }}><option value="">Toda entidad</option>{['ANIMAL', 'IDENTIFICADOR', 'PARENTESCO', 'LOTE', 'MOVIMIENTO', 'MIEMBRO_EMPRESA', 'ROL', 'PROPIEDAD', 'POTRERO'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por usuario" value={usuarioId} onChange={(event) => { setUsuarioId(event.target.value); setPage(0) }}><option value="">Todos los usuarios</option>{usuarios.data?.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select>
        <input aria-label="Desde" type="datetime-local" value={desde} onChange={(event) => { setDesde(event.target.value); setPage(0) }} />
        <input aria-label="Hasta" type="datetime-local" value={hasta} onChange={(event) => { setHasta(event.target.value); setPage(0) }} />
      </div>
      {query.isPending && <LoadingState message="Consultando auditoría…" />}
      {query.data?.content.length === 0 && <EmptyState title="Sin registros" description="No hay acciones que coincidan con los filtros." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Fecha</th><th>Usuario</th><th>Módulo</th><th>Acción</th><th>Entidad</th><th>Resultado</th><th></th></tr></thead><tbody>{query.data.content.map((item) => {
          const user = usuarios.data?.find((u) => u.usuarioId === item.usuarioId)
          const isOpen = expanded === item.id
          return <Fragment key={item.id}>
            <tr><td>{new Date(item.createdAt).toLocaleString('es-BO')}</td><td>{user ? `${user.nombres} ${user.apellidos}` : '—'}</td><td>{item.modulo}</td><td><span className="status-badge">{item.accion}</span></td><td>{item.entidad}</td><td><span className="status-badge">{item.resultado}</span></td>
              <td>{(item.datosAnteriores || item.datosNuevos || item.datos) && <Button variant="ghost" onClick={() => setExpanded(isOpen ? null : item.id)}>{isOpen ? <ChevronUp size={17} /> : <ChevronDown size={17} />}Detalle</Button>}</td></tr>
            {isOpen && <tr className="selected-row"><td colSpan={7}><div className="permission-section">
              <div className="two-column-grid">
                {item.datosAnteriores && <div><h4>Datos anteriores</h4><pre className="code-block">{JSON.stringify(item.datosAnteriores, null, 2)}</pre></div>}
                {item.datosNuevos && <div><h4>Datos nuevos</h4><pre className="code-block">{JSON.stringify(item.datosNuevos, null, 2)}</pre></div>}
              </div>
              <p className="table-secondary">IP: {item.ip ?? '—'} · Dispositivo: {item.dispositivo ?? '—'} · Correlation: {item.correlationId ?? '—'}{item.userAgent ? ` · UA: ${item.userAgent}` : ''}</p>
            </div></td></tr>}
          </Fragment>
        })}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
