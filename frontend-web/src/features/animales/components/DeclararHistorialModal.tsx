import { type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { registrarAplicacionDeclarada, TIPO_ACTIVIDAD_LABELS, type TipoActividad } from '@/features/sanidad/api'
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
 * PLAN_SANITARIO_SANTA_CRUZ.md, sección 7): solo actividad, fecha y observaciones. Vincular
 * a un item de plan concreto (planItemId) queda para cuando se declare desde Sanidad.
 */
export function DeclararHistorialModal({ animalId, animalLabel, onClose, onSuccess }: DeclararHistorialModalProps) {
  const registrar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarAplicacionDeclarada({
        animalId,
        tipoActividad: String(data.get('tipoActividad')) as TipoActividad,
        fechaAplicacion: String(data.get('fechaAplicacion')),
        observaciones: String(data.get('observaciones') || '') || undefined,
      })
    },
    onSuccess: () => onSuccess(),
  })
  const normalized = registrar.error ? normalizeApiError(registrar.error) : null

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (registrar.isPending) return
    registrar.mutate(event.currentTarget)
  }

  return <Modal open title={`Declarar historial de ${animalLabel}`} description="Registra lo que certifica el proveedor para este animal, sin necesidad de una jornada." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger">{normalized.message}</Alert>}
      <Field label="Actividad" required>
        <select name="tipoActividad" required defaultValue="">
          <option value="" disabled>Selecciona…</option>
          {(Object.keys(TIPO_ACTIVIDAD_LABELS) as TipoActividad[]).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ACTIVIDAD_LABELS[tipo]}</option>)}
        </select>
      </Field>
      <Field label="Fecha" required><input name="fechaAplicacion" type="date" required max={HOY} /></Field>
      <Field label="Observaciones" hint="Ej. según certificado del proveedor Estancia El Roble"><textarea name="observaciones" rows={3} maxLength={1000} /></Field>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button type="submit" loading={registrar.isPending}><Save size={17} aria-hidden="true" />Guardar</Button>
      </div>
    </form>
  </Modal>
}
