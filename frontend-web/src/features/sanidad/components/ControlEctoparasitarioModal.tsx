import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  crearControlEctoparasitario,
  getPrincipiosActivosRecientes,
  NIVEL_CARGA_PARASITARIA_LABELS,
  TIPO_ECTOPARASITO_LABELS,
  type CrearControlEctoparasitarioInput,
  type NivelCargaParasitaria,
  type TipoEctoparasito,
} from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface ControlEctoparasitarioModalProps {
  /** Mutuamente excluyentes: pasa uno u otro, nunca ambos. */
  animalId?: string
  loteGanaderoId?: string
  destinoLabel: string
  onClose: () => void
  onSaved: () => void
}

export function ControlEctoparasitarioModal({ animalId, loteGanaderoId, destinoLabel, onClose, onSaved }: ControlEctoparasitarioModalProps) {
  const client = useQueryClient()
  const [principioActivo, setPrincipioActivo] = useState('')
  const principiosRecientes = useQuery({
    queryKey: ['sanidad-control-ecto-principios', animalId, loteGanaderoId],
    queryFn: () => getPrincipiosActivosRecientes({ animalId, loteGanaderoId }),
  })

  const recientes = principiosRecientes.data ?? []
  const coincide = principioActivo.trim().length > 0
    && recientes.some((previo) => previo.trim().toLowerCase() === principioActivo.trim().toLowerCase())

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearControlEctoparasitarioInput = {
        animalId,
        loteGanaderoId,
        tipo: String(data.get('tipo')) as TipoEctoparasito,
        nivelCarga: String(data.get('nivelCarga')) as NivelCargaParasitaria,
        tratado: data.get('tratado') === 'on',
        producto: String(data.get('producto') || '') || undefined,
        principioActivo: principioActivo.trim() || undefined,
        fecha: String(data.get('fecha')),
        observaciones: String(data.get('observaciones') || '') || undefined,
      }
      return crearControlEctoparasitario(input)
    },
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['sanidad-control-ecto'] })
      onSaved()
    },
  })

  return <Modal open title="Registrar control ectoparasitario" onClose={onClose} wide description={`Checklist de control ectoparasitario para ${destinoLabel}.`}>
    <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
      <Field label="Tipo" required><select name="tipo" required defaultValue="GARRAPATA">{(Object.keys(TIPO_ECTOPARASITO_LABELS) as TipoEctoparasito[]).map((tipo) => <option key={tipo} value={tipo}>{TIPO_ECTOPARASITO_LABELS[tipo]}</option>)}</select></Field>
      <Field label="Nivel de carga" required><select name="nivelCarga" required defaultValue="BAJO">{(Object.keys(NIVEL_CARGA_PARASITARIA_LABELS) as NivelCargaParasitaria[]).map((nivel) => <option key={nivel} value={nivel}>{NIVEL_CARGA_PARASITARIA_LABELS[nivel]}</option>)}</select></Field>
      <Field label="Fecha" required><input name="fecha" type="date" required /></Field>
      <Field label="Tratado en este registro"><input name="tratado" type="checkbox" /></Field>
      <Field label="Producto"><input name="producto" maxLength={200} /></Field>
      <Field label="Principio activo">
        <input name="principioActivo" maxLength={200} value={principioActivo} onChange={(event) => setPrincipioActivo(event.target.value)} />
      </Field>
      {coincide && <div className="form-full"><Alert tone="warning">Los últimos registros usaron: {recientes.join(', ')} — considerá rotar el principio activo para evitar resistencia.</Alert></div>}
      <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
      <div className="form-actions"><Button type="submit" loading={crear.isPending}>Guardar control</Button></div>
    </form>
    {crear.error && <Alert tone="danger">{normalizeApiError(crear.error).message}</Alert>}
  </Modal>
}
