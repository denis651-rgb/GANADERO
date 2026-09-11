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
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'

const EMPTY_RESUMEN: DashboardResumen = {
  totalAnimales: 0, animalesEnPotrero: 0, lotesActivos: 0, potrerosActivos: 0,
  pesoPromedioKg: undefined, gananciaPromedioKg: undefined, pesajesUltimos7Dias: 0,
  movimientosUltimos7Dias: 0, animalesSinPesaje: 0, animalesPorCategoria: [],
  animalesPorPotrero: [], animalesPorLote: [], pesajesRecientes: [], alertas: [], generadoEn: '',
}

const ATTENTION_ICONS: Record<AttentionItem['severidad'], typeof AlertCircle> = {
  danger: AlertCircle,
  warning: AlertTriangle,
  info: Info,
}

function DistribucionBlock({ title, items }: { title: string; items: DashboardDistribucion[] }) {
  const max = Math.max(1, ...items.map((item) => item.total))
  return <div className="dp-dist-block">
    <p className="dp-dist-title">{title}</p>
    {items.length ? items.map((item) => <div key={item.nombre} className="dp-dist-row">
      <span className="dp-dist-name">{item.nombre}</span>
      <span className="dp-dist-bar"><i style={{ width: `${Math.max(4, (item.total / max) * 100)}%` }} /></span>
      <span className="dp-dist-count">{item.total}</span>
    </div>) : <p className="muted">Sin registros.</p>}
  </div>
}

export function DashboardPage() {
  const { user, can } = useAuth()
  const resumenQuery = useQuery({ queryKey: ['dashboard-resumen'], queryFn: getDashboardResumen })
  const backendError = resumenQuery.error ? normalizeApiError(resumenQuery.error) : null
  const resumen = resumenQuery.data ?? EMPTY_RESUMEN
  const model = useMemo(() => buildDashboardModel(resumen), [resumen])
  const { resumen: r, attentionItems } = model
  const hasAttention = attentionItems.length > 0
  const nombre = user.displayName ? user.displayName.split(' ')[0] : ''

  return <div className="page-stack dashboard-page dashboard-institutional">
    {backendError && backendError.code !== 'NETWORK_ERROR' && <Alert tone="danger" title="No se pudo actualizar el resumen">{backendError.message}</Alert>}

    <section className="dp-hero">
      <div className="dp-hero-text">
        <span className="eyebrow">Panorama operativo</span>
        <h1>Buen día{nombre ? `, ${nombre}` : ''}</h1>
        <p>{r.generadoEn ? `Resumen actualizado ${new Date(r.generadoEn).toLocaleString('es-BO')}` : 'Esto es lo más importante de tu operación ganadera.'}</p>
      </div>
      <div className="dp-hero-stats">
        <div className="dp-hero-stat"><strong>{r.totalAnimales.toLocaleString('es-BO')}</strong><span>Animales activos</span></div>
        <div className="dp-hero-stat"><strong>{formatPesoKg(r.pesoPromedioKg)}</strong><span>Peso promedio</span></div>
        <div className="dp-hero-stat"><strong>{r.gananciaPromedioKg != null ? `${r.gananciaPromedioKg.toLocaleString('es-BO', { maximumFractionDigits: 2 })} kg` : '—'}</strong><span>Ganancia diaria</span></div>
      </div>
    </section>

    <section aria-labelledby="dp-attention-title">
      <div className="dp-section-head">
        <div><span className="eyebrow">Prioridad diaria</span><h2 id="dp-attention-title">Atención requerida</h2></div>
        <span className="dp-badge">{hasAttention ? `${attentionItems.length} pendientes` : 'Todo al día'}</span>
      </div>
      <div className="card dp-panel">
        {hasAttention ? <ul className="dp-att-list">{attentionItems.map((item) => {
          const Icon = ATTENTION_ICONS[item.severidad]
          const dotColor = item.severidad === 'danger' ? 'var(--danger)' : item.severidad === 'warning' ? 'var(--warning)' : 'var(--info)'
          return <li key={item.key} className="dp-att-row">
            <span className="dp-att-dot" style={{ background: dotColor }} aria-hidden="true" />
            <Icon size={16} aria-hidden="true" style={{ color: dotColor, flex: '0 0 auto' }} />
            <div className="dp-att-body"><strong>{item.mensaje}</strong><span>{item.detalle}</span></div>
            {item.actionHref && <Link to={item.actionHref}>{item.actionLabel}</Link>}
          </li>
        })}</ul> : <p className="dp-att-empty">No hay tareas urgentes. El hato está al día con la información disponible.</p>}
      </div>
    </section>

    <section aria-labelledby="dp-metrics-title">
      <div className="dp-section-head">
        <div><span className="eyebrow">Situación actual</span><h2 id="dp-metrics-title">Resumen productivo</h2></div>
        {r.generadoEn && <span className="dp-section-note">Actualizado {new Date(r.generadoEn).toLocaleString('es-BO')}</span>}
      </div>
      <div className="card dp-kpi-grid">
        <div className="dp-kpi-card">
          <span className="dp-kpi-icon"><Beef size={17} aria-hidden="true" /></span>
          <span className="dp-kpi-label">Animales activos</span>
          <strong>{r.totalAnimales.toLocaleString('es-BO')}</strong>
          <small>{r.animalesEnPotrero} ubicados en potreros</small>
        </div>
        <div className="dp-kpi-card">
          <span className="dp-kpi-icon"><Scale size={17} aria-hidden="true" /></span>
          <span className="dp-kpi-label">Peso promedio</span>
          <strong>{formatPesoKg(r.pesoPromedioKg)}</strong>
          <small>{r.pesajesUltimos7Dias} pesajes en 7 días</small>
        </div>
        <div className="dp-kpi-card">
          <span className="dp-kpi-icon"><TrendingUp size={17} aria-hidden="true" /></span>
          <span className="dp-kpi-label">Ganancia diaria</span>
          <strong>{r.gananciaPromedioKg != null ? `${r.gananciaPromedioKg.toLocaleString('es-BO', { maximumFractionDigits: 2 })} kg` : '—'}</strong>
          <small>Promedio por animal</small>
        </div>
        <div className={`dp-kpi-card ${r.animalesSinPesaje > 0 ? 'is-warning' : ''}`}>
          <span className="dp-kpi-icon"><AlertTriangle size={17} aria-hidden="true" /></span>
          <span className="dp-kpi-label">Sin pesaje reciente</span>
          <strong>{r.animalesSinPesaje}</strong>
          <small>{r.animalesSinPesaje ? 'Requieren seguimiento' : 'Todos controlados'}</small>
        </div>
      </div>
    </section>

    <section aria-labelledby="dp-quick-title">
      <div className="dp-section-head"><div><span className="eyebrow">Trabajo diario</span><h2 id="dp-quick-title">Acciones rápidas</h2></div></div>
      <div className="dp-quick-grid">
        {can('ANIMAL_CREAR') && <Link to="/animales/nuevo" className="dp-quick-tile"><span className="dp-qt-icon"><Beef size={18} aria-hidden="true" /></span><div><strong>Registrar animal</strong><small>Agregar una ficha al hato</small></div><ChevronRight size={17} aria-hidden="true" className="dp-qt-chev" /></Link>}
        {can('PESAJE_REGISTRAR') && <Link to="/pesajes" className="dp-quick-tile"><span className="dp-qt-icon"><Scale size={18} aria-hidden="true" /></span><div><strong>Registrar pesaje</strong><small>Control individual o por lote</small></div><ChevronRight size={17} aria-hidden="true" className="dp-qt-chev" /></Link>}
        {can('MOVIMIENTO_CREAR') && <Link to="/movimientos" className="dp-quick-tile"><span className="dp-qt-icon"><Route size={18} aria-hidden="true" /></span><div><strong>Crear movimiento</strong><small>Trasladar animales con control</small></div><ChevronRight size={17} aria-hidden="true" className="dp-qt-chev" /></Link>}
        {can('ANIMAL_VER') && <Link to="/qr/escanear" className="dp-quick-tile"><span className="dp-qt-icon"><QrCode size={18} aria-hidden="true" /></span><div><strong>Escanear QR</strong><small>Abrir rápidamente una ficha</small></div><ChevronRight size={17} aria-hidden="true" className="dp-qt-chev" /></Link>}
      </div>
    </section>

    <section aria-labelledby="dp-facts-title">
      <div className="dp-section-head"><div><span className="eyebrow">Campo</span><h2 id="dp-facts-title">Estado operativo</h2></div></div>
      <div className="card dp-panel">
        <div className="dp-facts-grid">
          <div>
            <div className="dp-fact-row"><span className="dp-fact-label"><MapPin size={15} aria-hidden="true" />En potrero</span><span className="dp-fact-leader" /><span className="dp-fact-value">{r.animalesEnPotrero}</span></div>
            <div className="dp-fact-row"><span className="dp-fact-label"><Boxes size={15} aria-hidden="true" />Lotes activos</span><span className="dp-fact-leader" /><span className="dp-fact-value">{r.lotesActivos}</span></div>
            <div className="dp-fact-row"><span className="dp-fact-label"><MapPin size={15} aria-hidden="true" />Potreros activos</span><span className="dp-fact-leader" /><span className="dp-fact-value">{r.potrerosActivos}</span></div>
          </div>
          <div>
            <div className="dp-fact-row"><span className="dp-fact-label"><Scale size={15} aria-hidden="true" />Pesajes · 7 días</span><span className="dp-fact-leader" /><span className="dp-fact-value">{r.pesajesUltimos7Dias}</span></div>
            <div className="dp-fact-row"><span className="dp-fact-label"><Route size={15} aria-hidden="true" />Movimientos · 7 días</span><span className="dp-fact-leader" /><span className="dp-fact-value">{r.movimientosUltimos7Dias}</span></div>
          </div>
        </div>
      </div>
    </section>

    <section aria-labelledby="dp-activity-title">
      <div className="dp-two-col">
        <div>
          <div className="dp-section-head"><div><span className="eyebrow">Trazabilidad</span><h2 id="dp-activity-title">Pesajes recientes</h2></div><Link to="/pesajes" className="text-link">Ver todos <ChevronRight size={16} aria-hidden="true" /></Link></div>
          <div className="card dp-panel">
            {r.pesajesRecientes.length ? <table className="dp-table"><caption className="visually-hidden">Pesajes registrados más recientemente</caption><thead><tr><th scope="col">Animal</th><th scope="col">Fecha</th><th scope="col" className="dp-num">Peso</th></tr></thead><tbody>{r.pesajesRecientes.map((item: DashboardPesajeReciente) => <tr key={item.id}><td><Link to={`/animales/${item.animalId}`}>{item.animalNombre || item.animalCodigo}</Link></td><td className="dp-date">{new Date(`${item.fecha}T00:00:00`).toLocaleDateString('es-BO')}</td><td className="dp-num">{formatPesoKg(item.pesoKg)}</td></tr>)}</tbody></table> : <p className="muted">Todavía no se registraron pesajes.</p>}
          </div>
        </div>
        <div>
          <div className="dp-section-head"><div><span className="eyebrow">Composición</span><h2>Distribución del hato</h2></div></div>
          <div className="card dp-panel">
            <DistribucionBlock title="Por categoría" items={r.animalesPorCategoria} />
            <DistribucionBlock title="Por potrero" items={r.animalesPorPotrero} />
            <DistribucionBlock title="Por lote" items={r.animalesPorLote} />
          </div>
        </div>
      </div>
    </section>
  </div>
}
