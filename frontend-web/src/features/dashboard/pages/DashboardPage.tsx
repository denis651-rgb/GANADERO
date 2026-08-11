import { useMemo } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useLiveQuery } from 'dexie-react-hooks'
import { AlertTriangle, Beef, Boxes, ChevronRight, MapPin, QrCode, RefreshCw, Route, Scale, TrendingUp } from 'lucide-react'
import {
  getDashboardResumen,
  type DashboardAlerta,
  type DashboardDistribucion,
  type DashboardPesajeReciente,
  type DashboardResumen,
} from '@/features/dashboard/api'
import { buildDashboardModel, formatPesoKg, type DashboardScope } from '@/features/dashboard/dashboardModel'
import { useAuth } from '@/auth/auth-context'
import { db } from '@/offline/db'
import { Card } from '@/shared/components/Card'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
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
  return <Card><div className="section-heading"><h3>Pesajes recientes</h3><Link to="/pesajes" className="text-link">Ver todos <ChevronRight size={16} aria-hidden="true" /></Link></div>{items.length ? <ul className="recent-list">{items.map((item) => <li key={item.id}><span className="recent-icon"><Scale size={16} aria-hidden="true" /></span><div><Link to={`/animales/${item.animalId}`}><strong>{item.animalNombre || item.animalCodigo}</strong></Link><span>{new Date(`${item.fecha}T00:00:00`).toLocaleDateString('es-BO')}</span></div><strong>{Number(item.pesoKg).toLocaleString('es-BO', { maximumFractionDigits: 1 })} kg</strong></li>)}</ul> : <p className="muted">Todavía no se registraron pesajes.</p>}</Card>
}

function AlertasCard({ items, animalesSinPesaje, conflictos, pendientes }: { items: DashboardAlerta[]; animalesSinPesaje: number; conflictos: number; pendientes: number }) {
  const hasAttention = items.length > 0 || animalesSinPesaje > 0 || conflictos > 0 || pendientes > 0
  return <Card className="attention-card"><div className="section-heading"><div><span className="eyebrow">Prioridad diaria</span><h2>Atención requerida</h2></div>{!hasAttention && <span className="status-badge status-activo">Todo al día</span>}</div>{hasAttention ? <ul className="dashboard-attention-list">{animalesSinPesaje > 0 && <li><span className="attention-icon"><Scale size={18} aria-hidden="true" /></span><div><strong>{animalesSinPesaje} animales sin pesaje reciente</strong><span>Registra controles para mantener actualizado el seguimiento productivo.</span></div><Link to="/pesajes">Registrar pesaje</Link></li>}{conflictos > 0 && <li className="attention-danger"><span className="attention-icon"><AlertTriangle size={18} aria-hidden="true" /></span><div><strong>{conflictos} conflictos de sincronización</strong><span>Revisa qué versión de los datos debe conservarse.</span></div><Link to="/sincronizacion">Resolver</Link></li>}{pendientes > 0 && <li><span className="attention-icon"><RefreshCw size={18} aria-hidden="true" /></span><div><strong>{pendientes} operaciones pendientes</strong><span>Se enviarán cuando el dispositivo tenga conexión.</span></div><Link to="/sincronizacion">Revisar</Link></li>}{items.map((item) => <li key={item.tipo} className={item.severidad === 'danger' ? 'attention-danger' : ''}><span className="attention-icon"><AlertTriangle size={18} aria-hidden="true" /></span><div><strong>{item.mensaje}</strong><span>{item.total} registro(s) requieren atención.</span></div></li>)}</ul> : <p className="attention-empty">No hay tareas urgentes. El hato está al día con la información disponible.</p>}</Card>
}

export function DashboardPage() {
  const online = useOnlineStatus()
  const { user, can } = useAuth()
  const resumenQuery = useQuery({ queryKey: ['dashboard-resumen'], queryFn: getDashboardResumen })
  const backendError = resumenQuery.error ? normalizeApiError(resumenQuery.error) : null
  const resumen = resumenQuery.data ?? EMPTY_RESUMEN
  const pending = useLiveQuery(() => db.operacionesPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'ERROR').count(), [], 0)
  const conflictos = useLiveQuery(() => db.operacionesPendientes.where('status').equals('CONFLICT').count(), [], 0)
  const pendingFiles = useLiveQuery(() => db.archivosPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'ERROR').count(), [], 0)
  const lastSync = useLiveQuery(() => db.estadoSincronizacion.get('lastSyncAt'))
  const scope: DashboardScope = user?.propertyIds?.length ? 'PROPIEDADES_ASIGNADAS' : 'TODA_EMPRESA'
  const model = useMemo(() => buildDashboardModel(resumen, { operacionesPendientes: pending ?? 0, conflictos: conflictos ?? 0, archivosPendientes: pendingFiles ?? 0, ultimaSincronizacion: lastSync?.value }, scope), [resumen, pending, conflictos, pendingFiles, lastSync, scope])
  const { resumen: r, local } = model
  const pendingTotal = local.operacionesPendientes + local.archivosPendientes

  return <div className="page-stack dashboard-page">
    <PageHeader eyebrow="Resumen del hato" title={`Buen día${user?.displayName ? `, ${user.displayName.split(' ')[0]}` : ''}`} description={scope === 'TODA_EMPRESA' ? 'Esto es lo más importante de tu operación ganadera.' : 'Indicadores de las propiedades que tienes asignadas.'} />
    {!online && <Alert tone="info" title="Trabajando sin conexión">Estás viendo los últimos datos descargados. Las operaciones compatibles quedarán pendientes para sincronizar.</Alert>}
    {backendError && backendError.code !== 'NETWORK_ERROR' && <Alert tone="danger" title="No se pudo actualizar el resumen">{backendError.message}</Alert>}

    <AlertasCard items={r.alertas} animalesSinPesaje={r.animalesSinPesaje} conflictos={local.conflictos} pendientes={pendingTotal} />

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

    <Card className="field-summary"><div className="dashboard-section-heading"><div><span className="eyebrow">Campo</span><h2>Estado operativo</h2></div></div><dl className="dashboard-facts"><div><dt><MapPin size={17} aria-hidden="true" />En potrero</dt><dd>{r.animalesEnPotrero}</dd></div><div><dt><Boxes size={17} aria-hidden="true" />Lotes activos</dt><dd>{r.lotesActivos}</dd></div><div><dt><MapPin size={17} aria-hidden="true" />Potreros activos</dt><dd>{r.potrerosActivos}</dd></div><div><dt><Scale size={17} aria-hidden="true" />Pesajes · 7 días</dt><dd>{r.pesajesUltimos7Dias}</dd></div><div><dt><Route size={17} aria-hidden="true" />Movimientos · 7 días</dt><dd>{r.movimientosUltimos7Dias}</dd></div></dl></Card>

    <div className="two-column-grid dashboard-activity"><PesajesCard items={r.pesajesRecientes} /><Card><div className="section-heading"><h3>Estado de sincronización</h3><Link to="/sincronizacion" className="text-link">Ver detalle <ChevronRight size={16} aria-hidden="true" /></Link></div><div className={`sync-summary ${online && !pendingTotal && !local.conflictos ? 'sync-summary-ok' : ''}`}><RefreshCw size={24} aria-hidden="true" /><div><strong>{!online ? 'Sin conexión' : local.conflictos ? 'Requiere revisión' : pendingTotal ? 'Cambios pendientes' : 'Datos sincronizados'}</strong><span>{local.ultimaSincronizacion ? `Última sincronización: ${new Date(local.ultimaSincronizacion).toLocaleString('es-BO')}` : 'Todavía no se realizó una sincronización completa.'}</span></div></div></Card></div>

    <section aria-labelledby="distribution-title"><div className="dashboard-section-heading"><div><span className="eyebrow">Composición</span><h2 id="distribution-title">Distribución del hato</h2></div></div><div className="distribucion-grid"><DistribucionCard title="Por categoría" items={r.animalesPorCategoria} /><DistribucionCard title="Por potrero" items={r.animalesPorPotrero} /><DistribucionCard title="Por lote" items={r.animalesPorLote} /></div></section>
  </div>
}
