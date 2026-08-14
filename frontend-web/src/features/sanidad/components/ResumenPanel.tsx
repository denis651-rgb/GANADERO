import { Activity, CalendarCheck2, ClipboardList, Syringe } from 'lucide-react'
import type { CasoClinico, JornadaSanitaria, PlanSanitario, Tratamiento } from '@/features/sanidad/api'
import { ESTADO_JORNADA_LABELS, SEVERIDAD_LABELS } from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Card } from '@/shared/components/Card'

interface ResumenPanelProps {
  planes: PlanSanitario[]
  jornadas: JornadaSanitaria[]
  casos: CasoClinico[]
  tratamientos: Tratamiento[]
  catalogs: SanidadCatalogs
}

export function ResumenPanel({ planes, jornadas, casos, tratamientos, catalogs }: ResumenPanelProps) {
  const planesActivos = planes.filter((plan) => plan.estado === 'ACTIVO').length
  const casosAbiertos = casos.filter((caso) => !['CERRADO', 'ANULADO'].includes(caso.estado)).length
  const tratamientosActivos = tratamientos.filter((tratamiento) => tratamiento.estado === 'ACTIVO').length
  const ahora = new Date()
  const jornadasDelMes = jornadas.filter((jornada) => {
    const fecha = new Date(jornada.fechaInicio)
    return fecha.getMonth() === ahora.getMonth() && fecha.getFullYear() === ahora.getFullYear() && jornada.estado !== 'ANULADA'
  }).length
  const recientes = [...jornadas].sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime()).slice(0, 5)
  const casosRecientes = casos.filter((caso) => caso.estado !== 'CERRADO').slice(0, 5)

  const metricas: Array<{ label: string; valor: number; icon: typeof Syringe }> = [
    { label: 'Planes activos', valor: planesActivos, icon: ClipboardList },
    { label: 'Casos abiertos', valor: casosAbiertos, icon: Activity },
    { label: 'Tratamientos activos', valor: tratamientosActivos, icon: Syringe },
    { label: 'Jornadas del mes', valor: jornadasDelMes, icon: CalendarCheck2 },
  ]

  return <div className="page-stack">
    <div className="metric-grid">{metricas.map(({ label, valor, icon: Icon }) => <Card key={label} className="metric-card"><span className="metric-icon" aria-hidden="true"><Icon size={22} /></span><strong>{valor}</strong><span>{label}</span></Card>)}</div>
    <div className="two-column-grid">
      <Card>
        <h3>Jornadas recientes</h3>
        {recientes.length === 0 && <p className="muted">Todavía no hay jornadas registradas.</p>}
        {recientes.length > 0 && <ul className="detail-list">{recientes.map((jornada) => <li key={jornada.id}><span><strong>{catalogs.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? 'Propiedad'}</strong><span className="table-secondary">{ESTADO_JORNADA_LABELS[jornada.estado]}</span></span><time dateTime={jornada.fechaInicio}>{new Date(jornada.fechaInicio).toLocaleDateString('es-BO')}</time></li>)}</ul>}
      </Card>
      <Card>
        <h3>Casos por atender</h3>
        {casosRecientes.length === 0 && <p className="muted">No hay casos clínicos abiertos.</p>}
        {casosRecientes.length > 0 && <ul className="detail-list">{casosRecientes.map((caso) => <li key={caso.id}><span><strong>{catalogs.animalLabel(caso.animalId)}</strong><span className={`status-badge status-badge-pending`}>{SEVERIDAD_LABELS[caso.severidad]}</span></span><time dateTime={caso.fechaInicio}>{new Date(caso.fechaInicio).toLocaleDateString('es-BO')}</time></li>)}</ul>}
      </Card>
    </div>
  </div>
}
