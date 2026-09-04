import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  crearControlNeonatal,
  ESTADO_CALOSTRADO_LABELS,
  MOMENTO_CONTROL_NEONATAL_LABELS,
  type CrearControlNeonatalInput,
  type EstadoCalostrado,
  type MomentoControlNeonatal,
} from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface ControlNeonatalModalProps {
  animalId: string
  animalCodigo: string
  onClose: () => void
  onSaved: () => void
}

export function ControlNeonatalModal({ animalId, animalCodigo, onClose, onSaved }: ControlNeonatalModalProps) {
  const client = useQueryClient()

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearControlNeonatalInput = {
        animalId,
        fechaControl: String(data.get('fechaControl')),
        momento: String(data.get('momento')) as MomentoControlNeonatal,
        calostrado: String(data.get('calostrado')) as EstadoCalostrado,
        ombligoDesinfectado: data.get('ombligoDesinfectado') === 'on',
        ombligoEstado: String(data.get('ombligoEstado') || '') || undefined,
        diarrea: data.get('diarrea') === 'on',
        estadoGeneral: String(data.get('estadoGeneral') || '') || undefined,
        lactancia: String(data.get('lactancia') || '') || undefined,
        temperaturaC: data.get('temperaturaC') ? Number(data.get('temperaturaC')) : undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      }
      return crearControlNeonatal(input)
    },
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['sanidad-control-neonatal', animalId] })
      onSaved()
    },
  })

  return <Modal open title="Registrar control neonatal" onClose={onClose} wide description={`Checklist de control neonatal para ${animalCodigo}.`}>
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
      <Field label="Momento" required><select name="momento" required defaultValue="DIA_0">{(Object.keys(MOMENTO_CONTROL_NEONATAL_LABELS) as MomentoControlNeonatal[]).map((momento) => <option key={momento} value={momento}>{MOMENTO_CONTROL_NEONATAL_LABELS[momento]}</option>)}</select></Field>
      <Field label="Fecha del control" required><input name="fechaControl" type="date" required /></Field>
      <Field label="Calostrado" required><select name="calostrado" required defaultValue="CORRECTO">{(Object.keys(ESTADO_CALOSTRADO_LABELS) as EstadoCalostrado[]).map((estado) => <option key={estado} value={estado}>{ESTADO_CALOSTRADO_LABELS[estado]}</option>)}</select></Field>
      <Field label="Temperatura (°C)"><input name="temperaturaC" type="number" inputMode="decimal" step="0.1" /></Field>
      <Field label="Ombligo desinfectado"><input name="ombligoDesinfectado" type="checkbox" /></Field>
      <Field label="Estado del ombligo"><input name="ombligoEstado" maxLength={200} /></Field>
      <Field label="Diarrea"><input name="diarrea" type="checkbox" /></Field>
      <Field label="Estado general"><input name="estadoGeneral" maxLength={200} /></Field>
      <Field label="Lactancia"><input name="lactancia" maxLength={200} /></Field>
      <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
      <div className="form-actions"><Button type="submit" loading={crear.isPending}>Guardar control</Button></div>
    </form>
    {crear.error && <Alert tone="danger">{normalizeApiError(crear.error).message}</Alert>}
  </Modal>
}
