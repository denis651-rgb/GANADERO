import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { listActivePlanItems, registrarAplicacionDeclarada, TIPO_ACTIVIDAD_LABELS, type TipoActividad } from '@/features/sanidad/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'

interface DeclararHistorialModalProps {
  animalId: string
  animalLabel: string
  onClose: () => void
  onSuccess: () => void
}

const HOY = new Date().toISOString().slice(0, 10)

/**
 * Acción corta y frecuente (declarar UNA actividad de UN animal) — modal, no página, según
 * el criterio de interacción del proyecto. Deliberadamente mínimo (docs/backend/
 * PLAN_SANITARIO_SANTA_CRUZ.md, sección 7): actividad, ítem del plan opcional, fecha,
 * producto y observaciones. Vincular un ítem del plan hace que el calendario sanitario
 * calcule la próxima aplicación desde esta fecha y no dispare vacuna próxima/vencida para
 * ese ítem.
 */
export function DeclararHistorialModal({ animalId, animalLabel, onClose, onSuccess }: DeclararHistorialModalProps) {
  const [tipoActividad, setTipoActividad] = useState<TipoActividad | ''>('')
  const [planItemId, setPlanItemId] = useState('')
  const planItems = useQuery({ queryKey: ['sanidad-plan-items-activos'], queryFn: listActivePlanItems })
  const opcionesPlan = (planItems.data ?? []).filter((item) => item.tipoActividad === tipoActividad)

  const registrar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarAplicacionDeclarada({
        animalId,
        tipoActividad: tipoActividad as TipoActividad,
        planItemId: planItemId || undefined,
        fechaAplicacion: String(data.get('fechaAplicacion')),
        productoTexto: String(data.get('productoTexto') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      })
    },
    onSuccess: () => onSuccess(),
  })
  const normalized = registrar.error ? normalizeApiError(registrar.error) : null

  function cambiarTipoActividad(value: string) {
    setTipoActividad(value as TipoActividad)
    setPlanItemId('')
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (registrar.isPending) return
    registrar.mutate(event.currentTarget)
  }

  return <Modal open title={`Declarar historial de ${animalLabel}`} description="Registra lo que certifica el proveedor para este animal, sin necesidad de una jornada." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger">{normalized.message}</Alert>}
      <Field label="Actividad" required>
        <select required value={tipoActividad} onChange={(event) => cambiarTipoActividad(event.target.value)}>
          <option value="" disabled>Selecciona…</option>
          {(Object.keys(TIPO_ACTIVIDAD_LABELS) as TipoActividad[]).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}
        </select>
      </Field>
      {tipoActividad && <Field
        label="Ítem del plan sanitario"
        hint={opcionesPlan.length > 0
          ? 'Opcional. Si lo vinculas, se actualiza el calendario sanitario y se evitan alertas de vacuna próxima/vencida para este ítem.'
          : 'No hay ítems activos de este tipo en el plan sanitario; se guardará solo como antecedente.'}
      >
        <select value={planItemId} onChange={(event) => setPlanItemId(event.target.value)} disabled={opcionesPlan.length === 0}>
          <option value="">Sin vincular (solo antecedente)</option>
          {opcionesPlan.map((item) => <option key={item.id} value={item.id}>{item.nombre}{item.productoRecomendadoTexto ? ` · ${item.productoRecomendadoTexto}` : ''}</option>)}
        </select>
      </Field>}
      <Field label="Fecha" required><input name="fechaAplicacion" type="date" required max={HOY} /></Field>
      <Field label="Producto o medicamento"><input name="productoTexto" placeholder="Nombre informado por el proveedor…" maxLength={300} /></Field>
      <Field label="Observaciones" hint="Ej. según certificado del proveedor Estancia El Roble"><textarea name="observaciones" rows={3} maxLength={1000} /></Field>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button type="submit" loading={registrar.isPending}><Save size={17} aria-hidden="true" />Guardar</Button>
      </div>
    </form>
  </Modal>
}
