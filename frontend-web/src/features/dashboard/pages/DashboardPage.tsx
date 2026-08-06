import { useQuery } from '@tanstack/react-query'
import { useLiveQuery } from 'dexie-react-hooks'
import { Beef, Boxes, Server, WifiOff } from 'lucide-react'
import { getSystemStatus } from '@/features/sistema/api'
import { db } from '@/offline/db'
import { Card } from '@/shared/components/Card'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'
import { normalizeApiError } from '@/shared/api/errors'

export function DashboardPage() {
  const online = useOnlineStatus()
  const pending = useLiveQuery(() => db.operacionesPendientes.where('status').anyOf('PENDING', 'ERROR').count(), [], 0)
  const statusQuery = useQuery({ queryKey: ['system-status'], queryFn: getSystemStatus })
  const backendError = statusQuery.error ? normalizeApiError(statusQuery.error) : null

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Resumen operativo"
        title="Panel principal"
        description="Estado técnico y punto de entrada de los módulos de GANADERO."
      />

      {!online && <Alert tone="info" title="Trabajando sin conexión">Las operaciones compatibles se guardarán localmente hasta sincronizar.</Alert>}
      {backendError && <Alert tone="danger" title="Backend no disponible">{backendError.message}</Alert>}

      <div className="metric-grid">
        <Card className="metric-card">
          <span className="metric-icon"><Server size={22} /></span>
          <div><span>Backend</span><strong>{statusQuery.isSuccess ? 'Disponible' : statusQuery.isPending ? 'Comprobando' : 'Sin conexión'}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><Boxes size={22} /></span>
          <div><span>Módulos registrados</span><strong>{statusQuery.data?.moduleCount ?? 20}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><WifiOff size={22} /></span>
          <div><span>Pendientes locales</span><strong>{pending ?? 0}</strong></div>
        </Card>
        <Card className="metric-card">
          <span className="metric-icon"><Beef size={22} /></span>
          <div><span>Animales activos</span><strong>—</strong></div>
        </Card>
      </div>

      <div className="two-column-grid">
        <Card>
          <h3>Estado del sistema</h3>
          <dl className="definition-list">
            <div><dt>Aplicación</dt><dd>{statusQuery.data?.application ?? 'GANADERO'}</dd></div>
            <div><dt>Arquitectura</dt><dd>{statusQuery.data?.architecture ?? 'MODULAR_MONOLITH'}</dd></div>
            <div><dt>Fase del backend</dt><dd>{statusQuery.data?.phase ?? 'FASE_0'}</dd></div>
            <div><dt>Red</dt><dd>{online ? 'En línea' : 'Sin conexión'}</dd></div>
          </dl>
        </Card>
        <Card>
          <h3>Próximo flujo funcional</h3>
          <ol className="steps-list">
            <li>Configurar empresa y usuario propietario.</li>
            <li>Crear propiedades, sectores y potreros.</li>
            <li>Registrar animales e identificadores.</li>
            <li>Crear lotes y confirmar movimientos.</li>
            <li>Verificar línea de tiempo y auditoría.</li>
          </ol>
        </Card>
      </div>
    </div>
  )
}
