import { useState } from 'react'
import { Link } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Ban, ChevronLeft, ChevronRight, Eye, Group, Plus, SlidersHorizontal } from 'lucide-react'
import { listPesajes } from '@/features/pesajes/api'
import { RegistrarPesajeForm } from '@/features/pesajes/components/RegistrarPesajeForm'
import { PesajeLoteForm } from '@/features/pesajes/components/PesajeLoteForm'
import { AnularPesajeForm } from '@/features/pesajes/components/AnularPesajeForm'
import type { Pesaje } from '@/features/pesajes/types'
import { listPropiedades } from '@/features/propiedades/api'
import { formatDate } from '@/shared/utils/date'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'

const tipoLabel: Record<string, string> = {
  RUTINA: 'Rutina',
  NACIMIENTO: 'Nacimiento',
  DESTETE: 'Destete',
  ENTRADA: 'Entrada',
  VENTA: 'Venta',
  PESADA_ESPECIAL: 'Especial',
}

export function PesajesPage() {
  const [propertyId, setPropertyId] = useState('')
  const [page, setPage] = useState(0)
  const [formMode, setFormMode] = useState<'individual' | 'lote' | null>(null)
  const [anularTarget, setAnularTarget] = useState<Pesaje | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'info' | 'danger'; text: string } | null>(null)
  const size = 10
  const filters = { propiedadId: propertyId, page, size }
  const query = useQuery({ queryKey: ['pesajes', filters], queryFn: () => listPesajes(filters), placeholderData: keepPreviousData })
  const properties = useQuery({ queryKey: ['pesaje-list-properties'], queryFn: listPropiedades })
  const error = query.error ?? properties.error

  const resetPage = () => setPage(0)

  return <div className="page-stack">
    <PageHeader
      eyebrow="Ganado"
      title="Pesajes y productividad"
      description="Controles individuales, por lote y curva de crecimiento."
      actions={<>
        <Button variant="secondary" onClick={() => { setFormMode('lote'); setAnularTarget(null) }}><Group size={18} />Pesaje por lote</Button>
        <Button onClick={() => { setFormMode('individual'); setAnularTarget(null) }}><Plus size={18} />Registrar pesaje</Button>
      </>}
    />
    {message && <Alert tone={message.tone}>{message.text}</Alert>}
    {formMode === 'individual' && <RegistrarPesajeForm onCancel={() => setFormMode(null)} onSaved={() => { setFormMode(null); setMessage({ tone: 'success', text: 'Pesaje registrado.' }) }} />}
    {formMode === 'lote' && <PesajeLoteForm onCancel={() => setFormMode(null)} onSaved={() => { setFormMode(null); setMessage({ tone: 'success', text: 'Pesaje por lote registrado.' }) }} />}
    {anularTarget && <AnularPesajeForm pesaje={anularTarget} onCancel={() => setAnularTarget(null)} onAnnulled={() => { setAnularTarget(null); setMessage({ tone: 'success', text: 'Pesaje anulado.' }) }} />}
    <Card>
      <div className="filter-heading"><span><SlidersHorizontal size={18} />Filtros</span>{query.data && <strong>{query.data.totalElements} pesajes</strong>}</div>
      <div className="animal-filters">
        <select aria-label="Filtrar por propiedad" value={propertyId} onChange={(event) => { setPropertyId(event.target.value); resetPage() }}><option value="">Todas las propiedades</option>{properties.data?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
      </div>
      {query.isPending && <LoadingState message="Consultando pesajes…" />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {query.data?.content.length === 0 && <EmptyState title="No hay pesajes" description="Registra el primer control para comenzar a medir la productividad." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Fecha</th><th>Animal</th><th>Peso</th><th>Tipo</th><th>Condición</th><th>Estado</th><th></th></tr></thead><tbody>{query.data.content.map((pesaje) => <tr key={pesaje.id}>
          <td>{formatDate(pesaje.fecha)}</td>
          <td><Link to={`/animales/${pesaje.animalId}`}><strong>{pesaje.codigoAnimal ?? '—'}</strong></Link>{pesaje.nombreAnimal ? <div className="cell-sub">{pesaje.nombreAnimal}</div> : null}</td>
          <td><strong>{pesaje.pesoKg} kg</strong></td>
          <td>{tipoLabel[pesaje.tipo] ?? pesaje.tipo}</td>
          <td>{pesaje.condicionCorporal ?? '—'}</td>
          <td><span className={`status-badge status-${pesaje.estado.toLowerCase()}`}>{pesaje.estado}</span></td>
          <td>
            <Link to={`/pesajes/${pesaje.id}`}><Button variant="ghost"><Eye size={16} />Ver</Button></Link>
            {pesaje.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => setAnularTarget(pesaje)}><Ban size={16} />Anular</Button>}
          </td>
        </tr>)}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
