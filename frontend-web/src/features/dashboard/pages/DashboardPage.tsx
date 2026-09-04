import { useMemo } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { AlertCircle, AlertTriangle, Beef, Boxes, ChevronRight, Info, MapPin, QrCode, Route, Scale, TrendingUp } from 'lucide-react'
import {
  getDashboardResumen,
  type DashboardDistribucion,
  type DashboardPesajeReciente,
  type DashboardResumen,
} from '@/features/dashboard/api'
import { buildDashboardModel, formatPesoKg, type AttentionItem } from '@/features/dashboard/dashboardModel'
import { useAuth } from '@/auth/auth-context'
import { Card } from '@/shared/components/Card'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'

const EMPTY_RESUMEN: DashboardResumen = {
  totalAnimales: 0, animalesEnPotrero: 0, lotesActivos: 0, potrerosActivos: 0,
  pesoPromedioKg: undefined, gananciaPromedioKg: undefined, pesajesUltimos7Dias: 0,
  movimientosUltimos7Dias: 0, animalesSinPesaje: 0, animalesPorCategoria: [],
  animalesPorPotrero: [], animalesPorLote: [], pesajesRecientes: [], alertas: [], generadoEn: '',
}

function DistribucionCard({ title, items }: { title: string; items: DashboardDistribucion[] }) {
  const max = Math.max(1, ...items.map((item) => item.total))
  return <Card className="dashboard-distribution"><h3>{title}</h3>{items.length ? <ul className="distribucion-list">{items.map((item) => <li key={item.nombre}><span>{item.nombre}</span><div className="distribucion-bar"><i style={{ width: `${Math.max(4, (item.total / max) * 100)}%` }} /></div><strong>{item.total}</strong></li>)}</ul> : <p className="muted">Sin registros.</p>}</Card>
}

function PesajesCard({ items }: { items: DashboardPesajeReciente[] }) {
  return <Card><div className="section-heading"><h3>Pesajes recientes</h3><Link to="/pesajes" className="text-link">Ver todos <ChevronRight size={16} aria-hidden="true" /></Link></div>{items.length ? <div className="table-wrapper"><table className="dashboard-pesajes-table"><caption className="visually-hidden">Pesajes registrados más recientemente</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha</th><th scope="col" className="numeric">Peso</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td><Link to={`/animales/${item.animalId}`}>{item.animalNombre || item.animalCodigo}</Link></td><td>{new Date(`${item.fecha}T00:00:00`).toLocaleDateString('es-BO')}</td><td className="numeric">{formatPesoKg(item.pesoKg)}</td></tr>)}</tbody></table></div> : <p className="muted">Todavía no se registraron pesajes.</p>}</Card>
}

const ATTENTION_ICONS: Record<AttentionItem['severidad'], typeof AlertCircle> = {
  danger: AlertCircle,
  warning: AlertTriangle,
  info: Info,
}

function AlertasCard({ items }: { items: AttentionItem[] }) {
  const hasAttention = items.length > 0
  return <Card className="attention-card"><div className="section-heading"><div><span className="eyebrow">Prioridad diaria</span><h2>Atención requerida</h2></div>{!hasAttention && <span className="status-badge status-activo">Todo al día</span>}</div>{hasAttention ? <ul className="attention-list">{items.map((item) => {
    const Icon = ATTENTION_ICONS[item.severidad]
    return <li key={item.key} className={`attention-${item.severidad}`}><span className="attention-icon"><Icon size={18} aria-hidden="true" /></span><div><strong>{item.mensaje}</strong><span>{item.detalle}</span></div>{item.actionHref && <Link to={item.actionHref}>{item.actionLabel}</Link>}</li>
  })}</ul> : <p className="attention-empty">No hay tareas urgentes. El hato está al día con la información disponible.</p>}</Card>
}

export function DashboardPage() {
  const { user, can } = useAuth()
  const resumenQuery = useQuery({ queryKey: ['dashboard-resumen'], queryFn: getDashboardResumen })
  const backendError = resumenQuery.error ? normalizeApiError(resumenQuery.error) : null
  const resumen = resumenQuery.data ?? EMPTY_RESUMEN
  const model = useMemo(() => buildDashboardModel(resumen), [resumen])
  const { resumen: r } = model

  return <div className="page-stack dashboard-page">
    <PageHeader eyebrow="Resumen del hato" title={`Buen día${user.displayName ? `, ${user.displayName.split(' ')[0]}` : ''}`} description="Esto es lo más importante de tu operación ganadera." />
    {backendError && backendError.code !== 'NETWORK_ERROR' && <Alert tone="danger" title="No se pudo actualizar el resumen">{backendError.message}</Alert>}

    <AlertasCard items={model.attentionItems} />

    <section aria-labelledby="dashboard-metrics-title"><div className="dashboard-section-heading"><div><span className="eyebrow">Situación actual</span><h2 id="dashboard-metrics-title">Resumen productivo</h2></div>{r.generadoEn && <span className="dashboard-updated">Actualizado {new Date(r.generadoEn).toLocaleString('es-BO')}</span>}</div><div className="metric-grid dashboard-metrics">
      <Card className="metric-card metric-primary"><span className="metric-icon"><Beef size={22} aria-hidden="true" /></span><div><span>Animales activos</span><strong>{r.totalAnimales.toLocaleString('es-BO')}</strong><small>{r.animalesEnPotrero} ubicados en potreros</small></div></Card>
      <Card className="metric-card"><span className="metric-icon"><Scale size={22} aria-hidden="true" /></span><div><span>Peso promedio</span><strong>{formatPesoKg(r.pesoPromedioKg)}</strong><small>{r.pesajesUltimos7Dias} pesajes en 7 días</small></div></Card>
      <Card className="metric-card"><span className="metric-icon"><TrendingUp size={22} aria-hidden="true" /></span><div><span>Ganancia diaria</span><strong>{r.gananciaPromedioKg != null ? `${r.gananciaPromedioKg.toLocaleString('es-BO', { maximumFractionDigits: 2 })} kg` : '—'}</strong><small>Promedio por animal</small></div></Card>
      <Card className={`metric-card ${r.animalesSinPesaje > 0 ? 'metric-warning' : ''}`}><span className="metric-icon"><AlertTriangle size={22} aria-hidden="true" /></span><div><span>Sin pesaje reciente</span><strong>{r.animalesSinPesaje}</strong><small>{r.animalesSinPesaje ? 'Requieren seguimiento' : 'Todos controlados'}</small></div></Card>
    </div></section>

    <section aria-labelledby="quick-actions-title"><div className="dashboard-section-heading"><div><span className="eyebrow">Trabajo diario</span><h2 id="quick-actions-title">Acciones rápidas</h2></div></div><div className="quick-action-grid">
      {can('ANIMAL_CREAR') && <Link to="/animales/nuevo" className="quick-action"><span><Beef size={22} aria-hidden="true" /></span><div><strong>Registrar animal</strong><small>Agregar una ficha al hato</small></div><ChevronRight size={18} aria-hidden="true" /></Link>}
      {can('PESAJE_REGISTRAR') && <Link to="/pesajes" className="quick-action"><span><Scale size={22} aria-hidden="true" /></span><div><strong>Registrar pesaje</strong><small>Control individual o por lote</small></div><ChevronRight size={18} aria-hidden="true" /></Link>}
      {can('MOVIMIENTO_CREAR') && <Link to="/movimientos" className="quick-action"><span><Route size={22} aria-hidden="true" /></span><div><strong>Crear movimiento</strong><small>Trasladar animales con control</small></div><ChevronRight size={18} aria-hidden="true" /></Link>}
      {can('ANIMAL_VER') && <Link to="/qr/escanear" className="quick-action"><span><QrCode size={22} aria-hidden="true" /></span><div><strong>Escanear QR</strong><small>Abrir rápidamente una ficha</small></div><ChevronRight size={18} aria-hidden="true" /></Link>}
    </div></section>

    <Card><div className="dashboard-section-heading"><div><span className="eyebrow">Campo</span><h2>Estado operativo</h2></div></div><dl className="dashboard-facts"><div><dt><MapPin size={17} aria-hidden="true" />En potrero</dt><dd>{r.animalesEnPotrero}</dd></div><div><dt><Boxes size={17} aria-hidden="true" />Lotes activos</dt><dd>{r.lotesActivos}</dd></div><div><dt><MapPin size={17} aria-hidden="true" />Potreros activos</dt><dd>{r.potrerosActivos}</dd></div><div><dt><Scale size={17} aria-hidden="true" />Pesajes · 7 días</dt><dd>{r.pesajesUltimos7Dias}</dd></div><div><dt><Route size={17} aria-hidden="true" />Movimientos · 7 días</dt><dd>{r.movimientosUltimos7Dias}</dd></div></dl></Card>

    <div className="two-column-grid dashboard-activity"><PesajesCard items={r.pesajesRecientes} /></div>

    <section aria-labelledby="distribution-title"><div className="dashboard-section-heading"><div><span className="eyebrow">Composición</span><h2 id="distribution-title">Distribución del hato</h2></div></div><div className="distribucion-grid"><DistribucionCard title="Por categoría" items={r.animalesPorCategoria} /><DistribucionCard title="Por potrero" items={r.animalesPorPotrero} /><DistribucionCard title="Por lote" items={r.animalesPorLote} /></div></section>
  </div>
}
