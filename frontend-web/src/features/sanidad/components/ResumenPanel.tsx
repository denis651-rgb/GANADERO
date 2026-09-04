import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Activity, AlertCircle, AlertTriangle, CalendarCheck2, ClipboardList, Syringe } from 'lucide-react'
import { attendAlert, listAlerts } from '@/features/alertas/api'
import { buildResumenAlertItems, type ResumenAlertItem } from '@/features/sanidad/alertasResumen'
import type { CasoClinico, JornadaSanitaria, PlanSanitario, Tratamiento } from '@/features/sanidad/api'
import { ESTADO_JORNADA_LABELS, SEVERIDAD_BADGE_CLASS, SEVERIDAD_LABELS } from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import type { Seccion } from '@/features/sanidad/pages/SanidadPage'
import { Alert } from '@/shared/components/Alert'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

interface ResumenPanelProps {
  planes: PlanSanitario[]
  jornadas: JornadaSanitaria[]
  casos: CasoClinico[]
  tratamientos: Tratamiento[]
  catalogs: SanidadCatalogs
  onIrA: (seccion: Seccion) => void
}

const ATTENTION_ICONS: Record<ResumenAlertItem['severidad'], typeof AlertCircle> = {
  danger: AlertCircle,
  warning: AlertTriangle,
}

function AtencionSanitariaCard({ onIrA }: { onIrA: (seccion: Seccion) => void }) {
  const client = useQueryClient()
  const [attendTarget, setAttendTarget] = useState<ResumenAlertItem | null>(null)

  const alertasQuery = useQuery({
    queryKey: ['sanidad-alertas-resumen'],
    queryFn: async () => {
      const [vacunaVencida, casoCritico, vacunaProxima, tratamientoAtrasado, revisionIngreso] = await Promise.all([
        listAlerts({ tipo: 'VACUNA_VENCIDA' }),
        listAlerts({ tipo: 'CASO_CLINICO_CRITICO' }),
        listAlerts({ tipo: 'VACUNA_PROXIMA' }),
        listAlerts({ tipo: 'TRATAMIENTO_ATRASADO' }),
        listAlerts({ tipo: 'REVISION_SANITARIA_INGRESO' }),
      ])
      return buildResumenAlertItems([
        { alertas: vacunaVencida, severidad: 'danger', seccion: 'jornadas', seccionLabel: 'Jornadas' },
        { alertas: casoCritico, severidad: 'danger', seccion: 'casos', seccionLabel: 'Casos clínicos' },
        { alertas: vacunaProxima, severidad: 'warning', seccion: 'jornadas', seccionLabel: 'Jornadas' },
        { alertas: tratamientoAtrasado, severidad: 'warning', seccion: 'tratamientos', seccionLabel: 'Tratamientos' },
        // Animal comprado sin historial verificable (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md,
        // sección 3.3b): es trazabilidad/compliance de la compra, no un "vencido" real — el
        // mensaje del backend ya distingue dentro/fuera de ventana, no hace falta otro bucket.
        { alertas: revisionIngreso, severidad: 'warning', seccion: 'jornadas', seccionLabel: 'Jornadas' },
      ])
    },
  })

  const attend = useMutation({
    mutationFn: (id: string) => attendAlert(id),
    onSuccess: () => { setAttendTarget(null); void client.invalidateQueries({ queryKey: ['sanidad-alertas-resumen'] }) },
  })

  const items = alertasQuery.data ?? []
  const hasAttention = items.length > 0

  return <Card className="attention-card">
    <div className="section-heading">
      <div><span className="eyebrow">Prioridad diaria</span><h2>Atención requerida</h2></div>
      {!alertasQuery.isPending && !hasAttention && <span className="status-badge status-activo">Todo al día</span>}
    </div>
    {alertasQuery.isPending && <LoadingState message="Cargando alertas sanitarias…" />}
    {alertasQuery.error && <Alert tone="danger">{normalizeApiError(alertasQuery.error).message}</Alert>}
    {!alertasQuery.isPending && !alertasQuery.error && (hasAttention
      ? <ul className="attention-list">{items.map((item) => {
          const Icon = ATTENTION_ICONS[item.severidad]
          return <li key={item.id} className={`attention-${item.severidad}`}>
            <span className="attention-icon"><Icon size={18} aria-hidden="true" /></span>
            <div><strong>{item.mensaje}</strong><span>{item.detalle}</span></div>
            <div className="inline-actions">
              <button type="button" className="text-link" onClick={() => onIrA(item.seccion)}>Ir a {item.seccionLabel}</button>
              <button type="button" className="text-link" onClick={() => setAttendTarget(item)}>Marcar atendida</button>
            </div>
          </li>
        })}</ul>
      : <p className="attention-empty">No hay alertas sanitarias pendientes. El hato está al día.</p>)}

    <ConfirmDialog
      open={Boolean(attendTarget)}
      title="Marcar alerta como atendida"
      confirmLabel="Confirmar"
      variant="warning"
      loading={attend.isPending}
      error={attend.error}
      onClose={() => setAttendTarget(null)}
      onConfirm={() => { if (attendTarget && !attend.isPending) attend.mutate(attendTarget.id) }}
    >
      {attendTarget && <p className="muted">«{attendTarget.mensaje}» dejará de aparecer en atención requerida. Podés seguir el seguimiento completo desde {attendTarget.seccionLabel}.</p>}
    </ConfirmDialog>
  </Card>
}

export function ResumenPanel({ planes, jornadas, casos, tratamientos, catalogs, onIrA }: ResumenPanelProps) {
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
    <AtencionSanitariaCard onIrA={onIrA} />
    <div className="metric-grid">{metricas.map(({ label, valor, icon: Icon }) => <Card key={label} className="metric-card"><span className="metric-icon" aria-hidden="true"><Icon size={22} /></span><div><span>{label}</span><strong>{valor}</strong></div></Card>)}</div>
    <div className="two-column-grid">
      <Card>
        <h3>Jornadas recientes</h3>
        {recientes.length === 0 && <p className="muted">Todavía no hay jornadas registradas.</p>}
        {recientes.length > 0 && <div className="table-wrapper"><table><caption className="visually-hidden">Jornadas sanitarias más recientes</caption><thead><tr><th scope="col">Propiedad</th><th scope="col">Estado</th><th scope="col">Fecha</th></tr></thead><tbody>{recientes.map((jornada) => <tr key={jornada.id}>
          <td>{catalogs.properties.find((item) => item.id === jornada.propiedadId)?.nombre ?? 'Propiedad'}</td>
          <td><span className="status-badge">{ESTADO_JORNADA_LABELS[jornada.estado]}</span></td>
          <td>{new Date(jornada.fechaInicio).toLocaleDateString('es-BO')}</td>
        </tr>)}</tbody></table></div>}
      </Card>
      <Card>
        <h3>Casos por atender</h3>
        {casosRecientes.length === 0 && <p className="muted">No hay casos clínicos abiertos.</p>}
        {casosRecientes.length > 0 && <div className="table-wrapper"><table><caption className="visually-hidden">Casos clínicos por atender</caption><thead><tr><th scope="col">Animal</th><th scope="col">Severidad</th><th scope="col">Fecha</th></tr></thead><tbody>{casosRecientes.map((caso) => <tr key={caso.id}>
          <td>{catalogs.animalLabel(caso.animalId)}</td>
          <td><span className={`status-badge ${SEVERIDAD_BADGE_CLASS[caso.severidad]}`}>{SEVERIDAD_LABELS[caso.severidad]}</span></td>
          <td>{new Date(caso.fechaInicio).toLocaleDateString('es-BO')}</td>
        </tr>)}</tbody></table></div>}
      </Card>
    </div>
  </div>
}
