import { Modal } from '@/shared/components/Modal'
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

function corto(id?: string) {
  if (!id) return '—'
  return id.slice(0, 8)
}

export function MovimientoDetailModal({ open, onClose, movimiento, detalles, catalogs, onValidar, onConfirmar, onAnular, onRevertir, onViewRelated, pending }: MovimientoDetailModalProps) {
  if (!movimiento) return null
  const nombre = (items: { id: string; nombre: string }[] | undefined, id?: string) => (id && items?.find((item) => item.id === id)?.nombre) ?? '—'
  const origen = [nombre(catalogs?.propiedades, movimiento.origenPropiedadId), nombre(catalogs?.potreros, movimiento.origenPotreroId), nombre(catalogs?.lotes, movimiento.origenLoteId)].join(' / ')
  const destino = [nombre(catalogs?.propiedades, movimiento.destinoPropiedadId), nombre(catalogs?.potreros, movimiento.destinoPotreroId), nombre(catalogs?.lotes, movimiento.destinoLoteId)].join(' / ')

  return (
    <Modal open={open} onClose={onClose} title={`Movimiento · ${movimiento.tipo.replaceAll('_', ' ')}`} wide>
      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 14 }}>
        <div><dt>Estado</dt><dd><MovimientoStatusBadge estado={movimiento.estado} /></dd></div>
        <div><dt>Fecha</dt><dd>{new Date(movimiento.fechaMovimiento).toLocaleDateString('es-BO')}</dd></div>
        <div><dt>Motivo</dt><dd>{movimiento.motivo ?? '—'}</dd></div>
      </div>
      {movimiento.observacion && <p className="table-secondary">Observación: {movimiento.observacion}</p>}
      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, margin: '12px 0' }}>
        <div><dt>Origen</dt><dd>{origen === '— / — / —' ? '—' : origen}</dd></div>
        <div><dt>Destino</dt><dd>{destino === '— / — / —' ? '—' : destino}</dd></div>
      </div>

      <h4>Animales ({detalles?.length ?? 0})</h4>
      <div className="table-wrapper" style={{ maxHeight: 260, overflowY: 'auto' }}>
        <table>
          <thead><tr><th>Código</th><th>Versión esperada</th><th>Estado</th><th>Resultado</th></tr></thead>
          <tbody>
            {detalles?.map((detalle) => (
              <tr key={detalle.id}>
                <td>{catalogs?.animales.find((animal) => animal.id === detalle.animalId)?.codigo ?? corto(detalle.animalId)}</td>
                <td className="table-secondary">{detalle.animalVersionEsperada}</td>
                <td className="table-secondary">{detalle.estadoDespues ?? '—'}</td>
                <td className="table-secondary">{detalle.estadoResultado ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, margin: '14px 0' }}>
        <div><dt>Confirmado por</dt><dd className="table-secondary">{corto(movimiento.usuarioConfirma)} · {movimiento.fechaConfirmacion ? new Date(movimiento.fechaConfirmacion).toLocaleString('es-BO') : '—'}</dd></div>
        <div><dt>Anulado por</dt><dd className="table-secondary">{corto(movimiento.usuarioAnula)}{movimiento.motivoAnulacion ? ` · ${movimiento.motivoAnulacion}` : ''}</dd></div>
        <div><dt>Revertido por</dt><dd className="table-secondary">{corto(movimiento.usuarioRevierte)} · {movimiento.fechaReversion ? new Date(movimiento.fechaReversion).toLocaleString('es-BO') : '—'}{movimiento.motivoReversion ? ` · ${movimiento.motivoReversion}` : ''}</dd></div>
      </div>

      {movimiento.movimientoReversionId && (
        <p><Button variant="ghost" onClick={() => onViewRelated(movimiento.movimientoReversionId!)}>Ver movimiento inverso</Button></p>
      )}
      {movimiento.movimientoRevertidoId && (
        <p><Button variant="ghost" onClick={() => onViewRelated(movimiento.movimientoRevertidoId!)}>Ver movimiento revertido</Button></p>
      )}

      <div className="form-actions" style={{ marginTop: 16 }}>
        {movimiento.estado === 'PENDIENTE' && (
          <>
            <Button loading={pending.validar} onClick={onValidar}>Validar</Button>
            <Button loading={pending.confirmar} disabled={!detalles} onClick={onConfirmar}>Confirmar</Button>
            <Button variant="danger" loading={pending.anular} onClick={onAnular}>Anular</Button>
          </>
        )}
        {movimiento.estado === 'CONFIRMADO' && (
          <Button variant="danger" loading={pending.revertir} onClick={onRevertir}>Revertir</Button>
        )}
        <Button variant="secondary" onClick={onClose}>Cerrar</Button>
      </div>
    </Modal>
  )
}
