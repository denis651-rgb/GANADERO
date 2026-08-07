import type { LucideIcon } from 'lucide-react'
import { CheckCircle2, CircleDashed } from 'lucide-react'
import { MODULE_STATUS_LABEL, type ModuleStatus } from '@/app/modules'
import { Card } from '@/shared/components/Card'
import { PageHeader } from '@/shared/components/PageHeader'

interface ModuleLandingPageProps {
  title: string
  description: string
  phase: number
  icon: LucideIcon
  capabilities: string[]
  endpoints?: string[]
  status?: ModuleStatus
}

export function ModuleLandingPage({ title, description, phase, icon: Icon, capabilities, endpoints = [], status = 'EN_DESARROLLO' }: ModuleLandingPageProps) {
  return (
    <div className="page-stack">
      <PageHeader eyebrow={`Fase ${phase}`} title={title} description={description} />
      <div className="module-hero card">
        <span className="module-icon"><Icon size={30} /></span>
        <div>
          <h3>{status === 'LISTO' ? 'Módulo listo' : status === 'PROXIMAMENTE' ? 'Próximamente' : 'En desarrollo'}</h3>
          <span className={`module-status module-status-${status.toLowerCase()}`}>{MODULE_STATUS_LABEL[status]}</span>
          <p>Este módulo aún no está operativo. La ruta, navegación y permisos ya están creados; los flujos se incorporarán en una próxima fase.</p>
        </div>
      </div>
      <div className="two-column-grid">
        <Card>
          <h3>Capacidades previstas</h3>
          <ul className="check-list">
            {capabilities.map((item) => <li key={item}><CheckCircle2 size={17} />{item}</li>)}
          </ul>
        </Card>
        <Card>
          <h3>Contratos de API</h3>
          {endpoints.length ? (
            <ul className="endpoint-list">
              {endpoints.map((endpoint) => <li key={endpoint}><code>{endpoint}</code></li>)}
            </ul>
          ) : (
            <p className="muted"><CircleDashed size={16} /> Los endpoints se incorporarán en la fase correspondiente.</p>
          )}
        </Card>
      </div>
    </div>
  )
}
