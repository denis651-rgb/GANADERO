import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  confirmarJornada,
  LUGAR_APLICACION_LABELS,
  TIPO_ACTIVIDAD_LABELS,
  UNIDAD_DOSIS_LABELS,
  VIA_ADMINISTRACION_LABELS,
  type ConfirmacionJornadaResult,
  type JornadaSanitaria,
  type LugarAplicacion,
  type PlanSanitarioItem,
  type ViaAdministracion,
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
  const [viaAdministracion, setViaAdministracion] = useState<ViaAdministracion | ''>(planItem.viaAdministracionCodigo ?? '')
  const [lugarAplicacion, setLugarAplicacion] = useState<LugarAplicacion | ''>(planItem.lugarAplicacion ?? '')
  const confirmar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return confirmarJornada(jornada.id, {
        operationId,
        version: jornada.version,
        planItemId: planItem.id,
        dosisAplicada: Number(data.get('dosisAplicada')) || undefined,
        motivoAjusteDosis: String(data.get('motivoAjusteDosis') || '') || undefined,
        unidadDosis: planItem.dosisUnidad,
        productoAplicadoTexto: String(data.get('productoAplicadoTexto') || '') || undefined,
        motivoCambioProducto: String(data.get('motivoCambioProducto') || '') || undefined,
        viaAdministracion: viaAdministracion || undefined,
        lugarAplicacion: lugarAplicacion || undefined,
        fechaAplicacion,
        resultado: String(data.get('resultado') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        retiroCarneDias: Number(data.get('retiroCarneDias')) || undefined,
        retiroLecheDias: Number(data.get('retiroLecheDias')) || undefined,
      })
    },
    onSuccess: (resultado) => onConfirmado(resultado),
  })

  const dosisRecomendadaTexto = planItem.dosisTipoCalculo === 'NO_APLICA'
    ? 'No aplica'
    : planItem.dosisTipoCalculo === 'POR_PESO'
      ? 'Se calcula por el peso vigente de cada animal al confirmar'
      : `${planItem.dosisCantidad ?? '—'} ${planItem.dosisUnidad ? UNIDAD_DOSIS_LABELS[planItem.dosisUnidad] : ''}`.trim()

  return <Modal open title={`Confirmar jornada · ${TIPO_ACTIVIDAD_LABELS[jornada.tipoJornada]}`} onClose={onClose} wide description="Revisa los datos de la aplicación antes de confirmarla.">
    <div className="page-stack">
      <Alert tone="success">Los {animalesSeleccionados} animal(es) fueron validados para esta actividad y fecha.</Alert>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); confirmar.mutate(event.currentTarget) }}>
        <Field label="Actividad del plan"><input value={planItem.nombre} readOnly /></Field>
        <Field label="Fecha de aplicación"><input type="date" value={fechaAplicacion} readOnly /></Field>
        <Field label="Medicamento recomendado"><input value={planItem.productoRecomendadoTexto ?? 'Sin especificar'} readOnly /></Field>
        <Field label="Dosis recomendada"><input value={dosisRecomendadaTexto} readOnly /></Field>
        {planItem.instruccionesVeterinario && <div className="form-full"><Field label="Instrucciones del veterinario"><textarea value={planItem.instruccionesVeterinario} readOnly rows={2} /></Field></div>}

        <Field label="Dosis aplicada (si difiere de la calculada)" hint="Déjalo vacío para usar la dosis calculada por animal."><input name="dosisAplicada" type="number" inputMode="decimal" min="0" step="0.001" /></Field>
        <Field label="Motivo del ajuste de dosis"><input name="motivoAjusteDosis" maxLength={300} autoComplete="off" /></Field>

        <Field label="Producto realmente aplicado (si difiere del recomendado)"><input name="productoAplicadoTexto" maxLength={300} autoComplete="off" placeholder={planItem.productoRecomendadoTexto} /></Field>
        <Field label="Motivo del cambio de producto"><input name="motivoCambioProducto" maxLength={300} autoComplete="off" /></Field>

        <Field label="Vía de administración">
          <select value={viaAdministracion} onChange={(event) => setViaAdministracion(event.target.value as ViaAdministracion | '')}>
            <option value="">Selecciona…</option>
            {(Object.keys(VIA_ADMINISTRACION_LABELS) as ViaAdministracion[]).map((via) => <option key={via} value={via}>{VIA_ADMINISTRACION_LABELS[via]}</option>)}
          </select>
        </Field>
        <Field label="Lugar de aplicación">
          <select value={lugarAplicacion} onChange={(event) => setLugarAplicacion(event.target.value as LugarAplicacion | '')}>
            <option value="">Selecciona…</option>
            {(Object.keys(LUGAR_APLICACION_LABELS) as LugarAplicacion[]).map((lugar) => <option key={lugar} value={lugar}>{LUGAR_APLICACION_LABELS[lugar]}</option>)}
          </select>
        </Field>

        <Field label="Días de retiro de carne"><input name="retiroCarneDias" type="number" inputMode="numeric" min="0" /></Field>
        <Field label="Días de retiro de leche"><input name="retiroLecheDias" type="number" inputMode="numeric" min="0" /></Field>

        <Field label="Resultado"><input name="resultado" maxLength={60} autoComplete="off" /></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={confirmar.isPending}>Confirmar jornada</Button></div>
      </form>
      {confirmar.error && <Alert tone="danger">{normalizeApiError(confirmar.error).message}</Alert>}
    </div>
  </Modal>
}
