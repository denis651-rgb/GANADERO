import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useLiveQuery } from 'dexie-react-hooks'
import {
  AlertTriangle,
  Beef,
  Boxes,
  CheckCircle2,
  FileImage,
  MapPin,
  Scale,
  Server,
  Tractor,
  WifiOff,
} from 'lucide-react'
import {
  getDashboardResumen,
  type DashboardAlerta,
  type DashboardDistribucion,
  type DashboardPesajeReciente,
  type DashboardResumen,
} from '@/features/dashboard/api'
import { buildDashboardModel, formatPesoKg, type DashboardScope } from '@/features/dashboard/dashboardModel'
import { appModules } from '@/app/modules'
import { useAuth } from '@/auth/auth-context'
import { db } from '@/offline/db'
import { Card } from '@/shared/components/Card'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
import { normalizeApiError } from '@/shared/api/errors'

const EMPTY_RESUMEN: DashboardResumen = {
  totalAnimales: 0,
  animalesEnPotrero: 0,
  lotesActivos: 0,
  potrerosActivos: 0,
  pesoPromedioKg: undefined,
  gananciaPromedioKg: undefined,
  pesajesUltimos7Dias: 0,
  movimientosUltimos7Dias: 0,
  animalesSinPesaje: 0,
  animalesPorCategoria: [],
  animalesPorPotrero: [],
  animalesPorLote: [],
  pesajesRecientes: [],
  alertas: [],
  generadoEn: '',
}

function DistribucionCard({ title, items }: { title: string; items: DashboardDistribucion[] }) {
  const max = Math.max(1, ...items.map((item) => item.total))
  return (
    <Card>
      <h3>{title}</h3>
      {items.length ? (
        <ul className="distribucion-list">
          {items.map((item) => (
            <li key={item.nombre}>
              <span>{item.nombre}</span>
              <div className="distribucion-bar"><i style={{ width: `${Math.max(4, (item.total / max) * 100)}%` }} /></div>
              <strong>{item.total}</strong>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">Sin registros.</p>
      )}
    </Card>
  )
}

function PesajesCard({ items }: { items: DashboardPesajeReciente[] }) {
  return (
    <Card>
      <h3>Pesajes recientes</h3>
      {items.length ? (
        <ul className="recent-list">
          {items.map((item) => (
            <li key={item.id}>
              <span className="recent-icon"><Scale size={16} /></span>
              <div>
                <strong>{item.animalNombre || item.animalCodigo}</strong>
                <span>{new Date(`${item.fecha}T00:00:00`).toLocaleDateString('es-BO')}</span>
              </div>
              <strong>{Number(item.pesoKg).toLocaleString('es-BO', { maximumFractionDigits: 1 })} kg</strong>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">Sin pesajes recientes.</p>
      )}
    </Card>
  )
}

function AlertasCard({ items }: { items: DashboardAlerta[] }) {
  return (
    <Card>
      <h3>Alertas básicas</h3>
      {items.length ? (
        <ul className="alertas-list">
          {items.map((alerta) => (
            <li key={alerta.tipo} className={`alerta-item alerta-${alerta.severidad}`}>
              {alerta.severidad === 'danger' ? <AlertTriangle size={17} /> : <CheckCircle2 size={17} />}
              <span>{alerta.mensaje}</span>
              <strong>{alerta.total}</strong>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">Sin alertas pendientes.</p>
      )}
    </Card>
  )
}

export function DashboardPage() {
  const online = useOnlineStatus()
  const { user } = useAuth()
  const resumenQuery = useQuery({ queryKey: ['dashboard-resumen'], queryFn: getDashboardResumen })
  const backendError = resumenQuery.error ? normalizeApiError(resumenQuery.error) : null
  const resumen = resumenQuery.data ?? EMPTY_RESUMEN

  const pending = useLiveQuery(() => db.operacionesPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'ERROR').count(), [], 0)
  const conflictos = useLiveQuery(() => db.operacionesPendientes.where('status').equals('CONFLICT').count(), [], 0)
  const pendingFiles = useLiveQuery(() => db.archivosPendientes.where('status').anyOf('PENDING', 'RETRYABLE', 'ERROR').count(), [], 0)
  const lastSync = useLiveQuery(() => db.estadoSincronizacion.get('lastSyncAt'))
  const propiedades = useLiveQuery(() => db.catalogos.where('type').equals('PROPIEDAD').toArray(), [], [])
  const animalesCount = useLiveQuery(() => db.animalesResumen.count(), [], 0)
  const lotesCount = useLiveQuery(() => db.lotes.count(), [], 0)
  const potrerosCount = useLiveQuery(() => db.potreros.count(), [], 0)

  const scope: DashboardScope = user?.propertyIds?.length ? 'PROPIEDADES_ASIGNADAS' : 'TODA_EMPRESA'

  const model = useMemo(() => buildDashboardModel(
    resumen,
    {
      operacionesPendientes: pending ?? 0,
      conflictos: conflictos ?? 0,
      archivosPendientes: pendingFiles ?? 0,
      ultimaSincronizacion: lastSync?.value,
    },
    scope,
  ), [resumen, pending, conflictos, pendingFiles, lastSync, scope])

  const { resumen: r, local } = model
  const completados = appModules.filter((item) => item.status === 'LISTO').length
  const enDesarrollo = appModules.filter((item) => item.status === 'EN_DESARROLLO').length
  const proximamente = appModules.filter((item) => item.status === 'PROXIMAMENTE').length

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Resumen operativo"
        title="Panel principal"
        description={scope === 'TODA_EMPRESA' ? 'Indicadores de toda la empresa.' : 'Indicadores de las propiedades asignadas.'}
      />

      {!online && <Alert tone="info" title="Trabajando sin conexión">Los indicadores muestran los datos descargados; las operaciones compatibles se guardarán localmente hasta sincronizar.</Alert>}
      {backendError && <Alert tone="danger" title="No se pudo cargar el resumen">{backendError.message}</Alert>}

      <div className="metric-grid">
        <Card className="metric-card">
          <span className="metric-icon"><Server size={22} /></span>
          <div><span>Red</span><strong>{online ? 'En línea' : 'Sin conexión'}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><Beef size={22} /></span>
          <div><span>Animales activos</span><strong>{r.totalAnimales}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><Boxes size={22} /></span>
          <div><span>Módulos completados</span><strong>{completados}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><WifiOff size={22} /></span>
          <div><span>Pendientes locales</span><strong>{local.operacionesPendientes}</strong></div>
        </Card>
      </div>

      <Card>
        <h3>Indicadores de producción</h3>
        {!model.tieneDatos && <p className="muted">Sin registros todavía: crea animales, lotes o potreros para ver indicadores.</p>}
        <dl className="definition-list grid">
          <div><dt>Animales en potrero</dt><dd>{r.animalesEnPotrero}</dd></div>
          <div><dt>Lotes activos</dt><dd>{r.lotesActivos}</dd></div>
          <div><dt>Potreros activos</dt><dd>{r.potrerosActivos}</dd></div>
          <div><dt>Peso promedio</dt><dd>{formatPesoKg(r.pesoPromedioKg)}</dd></div>
          <div><dt>Ganancia promedio</dt><dd>{r.gananciaPromedioKg != null ? `${r.gananciaPromedioKg.toLocaleString('es-BO', { maximumFractionDigits: 2 })} kg/día` : '—'}</dd></div>
          <div><dt>Animales sin pesaje</dt><dd>{r.animalesSinPesaje}</dd></div>
          <div><dt>Pesajes últimos 7 días</dt><dd>{r.pesajesUltimos7Dias}</dd></div>
          <div><dt>Movimientos últimos 7 días</dt><dd>{r.movimientosUltimos7Dias}</dd></div>
        </dl>
      </Card>

      <div className="distribucion-grid">
        <DistribucionCard title="Animales por categoría" items={r.animalesPorCategoria} />
        <DistribucionCard title="Animales por potrero" items={r.animalesPorPotrero} />
        <DistribucionCard title="Animales por lote" items={r.animalesPorLote} />
      </div>

      <div className="two-column-grid">
        <PesajesCard items={r.pesajesRecientes} />
        <AlertasCard items={r.alertas} />
      </div>

      <div className="two-column-grid">
        <Card>
          <h3>Sincronización y datos locales</h3>
          <div className="offline-data-grid">
            <div><span className="metric-icon"><WifiOff size={20} /></span><div><span>Operaciones pendientes</span><strong>{local.operacionesPendientes}</strong></div></div>
            <div><span className="metric-icon"><AlertTriangle size={20} /></span><div><span>Conflictos</span><strong>{local.conflictos}</strong></div></div>
            <div><span className="metric-icon"><FileImage size={20} /></span><div><span>Archivos pendientes</span><strong>{local.archivosPendientes}</strong></div></div>
            <div><span className="metric-icon"><MapPin size={20} /></span><div><span>Propiedad(es)</span><strong>{propiedades?.length ? propiedades.map((item) => item.name).join(', ') : 'Sin datos'}</strong></div></div>
            <div><span className="metric-icon"><Beef size={20} /></span><div><span>Animales descargados</span><strong>{animalesCount ?? 0}</strong></div></div>
            <div><span className="metric-icon"><Tractor size={20} /></span><div><span>Lotes / Potreros</span><strong>{lotesCount ?? 0} / {potrerosCount ?? 0}</strong></div></div>
          </div>
          <p className="muted">Última sincronización: {local.ultimaSincronizacion ? new Date(local.ultimaSincronizacion).toLocaleString('es-BO') : 'todavía no realizada'}</p>
        </Card>
        <Card>
          <h3>Módulos</h3>
          <dl className="definition-list">
            <div><dt>Completados</dt><dd>{completados}</dd></div>
            <div><dt>En desarrollo</dt><dd>{enDesarrollo}</dd></div>
            <div><dt>Próximamente</dt><dd>{proximamente}</dd></div>
          </dl>
          <ul className="check-list compact">
            {appModules.filter((item) => item.status !== 'LISTO').map((item) => (
              <li key={item.key}><span className={`module-status module-status-${item.status.toLowerCase()}`}>{item.status === 'EN_DESARROLLO' ? 'En desarrollo' : 'Próximamente'}</span>{item.label}</li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  )
}
