import { Fragment, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, ClipboardList, ChevronDown, ChevronUp } from 'lucide-react'
import { listAuditoria, type AuditoriaRegistro } from '@/features/auditoria/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listUsuarios } from '@/features/usuarios/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { normalizeApiError } from '@/shared/api/errors'

function diffObjects(antes: Record<string, unknown> | undefined, nuevo: Record<string, unknown> | undefined) {
  const a = antes ?? {}
  const n = nuevo ?? {}
  const keys = Array.from(new Set([...Object.keys(a), ...Object.keys(n)]))
  return keys.filter((key) => JSON.stringify(a[key]) !== JSON.stringify(n[key]))
}

function Diferencia({ item }: { item: AuditoriaRegistro }) {
  const changed = diffObjects(item.datosAnteriores, item.datosNuevos)
  if (changed.length === 0) return null
  return <div>
    <h4>Diferencia</h4>
    <pre className="code-block">{changed.map((key) => `${key}: ${JSON.stringify(item.datosAnteriores?.[key] ?? null)} → ${JSON.stringify(item.datosNuevos?.[key] ?? null)}`).join('\n')}</pre>
  </div>
}

export function AuditoriaPage() {
  const [page, setPage] = useState(0)
  const [modulo, setModulo] = useState('')
  const [accion, setAccion] = useState('')
  const [entidad, setEntidad] = useState('')
  const [usuarioId, setUsuarioId] = useState('')
  const [propiedadId, setPropiedadId] = useState('')
  const [correlationId, setCorrelationId] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [expanded, setExpanded] = useState<string | null>(null)
  const size = 15

  const query = useQuery({
    queryKey: ['auditoria', { modulo, accion, entidad, usuarioId, propiedadId, correlationId, desde, hasta, page, size }],
    queryFn: () => listAuditoria({ modulo, accion, entidad, usuarioId, propiedadId, correlationId, desde: desde || undefined, hasta: hasta || undefined, page, size }),
    placeholderData: keepPreviousData,
  })
  const usuarios = useQuery({ queryKey: ['auditoria-usuarios'], queryFn: listUsuarios })
  const propiedades = useQuery({ queryKey: ['auditoria-propiedades'], queryFn: listPropiedades })
  const error = query.error ?? usuarios.error ?? propiedades.error

  return <div className="page-stack">
    <PageHeader eyebrow="Seguridad" title="Auditoría" description="Trazabilidad de acciones, cambios y correlación de solicitudes." />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="filter-heading"><span><ClipboardList size={18} />Filtros</span>{query.data && <strong>{query.data.totalElements} registros</strong>}</div>
      <div className="animal-filters">
        <select aria-label="Filtrar por módulo" value={modulo} onChange={(event) => { setModulo(event.target.value); setPage(0) }}><option value="">Todos los módulos</option>{['EMPRESAS', 'ANIMALES', 'PROPIEDADES', 'POTREROS', 'LOTES', 'MOVIMIENTOS', 'PESAJE', 'ARCHIVOS', 'SEGURIDAD', 'SINCRONIZACION', 'AUDITORIA'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por acción" value={accion} onChange={(event) => { setAccion(event.target.value); setPage(0) }}><option value="">Todas las acciones</option>{['CREAR', 'ACTUALIZAR', 'ELIMINAR', 'CERRAR', 'ASIGNAR', 'CONFIRMAR', 'ANULAR', 'BLOQUEAR', 'ACTIVAR', 'INVITAR', 'RETIRAR', 'RESOLVER_QR', 'PULL', 'PUSH', 'BOOTSTRAP', 'REGISTRAR_DISPOSITIVO'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por entidad" value={entidad} onChange={(event) => { setEntidad(event.target.value); setPage(0) }}><option value="">Toda entidad</option>{['EMPRESA', 'CONFIGURACION_EMPRESA', 'ANIMAL', 'IDENTIFICADOR', 'PARENTESCO', 'LOTE', 'MOVIMIENTO', 'PESAJE', 'DOCUMENTO', 'MIEMBRO_EMPRESA', 'ROL', 'PROPIEDAD', 'POTRERO', 'DISPOSITIVO', 'OPERACIONES'].map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por usuario" value={usuarioId} onChange={(event) => { setUsuarioId(event.target.value); setPage(0) }}><option value="">Todos los usuarios</option>{usuarios.data?.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select>
        <select aria-label="Filtrar por propiedad" value={propiedadId} onChange={(event) => { setPropiedadId(event.target.value); setPage(0) }}><option value="">Toda propiedad</option>{propiedades.data?.map((propiedad) => <option key={propiedad.id} value={propiedad.id}>{propiedad.codigo} · {propiedad.nombre}</option>)}</select>
        <input aria-label="Correlation ID" placeholder="Correlation ID" value={correlationId} onChange={(event) => { setCorrelationId(event.target.value); setPage(0) }} />
        <input aria-label="Desde" type="datetime-local" value={desde} onChange={(event) => { setDesde(event.target.value); setPage(0) }} />
        <input aria-label="Hasta" type="datetime-local" value={hasta} onChange={(event) => { setHasta(event.target.value); setPage(0) }} />
      </div>
      {query.isPending && <LoadingState message="Consultando auditoría…" />}
      {query.data?.content.length === 0 && <EmptyState title="Sin registros" description="No hay acciones que coincidan con los filtros." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Registros de auditoría</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Usuario</th><th scope="col">Módulo</th><th scope="col">Acción</th><th scope="col">Entidad</th><th scope="col">Resultado</th><th scope="col">Acciones</th></tr></thead><tbody>{query.data.content.map((item) => {
          const user = usuarios.data?.find((u) => u.usuarioId === item.usuarioId)
          const isOpen = expanded === item.id
          return <Fragment key={item.id}>
            <tr><td>{new Date(item.createdAt).toLocaleString('es-BO')}</td><td>{user ? `${user.nombres} ${user.apellidos}` : '—'}</td><td>{item.modulo}</td><td><span className="status-badge">{item.accion}</span></td><td>{item.entidad}</td><td><span className="status-badge">{item.resultado}</span></td>
              <td>{(item.datosAnteriores || item.datosNuevos || item.datos) && <Button variant="ghost" aria-label={`${isOpen ? 'Ocultar' : 'Mostrar'} detalle del registro de auditoría ${item.id}`} onClick={() => setExpanded(isOpen ? null : item.id)}>{isOpen ? <ChevronUp size={17} aria-hidden="true" /> : <ChevronDown size={17} aria-hidden="true" />}Detalle</Button>}</td></tr>
            {isOpen && <tr className="selected-row"><td colSpan={7}><div className="permission-section">
              <div className="two-column-grid">
                {item.datosAnteriores && <div><h4>Datos anteriores</h4><pre className="code-block">{JSON.stringify(item.datosAnteriores, null, 2)}</pre></div>}
                {item.datosNuevos && <div><h4>Datos nuevos</h4><pre className="code-block">{JSON.stringify(item.datosNuevos, null, 2)}</pre></div>}
              </div>
              <Diferencia item={item} />
              <p className="table-secondary">IP: {item.ip ?? '—'} · Dispositivo: {item.dispositivo ?? '—'} · Correlation: {item.correlationId ?? '—'}{item.userAgent ? ` · UA: ${item.userAgent}` : ''}</p>
            </div></td></tr>}
          </Fragment>
        })}</tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{query.data.content.map((item) => {
          const user = usuarios.data?.find((entry) => entry.usuarioId === item.usuarioId)
          const isOpen = expanded === item.id
          return <MobileEntityCard key={item.id} title={item.accion} status={<span className="status-badge">{item.resultado}</span>} subtitle={`${item.modulo} · ${item.entidad}`} metadata={<><span>{user ? `${user.nombres} ${user.apellidos}` : 'Usuario no disponible'}</span><span>{new Date(item.createdAt).toLocaleString('es-BO')}</span>{isOpen && <div className="audit-mobile-detail">{item.datosAnteriores && <><strong>Datos anteriores</strong><pre className="code-block">{JSON.stringify(item.datosAnteriores, null, 2)}</pre></>}{item.datosNuevos && <><strong>Datos nuevos</strong><pre className="code-block">{JSON.stringify(item.datosNuevos, null, 2)}</pre></>}</div>}</>} action={(item.datosAnteriores || item.datosNuevos || item.datos) ? <Button variant="ghost" onClick={() => setExpanded(isOpen ? null : item.id)}>{isOpen ? 'Ocultar detalle' : 'Ver detalle →'}</Button> : undefined} />
        })}</div></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
