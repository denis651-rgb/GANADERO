import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, RefreshCw } from 'lucide-react'
import { listarEstadoOcurrenciasCalendario, reintentarOcurrenciaCalendario } from '@/features/configuracion/calendarioExternoApi'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

export function CalendarioExternoPanel() {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['calendario-externo-ocurrencias'], queryFn: listarEstadoOcurrenciasCalendario, refetchInterval: 30_000 })
  const retry = useMutation({ mutationFn: reintentarOcurrenciaCalendario, onSuccess: async () => {
    await window.ganadero?.googleCalendar?.syncNow()
    void client.invalidateQueries({ queryKey: ['calendario-externo-ocurrencias'] })
  } })
  return <Card>
    <div className="section-heading"><div><h3>Calendario externo</h3><p className="muted">Seguimiento de actividades enviadas a Google Calendar.</p></div></div>
    {(query.error || retry.error) && <Alert tone="danger">{normalizeApiError(query.error ?? retry.error).message}</Alert>}
    {query.isPending && <LoadingState message="Consultando sincronización…" />}
    {query.data?.length === 0 && <p className="muted">Todavía no existen actividades sanitarias proyectadas para sincronizar.</p>}
    {query.data && query.data.length > 0 && <div className="table-scroll"><table><thead><tr><th>Actividad</th><th>Fecha</th><th>Ubicación</th><th>Animales</th><th>Google Calendar</th><th>Acciones</th></tr></thead><tbody>
      {query.data.map(item => <tr key={item.ocurrenciaId}>
        <td><strong>{item.actividad}</strong>{item.error && <small className="field-error">{item.error}</small>}</td>
        <td>{new Date(item.fechaPrevista).toLocaleString('es-BO')}</td>
        <td>{[item.propiedad, item.potrero, item.lote].filter(Boolean).join(' · ') || 'Sin ubicación'}</td>
        <td>{item.animales}</td>
        <td><span className={`status-badge ${item.estadoExterno === 'SINCRONIZADO' ? 'status-activo' : 'status-inactivo'}`}>{item.estadoExterno ?? item.estadoCola ?? 'NO SINCRONIZADO'}</span></td>
        <td><div className="actions-cell">
          {item.enlaceExterno && <a className="button button-ghost jornada-icon-action" href={item.enlaceExterno} target="_blank" rel="noreferrer" title="Ver calendario" aria-label="Ver calendario"><CalendarDays size={17} aria-hidden="true" /></a>}
          {(item.estadoCola === 'ERROR_DEFINITIVO' || !item.estadoExterno) && <Button type="button" variant="secondary" className="jornada-icon-action" title="Reintentar sincronización" aria-label="Reintentar sincronización" loading={retry.isPending && retry.variables === item.ocurrenciaId} onClick={() => retry.mutate(item.ocurrenciaId)}><RefreshCw size={17} aria-hidden="true" /></Button>}
        </div></td>
      </tr>)}
    </tbody></table></div>}
  </Card>
}
