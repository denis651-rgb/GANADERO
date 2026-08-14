import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  confirmarJornada,
  TIPO_ACTIVIDAD_LABELS,
  type ConfirmacionJornadaResult,
  type JornadaSanitaria,
  type PlanSanitarioItem,
} from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface JornadaConfirmarModalProps {
  jornada: JornadaSanitaria
  animalesSeleccionados: number
  planItem: PlanSanitarioItem
  fechaAplicacion: string
  onClose: () => void
  onConfirmado: (resultado: ConfirmacionJornadaResult) => void
}

export function JornadaConfirmarModal({ jornada, animalesSeleccionados, planItem, fechaAplicacion, onClose, onConfirmado }: JornadaConfirmarModalProps) {
  const [operationId] = useState(() => crypto.randomUUID())
  const confirmar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return confirmarJornada(jornada.id, {
        operationId,
        version: jornada.version,
        planItemId: planItem.id,
        dosis: Number(data.get('dosis')) || undefined,
        unidadDosis: String(data.get('unidadDosis') || '') || undefined,
        viaAdministracion: String(data.get('viaAdministracion') || '') || undefined,
        fechaAplicacion,
        resultado: String(data.get('resultado') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      })
    },
    onSuccess: (resultado) => onConfirmado(resultado),
  })

  const actividad = `${TIPO_ACTIVIDAD_LABELS[planItem.tipoActividad]}${planItem.productoRecomendadoTexto ? ` · ${planItem.productoRecomendadoTexto}` : ''}`

  return <Modal open title={`Confirmar jornada · ${TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}`} onClose={onClose} wide description="Revisa los datos de la aplicación antes de confirmarla.">
    <div className="page-stack">
      <Alert tone="success">Los {animalesSeleccionados} animal(es) fueron validados para esta actividad y fecha.</Alert>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); confirmar.mutate(event.currentTarget) }}>
        <Field label="Actividad del plan"><input value={actividad} readOnly /></Field>
        <Field label="Fecha de aplicación"><input type="date" value={fechaAplicacion} readOnly /></Field>
        <Field label="Dosis"><input name="dosis" type="number" inputMode="decimal" min="0.001" step="0.001" defaultValue={planItem.dosis} /></Field>
        <Field label="Unidad de dosis"><input name="unidadDosis" maxLength={30} placeholder="mL, cc…" autoComplete="off" defaultValue={planItem.unidadDosis} /></Field>
        <Field label="Vía de administración"><input name="viaAdministracion" maxLength={60} placeholder="IM, SC…" autoComplete="off" defaultValue={planItem.viaAdministracion} /></Field>
        <Field label="Resultado"><input name="resultado" maxLength={60} autoComplete="off" /></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={confirmar.isPending}>Confirmar jornada</Button></div>
      </form>
      {confirmar.error && <Alert tone="danger">{normalizeApiError(confirmar.error).message}</Alert>}
    </div>
  </Modal>
}
