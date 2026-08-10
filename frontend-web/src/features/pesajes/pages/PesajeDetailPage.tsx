import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Ban, Eye, TrendingUp } from 'lucide-react'
import { getPesaje, getPesajeHistory, getPesajeIndicadorAnimal } from '@/features/pesajes/api'
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

function fmtNum(value?: number, digits = 2): string {
  if (value == null || Number.isNaN(value)) return '—'
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(digits)}`
}

function PesajeChart({ puntos }: { puntos: Array<{ fecha: string; pesoKg: number }> }) {
  const width = 560
  const height = 180
  const pad = 30
  if (puntos.length === 0) return null
  const pesos = puntos.map((punto) => punto.pesoKg)
  const min = Math.min(...pesos)
  const max = Math.max(...pesos)
  const range = max - min || 1
  const innerW = width - pad * 2
  const innerH = height - pad * 2
  const x = (i: number) => pad + (puntos.length === 1 ? innerW / 2 : (i / (puntos.length - 1)) * innerW)
  const y = (peso: number) => pad + innerH - ((peso - min) / range) * innerH
  const line = puntos.map((punto, i) => `${x(i)},${y(punto.pesoKg)}`).join(' ')
  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="pesaje-chart" role="img" aria-label="Evolución del peso del animal">
      <line x1={pad} y1={y(min)} x2={width - pad} y2={y(min)} stroke="currentColor" strokeOpacity="0.25" strokeDasharray="4 4" />
      <line x1={pad} y1={y(max)} x2={width - pad} y2={y(max)} stroke="currentColor" strokeOpacity="0.25" strokeDasharray="4 4" />
      <text x={4} y={y(max) + 4} fontSize="11" opacity="0.6">{fmtNum(max, 0)} kg</text>
      <text x={4} y={y(min) + 4} fontSize="11" opacity="0.6">{fmtNum(min, 0)} kg</text>
      <polyline points={line} fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
      {puntos.map((punto, i) => <circle key={`${punto.fecha}-${i}`} cx={x(i)} cy={y(punto.pesoKg)} r="3.5" fill="currentColor" />)}
      <text x={x(0)} y={height - 6} fontSize="11" opacity="0.6">{formatDate(puntos[0].fecha)}</text>
      {puntos.length > 1 && <text x={x(puntos.length - 1)} y={height - 6} fontSize="11" opacity="0.6" textAnchor="end">{formatDate(puntos[puntos.length - 1].fecha)}</text>}
    </svg>
  )
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
    queryFn: () => getPesajeHistory(pesaje?.animalId ?? ''),
    enabled: Boolean(pesaje?.animalId),
  })
  const indicatorsQuery = useQuery({
    queryKey: ['pesaje-indicador', pesaje?.animalId],
    queryFn: () => getPesajeIndicadorAnimal(pesaje?.animalId ?? ''),
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
        void queryClient.invalidateQueries({ queryKey: ['pesaje-indicador', pesaje.animalId] })
      }} />}
      <Card>
        <div className="section-heading"><h3>Datos del control</h3>{pesaje.estado === 'ACTIVO' && <Button variant="danger" onClick={() => setAnullng(pesaje)}><Ban size={17} />Anular pesaje</Button>}</div>
        <dl className="definition-list grid">
          <div><dt>Animal</dt><dd><Link to={`/animales/${pesaje.animalId}`}>{pesaje.codigoAnimal ?? '—'}{pesaje.nombreAnimal ? ` · ${pesaje.nombreAnimal}` : ''}</Link></dd></div>
          <div><dt>Fecha</dt><dd>{formatDate(pesaje.fecha)}</dd></div>
          <div><dt>Peso</dt><dd><strong>{pesaje.pesoKg} kg</strong></dd></div>
          <div><dt>Tipo</dt><dd>{tipoLabel[pesaje.tipo] ?? pesaje.tipo}</dd></div>
          <div><dt>Condición corporal</dt><dd>{pesaje.condicionCorporal ?? '—'}</dd></div>
          <div><dt>Báscula</dt><dd>{pesaje.bascula || '—'}</dd></div>
          <div><dt>Responsable</dt><dd>{pesaje.responsableNombre || '—'}</dd></div>
          <div><dt>Dispositivo</dt><dd>{pesaje.dispositivo || '—'}</dd></div>
          <div><dt>Propiedad</dt><dd>{pesaje.propiedadNombre || '—'}</dd></div>
          <div><dt>Potrero</dt><dd>{pesaje.potreroNombre || '—'}</dd></div>
          <div><dt>Lote</dt><dd>{pesaje.loteNombre || '—'}</dd></div>
          <div><dt>Estado</dt><dd><span className={`status-badge status-${pesaje.estado.toLowerCase()}`}>{pesaje.estado}</span></dd></div>
          {pesaje.estado === 'ANULADO' && <div><dt>Motivo de anulación</dt><dd>{pesaje.motivoAnulacion || '—'}</dd></div>}
          <div><dt>Observaciones</dt><dd>{pesaje.observaciones || '—'}</dd></div>
        </dl>
      </Card>
      <Card>
        <div className="section-heading"><h3>Indicadores de crecimiento</h3><TrendingUp size={18} /></div>
        {indicatorsQuery.isPending && <LoadingState message="Calculando indicadores…" />}
        {indicatorsQuery.error && <Alert tone="danger">{normalizeApiError(indicatorsQuery.error).message}</Alert>}
        {indicatorsQuery.data && (indicatorsQuery.data.ultimoPesoKg == null
          ? <EmptyState title="Sin pesajes" description="Registra pesajes para ver la curva de crecimiento del animal." />
          : <>
            <dl className="definition-list grid">
              <div><dt>Último peso</dt><dd><strong>{fmtNum(indicatorsQuery.data.ultimoPesoKg, 1)} kg</strong>{indicatorsQuery.data.fechaUltimoPesaje && <div className="cell-sub">{formatDate(indicatorsQuery.data.fechaUltimoPesaje)}</div>}</dd></div>
              <div><dt>Variación</dt><dd>{indicatorsQuery.data.variacionKg == null ? '—' : <strong>{fmtNum(indicatorsQuery.data.variacionKg)} kg ({fmtNum(indicatorsQuery.data.variacionPct)}%)</strong>}</dd></div>
              <div><dt>Ganancia diaria</dt><dd>{indicatorsQuery.data.gananciaDiariaKg == null ? '—' : <strong>{fmtNum(indicatorsQuery.data.gananciaDiariaKg, 3)} kg/día</strong>}</dd></div>
              <div><dt>Vs. promedio del lote</dt><dd>{indicatorsQuery.data.promedioLoteKg == null ? '—' : <strong>{fmtNum(indicatorsQuery.data.diferenciaVsLoteKg)} kg ({fmtNum(indicatorsQuery.data.diferenciaVsLotePct)}%)</strong>}</dd></div>
            </dl>
            {indicatorsQuery.data.promedioLoteKg != null && <div className="cell-sub">Promedio del lote: {fmtNum(indicatorsQuery.data.promedioLoteKg, 1)} kg · {indicatorsQuery.data.animalesPesadosLote ?? 0} animal(es) pesado(s)</div>}
            <PesajeChart puntos={indicatorsQuery.data.evolucion} />
          </>)}
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
