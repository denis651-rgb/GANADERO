import { Activity, HeartPulse, Baby, Stethoscope, XCircle, Milk } from 'lucide-react'
import {
  estadoServicioBadge,
  ESTADO_SERVICIO_LABELS,
  TIPO_SERVICIO_LABELS,
  type Aborto,
  type CeloResponse,
  type Destete,
  type DiagnosticoGestacionResponse,
  type PageResponse,
  type Parto,
  type ServicioResponse,
} from '@/features/reproduccion/api'
import { Card } from '@/shared/components/Card'

interface ResumenPanelProps {
  celos: PageResponse<CeloResponse>
  servicios: PageResponse<ServicioResponse>
  diagnosticos: PageResponse<DiagnosticoGestacionResponse>
  partos: PageResponse<Parto>
  abortos: PageResponse<Aborto>
  destetes: PageResponse<Destete>
}

export function ResumenPanel({ celos, servicios, diagnosticos, partos, abortos, destetes }: ResumenPanelProps) {
  const proximos = servicios.content
    .filter((item) => item.estado === 'PENDIENTE_DIAGNOSTICO' && item.fechaDiagnosticoRecomendada)
    .sort((a, b) => a.fechaDiagnosticoRecomendada!.localeCompare(b.fechaDiagnosticoRecomendada!))
    .slice(0, 4)

  return <div className="page-stack">
    <div className="metric-grid">
      <div className="metric-card"><div className="metric-icon"><HeartPulse size={20} aria-hidden="true" /></div><div><span>Servicios</span><strong>{servicios.totalElements}</strong></div></div>
      <div className="metric-card"><div className="metric-icon"><Activity size={20} aria-hidden="true" /></div><div><span>Celos</span><strong>{celos.totalElements}</strong></div></div>
      <div className="metric-card"><div className="metric-icon"><Stethoscope size={20} aria-hidden="true" /></div><div><span>Diagnósticos</span><strong>{diagnosticos.totalElements}</strong></div></div>
      <div className="metric-card"><div className="metric-icon"><Baby size={20} aria-hidden="true" /></div><div><span>Partos</span><strong>{partos.totalElements}</strong></div></div>
      <div className="metric-card"><div className="metric-icon"><XCircle size={20} aria-hidden="true" /></div><div><span>Abortos</span><strong>{abortos.totalElements}</strong></div></div>
      <div className="metric-card"><div className="metric-icon"><Milk size={20} aria-hidden="true" /></div><div><span>Destetes</span><strong>{destetes.totalElements}</strong></div></div>
    </div>
    <Card>
      <h3>Próximos diagnósticos recomendados</h3>
      {proximos.length === 0 && <p className="muted">No hay servicios pendientes de diagnóstico.</p>}
      {proximos.length > 0 && <ul className="simple-list">
        {proximos.map((servicio) => <li key={servicio.id} className="simple-list-item">
          <div className="plan-summary"><strong>{servicio.codigoAnimal}</strong>{servicio.nombreAnimal ? ` · ${servicio.nombreAnimal}` : ''}<span className="muted">{TIPO_SERVICIO_LABELS[servicio.tipoServicio]} · Intento #{servicio.numeroIntento}</span></div>
          <div className="inline-actions"><span className="muted">Recomendado: {new Date(servicio.fechaDiagnosticoRecomendada!).toLocaleDateString('es-BO')}</span><span className={`status-badge status-badge-${estadoServicioBadge(servicio.estado)}`}>{ESTADO_SERVICIO_LABELS[servicio.estado]}</span></div>
        </li>)}
      </ul>}
    </Card>
  </div>
}
