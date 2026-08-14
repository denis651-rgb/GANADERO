import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { RefreshCw } from 'lucide-react'
import {
  aplicarTratamiento,
  ESTADO_APLICACION_LABELS,
  listAplicaciones,
  regenerarTratamiento,
  type AplicacionTratamiento,
  type Tratamiento,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface AplicacionesModalProps {
  tratamiento: Tratamiento
  catalogs: SanidadCatalogs
  onClose: () => void
}

const ACTIVAS: Array<AplicacionTratamiento['estado']> = ['PENDIENTE', 'ATRASADA']

export function AplicacionesModal({ tratamiento, catalogs, onClose }: AplicacionesModalProps) {
  const client = useQueryClient()
  const [aplicando, setAplicando] = useState<AplicacionTratamiento | null>(null)
  const query = useQuery({
    queryKey: ['sanidad-aplicaciones', tratamiento.id],
    queryFn: () => listAplicaciones(tratamiento.id),
  })
  const aplicar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return aplicarTratamiento(tratamiento.id, aplicando!.id, {
        dosisAplicada: Number(data.get('dosisAplicada')) || aplicando!.dosisProgramada,
        observaciones: String(data.get('observaciones') || '') || undefined,
        version: aplicando!.version,
      })
    },
    onSuccess: () => { setAplicando(null); void client.invalidateQueries({ queryKey: ['sanidad-aplicaciones'] }) },
  })
  const regenerar = useMutation({
    mutationFn: () => regenerarTratamiento(tratamiento.id),
    onSuccess: () => { void client.invalidateQueries({ queryKey: ['sanidad-aplicaciones'] }); void client.invalidateQueries({ queryKey: ['sanidad-tratamientos'] }) },
  })

  const proximas = (query.data ?? []).filter((aplicacion) => ACTIVAS.includes(aplicacion.estado)).sort((a, b) => new Date(a.fechaProgramada).getTime() - new Date(b.fechaProgramada).getTime())

  return <Modal open title="Aplicaciones del tratamiento" onClose={onClose} wide description="Cronograma de aplicaciones del tratamiento.">
    <div className="page-stack">
      <Alert tone="info">Animal: {catalogs.animalLabel(tratamiento.animalId)} · {new Date(tratamiento.fechaInicio).toLocaleDateString('es-BO')} → {new Date(tratamiento.fechaFinEstimada).toLocaleDateString('es-BO')}</Alert>
      {query.isPending && <LoadingState message="Cargando aplicaciones…" />}
      {query.error && <Alert tone="danger">{normalizeApiError(query.error).message}</Alert>}
      {query.data?.length === 0 && <p className="muted">Este tratamiento no tiene aplicaciones programadas.</p>}
      {proximas.length > 0 && <>
        <h3>Próximas por aplicar</h3>
        <div className="table-wrapper"><table><caption className="visually-hidden">Aplicaciones pendientes</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Dosis</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{proximas.map((aplicacion) => <tr key={aplicacion.id}>
          <td>{new Date(aplicacion.fechaProgramada).toLocaleDateString('es-BO')}</td>
          <td>{aplicacion.dosisProgramada}{aplicacion.estado === 'ATRASADA' ? ' (atrasada)' : ''}</td>
          <td><span className={`status-badge ${aplicacion.estado === 'ATRASADA' ? 'status-badge-danger' : 'status-badge-pending'}`}>{ESTADO_APLICACION_LABELS[aplicacion.estado]}</span></td>
          <td><Button variant="secondary" onClick={() => setAplicando(aplicacion)}>Aplicar</Button></td>
        </tr>)}</tbody></table></div>
      </>}
      {query.data && query.data.length > 0 && <>
        <h3>Historial</h3>
        <div className="table-wrapper"><table><caption className="visually-hidden">Historial de aplicaciones</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Dosis</th><th scope="col">Aplicado</th><th scope="col">Estado</th></tr></thead><tbody>{query.data.filter((aplicacion) => !ACTIVAS.includes(aplicacion.estado)).sort((a, b) => new Date(b.fechaProgramada).getTime() - new Date(a.fechaProgramada).getTime()).map((aplicacion) => <tr key={aplicacion.id}>
          <td>{new Date(aplicacion.fechaProgramada).toLocaleDateString('es-BO')}</td>
          <td>{aplicacion.dosisAplicada ?? aplicacion.dosisProgramada}</td>
          <td className="table-secondary">{aplicacion.aplicadoPor ? catalogs.userLabel(aplicacion.aplicadoPor) : '—'}</td>
          <td><span className="status-badge">{ESTADO_APLICACION_LABELS[aplicacion.estado]}</span></td>
        </tr>)}</tbody></table></div>
      </>}
      <div className="form-actions">
        <Button variant="secondary" onClick={() => regenerar.mutate()} loading={regenerar.isPending}><RefreshCw size={16} aria-hidden="true" />Regenerar cronograma</Button>
      </div>
      {regenerar.error && <Alert tone="danger">{normalizeApiError(regenerar.error).message}</Alert>}
    </div>

    <Modal open={Boolean(aplicando)} title="Registrar aplicación" onClose={() => setAplicando(null)} description="Registra la dosis efectivamente aplicada.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); aplicar.mutate(event.currentTarget) }}>
        {aplicando && <>
          <Field label="Dosis aplicada" required><input name="dosisAplicada" type="number" inputMode="decimal" min="0.001" step="0.001" required defaultValue={aplicando.dosisProgramada} /></Field>
          <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        </>}
        <div className="form-actions"><Button type="submit" loading={aplicar.isPending}>Guardar aplicación</Button></div>
      </form>
      {aplicar.error && <Alert tone="danger">{normalizeApiError(aplicar.error).message}</Alert>}
    </Modal>
  </Modal>
}
