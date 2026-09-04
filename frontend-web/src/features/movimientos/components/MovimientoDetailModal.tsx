import { Modal } from '@/shared/components/Modal'
import { useQueries } from '@tanstack/react-query'
import { getAnimal } from '@/features/animales/api'
import { formatDate } from '@/shared/utils/date'
import { Button } from '@/shared/components/Button'
import { MovimientoStatusBadge } from '@/features/movimientos/components/MovimientoStatusBadge'
import type { Movimiento, MovimientoDetalle } from '@/features/movimientos/api'

interface Catalog {
  propiedades: { id: string; nombre: string }[]
  potreros: { id: string; nombre: string }[]
  lotes: { id: string; nombre: string }[]
  animales: { id: string; codigo: string; nombre?: string }[]
}

interface MovimientoDetailModalProps {
  open: boolean
  onClose: () => void
  movimiento: Movimiento | null
  detalles: MovimientoDetalle[] | undefined
  catalogs: Catalog | undefined
  onValidar: () => void
  onConfirmar: () => void
  onAnular: () => void
  onRevertir: () => void
  onViewRelated: (id: string) => void
  pending: { validar?: boolean; confirmar?: boolean; anular?: boolean; revertir?: boolean }
}

const tipos = { SALIDA_VENTA: 'Venta de animales', INGRESO_COMPRA: 'Ingreso por compra', CAMBIO_POTRERO: 'Cambio de potrero', CAMBIO_LOTE: 'Cambio de lote', TRANSFERENCIA_PROPIEDAD: 'Traslado entre propiedades', CUARENTENA: 'Ingreso a cuarentena', RETORNO_CUARENTENA: 'Salida de cuarentena' }

function fechaHora(value?: string) {
  return value ? new Date(value).toLocaleString('es-BO', { timeZone: 'America/La_Paz' }) : 'Fecha no registrada'
}

export function MovimientoDetailModal({ open, onClose, movimiento, detalles, catalogs, onValidar, onConfirmar, onAnular, onRevertir, onViewRelated, pending }: MovimientoDetailModalProps) {
  const faltantes = [...new Set(detalles?.map((detalle) => detalle.animalId) ?? [])]
    .filter((id) => !catalogs?.animales.some((animal) => animal.id === id))
  const consultas = useQueries({ queries: faltantes.map((id) => ({
    queryKey: ['animal', id], queryFn: () => getAnimal(id), enabled: open, staleTime: 60_000,
  })) })
  const animales = new Map(catalogs?.animales.map((animal) => [animal.id, animal]))
  consultas.forEach((consulta) => { if (consulta.data) animales.set(consulta.data.id, consulta.data) })
  if (!movimiento) return null
  const nombre = (items: { id: string; nombre: string }[] | undefined, id?: string) => (id && items?.find((item) => item.id === id)?.nombre) ?? '—'
  const origen = [nombre(catalogs?.propiedades, movimiento.origenPropiedadId), nombre(catalogs?.potreros, movimiento.origenPotreroId), nombre(catalogs?.lotes, movimiento.origenLoteId)].join(' / ')
  const destino = [nombre(catalogs?.propiedades, movimiento.destinoPropiedadId), nombre(catalogs?.potreros, movimiento.destinoPotreroId), nombre(catalogs?.lotes, movimiento.destinoLoteId)].join(' / ')

  return (
    <Modal open={open} onClose={onClose} title={tipos[movimiento.tipo] ?? 'Detalle del movimiento'} wide>
      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 14 }}>
        <div><dt>Estado</dt><dd><MovimientoStatusBadge estado={movimiento.estado} /></dd></div>
        <div><dt>Fecha del movimiento</dt><dd>{formatDate(movimiento.fechaMovimiento)}</dd></div>
        <div><dt>Motivo</dt><dd>{movimiento.motivo ?? '—'}</dd></div>
      </div>
      {movimiento.observacion && <p className="table-secondary">Observación: {movimiento.observacion}</p>}
      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, margin: '12px 0' }}>
        <div><dt>Ubicación de origen</dt><dd>{origen === '— / — / —' ? 'No registrada en el movimiento' : origen}</dd></div>
        <div><dt>Destino</dt><dd>{movimiento.tipo === 'SALIDA_VENTA' ? 'Salida de la finca por venta' : destino === '— / — / —' ? 'No registrado en el movimiento' : destino}</dd></div>
      </div>

      <h4>Animales ({detalles?.length ?? 0})</h4>
      <div className="table-wrapper" style={{ maxHeight: 260, overflowY: 'auto' }}>
        <table>
          <thead><tr><th scope="col">Animal</th><th scope="col">Resultado del movimiento</th></tr></thead>
          <tbody>
            {detalles?.map((detalle) => (
              <tr key={detalle.id}>
                <td><strong>{animales.get(detalle.animalId)?.nombre?.trim() || animales.get(detalle.animalId)?.codigo || (consultas.some((consulta) => consulta.isLoading) ? 'Cargando nombre…' : 'Nombre no disponible')}</strong></td>
                <td>{movimiento.estado === 'REVERTIDO' ? 'Movimiento deshecho' : movimiento.estado === 'ANULADO' ? 'Movimiento cancelado' : movimiento.estado === 'PENDIENTE' ? 'Pendiente de confirmación' : detalle.estadoResultado === 'OK' ? (movimiento.tipo === 'SALIDA_VENTA' ? 'Venta registrada correctamente' : 'Movimiento realizado correctamente') : detalle.mensajeResultado || 'Resultado no disponible'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, margin: '14px 0' }}>
        {movimiento.fechaConfirmacion && <div><dt>Confirmado el</dt><dd>{fechaHora(movimiento.fechaConfirmacion)}</dd></div>}
        {movimiento.estado === 'ANULADO' && <div><dt>Cancelado el</dt><dd>{fechaHora(movimiento.fechaAnulacion)}{movimiento.motivoAnulacion ? ` · ${movimiento.motivoAnulacion}` : ''}</dd></div>}
        {movimiento.estado === 'REVERTIDO' && <div><dt>Deshecho el</dt><dd>{fechaHora(movimiento.fechaReversion)}{movimiento.motivoReversion ? ` · ${movimiento.motivoReversion}` : ''}</dd></div>}
      </div>

      {movimiento.movimientoReversionId && (
        <p><Button variant="ghost" onClick={() => onViewRelated(movimiento.movimientoReversionId!)}>Ver movimiento inverso</Button></p>
      )}
      {movimiento.movimientoRevertidoId && (
        <p><Button variant="ghost" onClick={() => onViewRelated(movimiento.movimientoRevertidoId!)}>Ver movimiento revertido</Button></p>
      )}

      {movimiento.estado === 'CONFIRMADO' && <p>Deshacer revierte los cambios de este movimiento en los animales. Cerrar solo cierra esta ventana.</p>}
      <div className="form-actions" style={{ marginTop: 16 }}>
        {movimiento.estado === 'PENDIENTE' && (
          <>
            <Button loading={pending.validar} onClick={onValidar}>Validar</Button>
            <Button loading={pending.confirmar} disabled={!detalles} onClick={onConfirmar}>Confirmar</Button>
            <Button variant="danger" loading={pending.anular} onClick={onAnular}>Anular</Button>
          </>
        )}
        {movimiento.estado === 'CONFIRMADO' && (
          <Button variant="danger" loading={pending.revertir} onClick={onRevertir}>Deshacer movimiento</Button>
        )}
        <Button variant="secondary" onClick={onClose}>Cerrar</Button>
      </div>
    </Modal>
  )
}
