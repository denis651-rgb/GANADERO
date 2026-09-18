import { useQuery } from '@tanstack/react-query'
import {
  ESTADO_APLICACION_SANITARIA_LABELS,
  LUGAR_APLICACION_LABELS,
  listAplicacionesJornada,
  TIPO_ACTIVIDAD_LABELS,
  VIA_ADMINISTRACION_LABELS,
  type AplicacionSanitaria,
  type JornadaSanitaria,
  type LugarAplicacion,
  type ViaAdministracion,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface JornadaDetalleModalProps {
  jornada: JornadaSanitaria
  catalogs: SanidadCatalogs
  onClose: () => void
}

function fechaTexto(valor?: string) {
  return valor ? new Date(`${valor}T00:00:00`).toLocaleDateString('es-BO') : undefined
}

function viaTexto(ap: AplicacionSanitaria) {
  if (!ap.viaAdministracion) return null
  return VIA_ADMINISTRACION_LABELS[ap.viaAdministracion as ViaAdministracion] ?? ap.viaAdministracion
}

function dosisTexto(ap: AplicacionSanitaria) {
  if (ap.dosisAplicada == null) return '—'
  return `${ap.dosisAplicada} ${ap.unidadDosis ?? ''}`.trim()
}

function retirosTexto(ap: AplicacionSanitaria) {
  const carne = fechaTexto(ap.retiroCarneHasta)
  const leche = fechaTexto(ap.retiroLecheHasta)
  if (!carne && !leche) return '—'
  return [carne ? `Carne: ${carne}` : null, leche ? `Leche: ${leche}` : null].filter(Boolean).join(' · ')
}

export function JornadaDetalleModal({ jornada, catalogs, onClose }: JornadaDetalleModalProps) {
  const aplicaciones = useQuery({
    queryKey: ['sanidad-jornada-aplicaciones', jornada.id],
    queryFn: () => listAplicacionesJornada(jornada.id),
  })

  const propiedad = catalogs.properties.find((item) => item.id === jornada.propiedadId)?.nombre
  const potrero = jornada.potreroId ? catalogs.paddocks.find((item) => item.id === jornada.potreroId)?.nombre : undefined
  const lote = jornada.loteGanaderoId ? catalogs.lots.find((item) => item.id === jornada.loteGanaderoId)?.nombre : undefined

  return <Modal open title={`Detalle de la jornada · ${TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}`} onClose={onClose} wide description="Registro de lo que se aplicó. Esta vista es de solo lectura.">
    <div className="page-stack">
      <div className="eligibility-criteria" aria-label="Datos de la jornada">
        <strong>Datos de la jornada</strong>
        <span>Fecha: <b>{(fechaTexto(jornada.fechaInicio) ?? jornada.fechaInicio)}</b></span>
        <span>Propiedad: <b>{propiedad ?? '—'}</b></span>
        {potrero && <span>Potrero: <b>{potrero}</b></span>}
        {lote && <span>Lote: <b>{lote}</b></span>}
      </div>
      {jornada.observaciones && <p className="muted">{jornada.observaciones}</p>}

      {aplicaciones.isPending && <LoadingState message="Cargando aplicaciones…" />}
      {aplicaciones.error && <Alert tone="danger">{normalizeApiError(aplicaciones.error).message}</Alert>}

      {aplicaciones.data && <>
        <p className="muted">{aplicaciones.data.length} aplicación(es) registradas.</p>
        {aplicaciones.data.length === 0 && <EmptyState title="Sin aplicaciones" description="Esta jornada no tiene aplicaciones registradas." />}
        {aplicaciones.data.length > 0 && <div className="table-wrapper"><table>
          <caption className="visually-hidden">Aplicaciones de la jornada</caption>
          <thead><tr>
            <th scope="col">Animal</th>
            <th scope="col">Producto</th>
            <th scope="col">Dosis aplicada</th>
            <th scope="col">Vía / lugar</th>
            <th scope="col">Retiros</th>
            <th scope="col">Resultado</th>
            <th scope="col">Estado</th>
          </tr></thead>
          <tbody>{aplicaciones.data.map((ap) => <tr key={ap.id}>
            <td><strong>{catalogs.animalLabel(ap.animalId)}</strong></td>
            <td>{ap.productoAplicadoTexto ?? '—'}{ap.motivoCambioProducto && <span className="table-secondary">{ap.motivoCambioProducto}</span>}</td>
            <td>{dosisTexto(ap)}{ap.motivoAjusteDosis && <span className="table-secondary">{ap.motivoAjusteDosis}</span>}</td>
            <td>{[viaTexto(ap), ap.lugarAplicacion ? LUGAR_APLICACION_LABELS[ap.lugarAplicacion as LugarAplicacion] : null].filter(Boolean).join(' · ') || '—'}</td>
            <td>{retirosTexto(ap)}</td>
            <td>{ap.resultado ?? '—'}{ap.observaciones && <span className="table-secondary">{ap.observaciones}</span>}</td>
            <td><span className="status-badge">{ESTADO_APLICACION_SANITARIA_LABELS[ap.estado]}</span></td>
          </tr>)}</tbody>
        </table></div>}
      </>}
    </div>
  </Modal>
}
