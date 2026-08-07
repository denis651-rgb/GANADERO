import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Save, X } from 'lucide-react'
import { pesajeLoteSchema, type PesajeLoteForm, type PesajeLoteFormInput } from '@/features/pesajes/schema'
import { registrarPesajeLote } from '@/features/pesajes/api'
import { listLotes } from '@/features/lotes/api'
import { createUuid } from '@/shared/utils/uuid'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'

interface PesajeLoteFormProps {
  onSaved?: (registrados: number) => void
  onCancel?: () => void
}

export function PesajeLoteForm({ onSaved, onCancel }: PesajeLoteFormProps) {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState<{ tone: 'success' | 'info' | 'danger'; text: string } | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<PesajeLoteFormInput, unknown, PesajeLoteForm>({
    resolver: zodResolver(pesajeLoteSchema),
  })
  const lots = useQuery({ queryKey: ['pesaje-lote-lots'], queryFn: () => listLotes({ estado: 'ACTIVO', page: 0, size: 200 }) })

  async function submit(values: PesajeLoteForm) {
    setMessage(null)
    if (!navigator.onLine) {
      setMessage({ tone: 'info', text: 'El pesaje por lote requiere conexión; registra los pesajes individuales sin conexión.' })
      return
    }
    const input = { ...values, dispositivo: 'WEB', idempotencyKey: createUuid() }
    try {
      const registrados = await registrarPesajeLote(input)
      void queryClient.invalidateQueries({ queryKey: ['pesajes'] })
      setMessage({ tone: 'success', text: `Pesaje registrado para ${registrados.length} animal(es) del lote.` })
      onSaved?.(registrados.length)
    } catch (reason) {
      setMessage({ tone: 'danger', text: normalizeApiError(reason).message })
    }
  }

  return (
    <Card className="form-card">
      <div className="section-heading">
        <h3>Pesaje por lote</h3>
        {onCancel && <Button variant="ghost" onClick={onCancel}><X size={17} />Cerrar</Button>}
      </div>
      <form className="form-grid" onSubmit={handleSubmit(submit)} noValidate>
        {message && <div className="form-full"><Alert tone={message.tone}>{message.text}</Alert></div>}
        <Field label="Lote" error={errors.loteId?.message}>
          <select {...register('loteId')}><option value="">Selecciona un lote…</option>{lots.data?.content.filter((item) => item.estado === 'ACTIVO').map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
        </Field>
        <Field label="Peso (kg)" error={errors.pesoKg?.message} hint="El mismo peso se aplicará a todos los animales activos del lote.">
          <input type="number" min="1" step="0.1" {...register('pesoKg')} placeholder="250" />
        </Field>
        <Field label="Fecha" error={errors.fecha?.message}>
          <input type="date" {...register('fecha')} />
        </Field>
        <div className="form-full">
          <Field label="Observaciones" error={errors.observaciones?.message}>
            <textarea rows={3} {...register('observaciones')} />
          </Field>
        </div>
        <div className="form-full form-actions">
          <Button type="submit" loading={isSubmitting}><Save size={18} />Registrar pesaje del lote</Button>
        </div>
      </form>
    </Card>
  )
}
