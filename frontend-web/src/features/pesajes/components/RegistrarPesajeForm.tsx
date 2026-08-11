import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Save, X } from 'lucide-react'
import { registrarPesajeSchema, type RegistrarPesajeForm, type RegistrarPesajeFormInput } from '@/features/pesajes/schema'
import { registrarPesaje } from '@/features/pesajes/api'
import type { AnimalSummary } from '@/features/animales/types'
import { listLotes } from '@/features/lotes/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { AnimalPicker } from '@/features/pesajes/components/AnimalPicker'
import { queueSyncOperation } from '@/offline/operationQueue'
import { offlineFormCatalogs } from '@/offline/catalogs'
import { createUuid } from '@/shared/utils/uuid'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import type { Pesaje } from '@/features/pesajes/types'

const tipos = [
  { value: 'RUTINA', label: 'Rutina' },
  { value: 'NACIMIENTO', label: 'Nacimiento' },
  { value: 'DESTETE', label: 'Destete' },
  { value: 'ENTRADA', label: 'Entrada' },
  { value: 'VENTA', label: 'Venta' },
  { value: 'PESADA_ESPECIAL', label: 'Pesada especial' },
] as const

interface RegistrarPesajeFormProps {
  onSaved?: (pesaje: Pesaje) => void
  onCancel?: () => void
}

export function RegistrarPesajeForm({ onSaved, onCancel }: RegistrarPesajeFormProps) {
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<AnimalSummary | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'info' | 'danger'; text: string } | null>(null)
  const { register, handleSubmit, control, setValue, reset, formState: { errors, isSubmitting } } = useForm<RegistrarPesajeFormInput, unknown, RegistrarPesajeForm>({
    resolver: zodResolver(registrarPesajeSchema),
    shouldFocusError: true,
    defaultValues: { animalId: '', tipo: 'RUTINA' },
  })
  const propertyId = useWatch({ control, name: 'propiedadId' })
  const catalogs = useQuery({
    queryKey: ['pesaje-form-catalogs'],
    queryFn: async () => {
      if (!navigator.onLine) return offlineFormCatalogs()
      const [properties, paddocks, lots] = await Promise.all([
        listPropiedades(),
        listPotreros(),
        listLotes({ estado: 'ACTIVO', page: 0, size: 200 }),
      ])
      return { properties, paddocks, lots }
    },
  })

  async function submit(values: RegistrarPesajeForm) {
    if (!selected) {
      setMessage({ tone: 'danger', text: 'Selecciona un animal para registrar el pesaje.' })
      return
    }
    setMessage(null)
    const id = createUuid()
    const input = {
      ...values,
      animalId: selected.id,
      id,
      dispositivo: 'WEB',
      clienteUuid: id,
      idempotencyKey: id,
    }
    try {
      if (!navigator.onLine) {
        await queueSyncOperation({
          tipo: 'PESAJE_REGISTRAR',
          entidad: 'PESAJE',
          idempotencyKey: id,
          datos: input,
        })
        setMessage({ tone: 'info', text: 'Pesaje guardado en el dispositivo. Quedó pendiente de sincronización.' })
        reset({ animalId: '', tipo: 'RUTINA' })
        setSelected(null)
        return
      }
      const created = await registrarPesaje(input)
      void queryClient.invalidateQueries({ queryKey: ['pesajes'] })
      setMessage({ tone: 'success', text: `Pesaje de ${created.pesoKg} kg registrado para ${created.codigoAnimal ?? created.animalId}.` })
      reset({ animalId: '', tipo: 'RUTINA' })
      setSelected(null)
      onSaved?.(created)
    } catch (reason) {
      setMessage({ tone: 'danger', text: normalizeApiError(reason).message })
    }
  }

  return (
    <Card className="form-card">
      <div className="section-heading">
        <h3>Registrar pesaje individual</h3>
        {onCancel && <Button variant="ghost" onClick={onCancel}><X size={17} />Cerrar</Button>}
      </div>
      <form className="form-grid" onSubmit={handleSubmit(submit)} noValidate>
        {message && <div className="form-full"><Alert tone={message.tone}>{message.text}</Alert></div>}
        <div className="form-full">
          <AnimalPicker
            value={selected}
            onChange={(animal) => { setSelected(animal); setValue('animalId', animal.id, { shouldValidate: true }) }}
            error={errors.animalId?.message}
          />
        </div>
        <Field label="Fecha" error={errors.fecha?.message}>
          <input type="date" {...register('fecha')} />
        </Field>
        <Field label="Peso (kg)" error={errors.pesoKg?.message}>
          <input type="number" inputMode="decimal" min="1" step="0.1" {...register('pesoKg')} placeholder="250" />
        </Field>
        <Field label="Tipo" error={errors.tipo?.message}>
          <select {...register('tipo')}>{tipos.map((tipo) => <option key={tipo.value} value={tipo.value}>{tipo.label}</option>)}</select>
        </Field>
        <Field label="Condición corporal" error={errors.condicionCorporal?.message} hint="Escala del 1 al 9.">
          <input type="number" inputMode="decimal" min="1" max="9" step="0.25" {...register('condicionCorporal')} placeholder="3.5" />
        </Field>
        <Field label="Báscula" error={errors.bascula?.message}>
          <input {...register('bascula')} placeholder="Báscula 1" />
        </Field>
        <Field label="Propiedad" error={errors.propiedadId?.message}>
          <select {...register('propiedadId')}><option value="">Sin especificar</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        </Field>
        <Field label="Potrero" error={errors.potreroId?.message}>
          <select {...register('potreroId')}><option value="">Sin especificar</option>{catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        </Field>
        <Field label="Lote" error={errors.loteId?.message}>
          <select {...register('loteId')}><option value="">Sin especificar</option>{catalogs.data?.lots.content.filter((item) => item.estado === 'ACTIVO').map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        </Field>
        <div className="form-full">
          <Field label="Observaciones" error={errors.observaciones?.message}>
            <textarea rows={3} {...register('observaciones')} />
          </Field>
        </div>
        <div className="form-full form-actions">
          <Button type="submit" loading={isSubmitting}><Save size={18} />Guardar pesaje</Button>
        </div>
      </form>
    </Card>
  )
}
