import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useQueryClient } from '@tanstack/react-query'
import { Ban, X } from 'lucide-react'
import { anularPesajeSchema, type AnularPesajeForm } from '@/features/pesajes/schema'
import { anularPesaje } from '@/features/pesajes/api'
import type { Pesaje } from '@/features/pesajes/types'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import { useToast } from '@/shared/toast/useToast'

interface AnularPesajeFormProps {
  pesaje: Pesaje
  onAnnulled?: (pesaje: Pesaje) => void
  onCancel?: () => void
}

export function AnularPesajeForm({ pesaje, onAnnulled, onCancel }: AnularPesajeFormProps) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<AnularPesajeForm>({
    resolver: zodResolver(anularPesajeSchema),
  })

  async function submit(values: AnularPesajeForm) {
    setError(null)
    try {
      const updated = await anularPesaje(pesaje.id, { motivo: values.motivo, version: pesaje.version })
      void queryClient.invalidateQueries({ queryKey: ['pesajes'] })
      showToast('Pesaje anulado correctamente.')
      onAnnulled?.(updated)
    } catch (reason) {
      setError(normalizeApiError(reason).message)
    }
  }

  return (
    <Card className="form-card">
      <div className="section-heading">
        <h3>Anular pesaje {pesaje.codigoAnimal ? `de ${pesaje.codigoAnimal}` : ''}</h3>
        {onCancel && <Button variant="ghost" onClick={onCancel}><X size={17} />Cerrar</Button>}
      </div>
      <form className="form-grid" onSubmit={handleSubmit(submit)} noValidate>
        {error && <div className="form-full"><Alert tone="danger">{error}</Alert></div>}
        <div className="form-full">
          <Field label="Motivo de anulación" error={errors.motivo?.message}>
            <textarea rows={3} {...register('motivo')} placeholder="Peso registrado por error…" />
          </Field>
        </div>
        <div className="form-full form-actions">
          <Button type="submit" variant="danger" loading={isSubmitting}><Ban size={18} />Confirmar anulación</Button>
        </div>
      </form>
    </Card>
  )
}
