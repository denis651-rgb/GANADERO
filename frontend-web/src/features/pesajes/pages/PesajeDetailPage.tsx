import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Ban, Eye } from 'lucide-react'
import { getPesaje, getPesajeHistory } from '@/features/pesajes/api'
import { AnularPesajeForm } from '@/features/pesajes/components/AnularPesajeForm'
import type { Pesaje } from '@/features/pesajes/types'
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

export function PesajeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const [anullng, setAnullng] = useState<Pesaje | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'info' | 'danger'; text: string } | null>(null)
  const pesajeQuery = useQuery({ queryKey: ['pesaje', id], queryFn: () => getPesaje(id ?? ''), enabled: Boolean(id) })
  const pesaje = pesajeQuery.data
  const historyQuery = useQuery({
    queryKey: ['pesaje-history', pesaje?.animalId],
    queryFn: () => getPesajeHistory(pesaje.animalId),
    enabled: Boolean(pesaje?.animalId),
  })
  const error = pesajeQuery.error

  return <div className="page-stack narrow-page">
    <PageHeader
      eyebrow="Pesajes"
      title="Detalle del pesaje"
      description={pesaje ? `${pesaje.codigoAnimal ?? 'Animal'} · ${pesaje.pesoKg} kg · ${formatDate(pesaje.fecha)}` : 'Cargando…'}
      actions={<Link to="/pesajes"><Button variant="ghost"><ArrowLeft size={18} />Volver</Button></Link>}
    />
    {message && <Alert tone={message.tone}>{message.text}</Alert>}
    {pesajeQuery.isPending && <LoadingState message="Cargando pesaje…" />}
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {pesaje && <>
      {anullng && <AnularPesajeForm pesaje={anullng} onCancel={() => setAnullng(null)} onAnnulled={() => {
        setAnullng(null)
        setMessage({ tone: 'success', text: 'Pesaje anulado.' })
        void queryClient.invalidateQueries({ queryKey: ['pesaje', pesaje.id] })
        void queryClient.invalidateQueries({ queryKey: ['pesaje-history', pesaje.animalId] })
      }} />}
      <Card>
        <div className="section-heading"><h3>Datos del control</h3>{pesaje.estado === 'ACTIVO' && <Button variant="danger" onClick={() => setAnullng(pesaje)}><Ban size={17} />Anular pesaje</Button>}</div>
        <dl className="definition-list">
          <div><dt>Animal</dt><dd><Link to={`/animales/${pesaje.animalId}`}>{pesaje.codigoAnimal ?? '—'}{pesaje.nombreAnimal ? ` · ${pesaje.nombreAnimal}` : ''}</Link></dd></div>
          <div><dt>Fecha</dt><dd>{formatDate(pesaje.fecha)}</dd></div>
          <div><dt>Peso</dt><dd><strong>{pesaje.pesoKg} kg</strong></dd></div>
          <div><dt>Tipo</dt><dd>{tipoLabel[pesaje.tipo] ?? pesaje.tipo}</dd></div>
          <div><dt>Condición corporal</dt><dd>{pesaje.condicionCorporal ?? '—'}</dd></div>
          <div><dt>Báscula</dt><dd>{pesaje.bascula || '—'}</dd></div>
          <div><dt>Dispositivo</dt><dd>{pesaje.dispositivo || '—'}</dd></div>
          <div><dt>Estado</dt><dd><span className={`status-badge status-${pesaje.estado.toLowerCase()}`}>{pesaje.estado}</span></dd></div>
          {pesaje.estado === 'ANULADO' && <div><dt>Motivo de anulación</dt><dd>{pesaje.motivoAnulacion || '—'}</dd></div>}
          <div><dt>Observaciones</dt><dd>{pesaje.observaciones || '—'}</dd></div>
          <div><dt>Versión</dt><dd>{pesaje.version}</dd></div>
        </dl>
      </Card>
      <Card>
        <h3>Historial de pesajes del animal</h3>
        {historyQuery.isPending && <LoadingState message="Cargando historial…" />}
        {historyQuery.data?.length === 0 && <EmptyState title="Sin historial" description="Este animal aún no tiene otros pesajes registrados." />}
        {historyQuery.data && historyQuery.data.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Fecha</th><th>Peso</th><th>Tipo</th><th>Estado</th><th></th></tr></thead><tbody>{historyQuery.data.map((item) => <tr key={item.id}>
          <td>{formatDate(item.fecha)}</td>
          <td><strong>{item.pesoKg} kg</strong></td>
          <td>{tipoLabel[item.tipo] ?? item.tipo}</td>
          <td><span className={`status-badge status-${item.estado.toLowerCase()}`}>{item.estado}</span></td>
          <td>{item.id !== pesaje.id && <Link to={`/pesajes/${item.id}`}><Button variant="ghost"><Eye size={16} />Ver</Button></Link>}</td>
        </tr>)}</tbody></table></div>}
      </Card>
    </>}
  </div>
}
