import { Modal } from '@/shared/components/Modal'
import { Button } from '@/shared/components/Button'
import type { ValidacionMovimiento } from '@/features/movimientos/api'

interface MovimientoValidationDialogProps {
  open: boolean
  onClose: () => void
  validation: ValidacionMovimiento | null
  loading?: boolean
  onConfirm: () => void
}

export function MovimientoValidationDialog({ open, onClose, validation, loading, onConfirm }: MovimientoValidationDialogProps) {
  return (
    <Modal open={open} onClose={onClose} title="Validación del movimiento" wide>
      {loading && <p className="table-secondary">Validando animales…</p>}
      {!loading && validation && (
        <>
          <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16 }}>
            <div><dt>Total</dt><dd>{validation.total}</dd></div>
            <div><dt>Válidos</dt><dd>{validation.validos}</dd></div>
            <div><dt>Inválidos</dt><dd>{validation.invalidos}</dd></div>
            <div><dt>Resultado</dt><dd><span className={validation.valid ? 'status-badge status-badge-valid' : 'status-badge status-badge-invalid'}>{validation.valid ? 'VALIDO' : 'RECHAZADO'}</span></dd></div>
          </div>
          <div className="table-wrapper" style={{ maxHeight: 320, overflowY: 'auto' }}>
            <table>
              <thead><tr><th>Animal</th><th>Estado</th><th>Código</th><th>Mensaje</th></tr></thead>
              <tbody>
                {validation.resultados.map((resultado) => (
                  <tr key={resultado.animalId}>
                    <td>{resultado.animalId}</td>
                    <td><span className={resultado.estado === 'VALIDO' ? 'status-badge status-badge-valid' : 'status-badge status-badge-invalid'}>{resultado.estado}</span></td>
                    <td className="table-secondary">{resultado.codigo ?? '—'}</td>
                    <td className="table-secondary">{resultado.mensaje ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="form-actions" style={{ marginTop: 16 }}>
            <Button variant="secondary" onClick={onClose}>Cerrar</Button>
            <Button disabled={!validation.valid} onClick={onConfirm}>Confirmar movimiento</Button>
          </div>
        </>
      )}
    </Modal>
  )
}
