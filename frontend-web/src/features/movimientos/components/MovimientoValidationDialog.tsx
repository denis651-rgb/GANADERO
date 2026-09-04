import { Link } from 'react-router'
import { Modal } from '@/shared/components/Modal'
import { Button } from '@/shared/components/Button'
import { Alert } from '@/shared/components/Alert'
import type { ValidacionMovimiento } from '@/features/movimientos/api'

const RUTA_REGISTRAR_PRUEBA_DIAGNOSTICA = '/sanidad?seccion=jornadas&tipoJornada=PRUEBA_DIAGNOSTICA'

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
          {validation.capacidad && <div style={{ marginBottom: 16 }}>
            <div className="detail-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              <div><dt>Capacidad recomendada</dt><dd>{validation.capacidad.recomendadaUa} UA</dd></div>
              <div><dt>Ocupación actual</dt><dd>{validation.capacidad.ocupacionActualUa} UA</dd></div>
              <div><dt>Ingreso</dt><dd>{validation.capacidad.ingresoUa} UA</dd></div>
              <div><dt>Ocupación proyectada</dt><dd>{validation.capacidad.ocupacionProyectadaUa} UA</dd></div>
            </div>
            {validation.capacidad.excedida && <Alert tone="warning">La ocupación proyectada supera la capacidad recomendada de {validation.capacidad.potrero}. Puedes continuar bajo tu responsabilidad; el sistema no bloqueará el movimiento.</Alert>}
          </div>}
          <div className="table-wrapper" style={{ maxHeight: 320, overflowY: 'auto' }}>
            <table>
              <thead><tr><th scope="col">Animal</th><th scope="col">Estado</th><th scope="col">Mensaje</th><th scope="col">Acción</th></tr></thead>
              <tbody>
                {validation.resultados.map((resultado) => (
                  <tr key={resultado.animalId}>
                    <td className="table-secondary">{resultado.animalId}</td>
                    <td><span className={resultado.estado === 'VALIDO' ? 'status-badge status-badge-valid' : 'status-badge status-badge-invalid'}>{resultado.estado}</span></td>
                    <td>{resultado.mensaje ?? '—'}</td>
                    <td>{resultado.error === 'MOVEMENT_CUARENTENA_SIN_PRUEBA_DIAGNOSTICA' && (
                      <Link className="text-link" to={RUTA_REGISTRAR_PRUEBA_DIAGNOSTICA}>Registrar prueba diagnóstica</Link>
                    )}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="form-actions" style={{ marginTop: 16 }}>
            <Button variant="secondary" onClick={onClose}>Cerrar</Button>
            <Button disabled={!validation.valid} onClick={onConfirm}>{validation.capacidad?.excedida ? 'Continuar de todas formas' : 'Confirmar movimiento'}</Button>
          </div>
        </>
      )}
    </Modal>
  )
}
