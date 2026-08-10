import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { RotateCcw, Save, X } from 'lucide-react'
import { pesajeLoteSchema, type PesajeLoteForm, type PesajeLoteFormInput } from '@/features/pesajes/schema'
import { registrarPesajeMasivo } from '@/features/pesajes/api'
import { buildMasivoInput, failedAnimalIds, retryMasivoInput } from '@/features/pesajes/masivo'
import { listLotes, listMembresias } from '@/features/lotes/api'
import type { PesajeMasivoInput, PesajeMasivoResultado } from '@/features/pesajes/types'
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
  const [ultimoEnvio, setUltimoEnvio] = useState<PesajeMasivoInput | null>(null)
  const [resultado, setResultado] = useState<PesajeMasivoResultado | null>(null)
  const [reintentando, setReintentando] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<PesajeLoteFormInput, unknown, PesajeLoteForm>({
    resolver: zodResolver(pesajeLoteSchema),
  })
  const lots = useQuery({ queryKey: ['pesaje-lote-lots'], queryFn: () => listLotes({ estado: 'ACTIVO', page: 0, size: 200 }) })

  async function enviar(input: PesajeMasivoInput) {
    setMessage(null)
    setResultado(null)
    if (!navigator.onLine) {
      setMessage({ tone: 'info', text: 'El pesaje por lote requiere conexión; registra los pesajes individuales sin conexión.' })
      return
    }
    try {
      const res = await registrarPesajeMasivo(input)
      setUltimoEnvio(input)
      setResultado(res)
      void queryClient.invalidateQueries({ queryKey: ['pesajes'] })
      if (res.conError === 0) {
        setMessage({ tone: 'success', text: `Pesaje registrado para ${res.registrados} animal(es) del lote.` })
        onSaved?.(res.registrados)
      } else {
        setMessage({ tone: 'info', text: `${res.registrados} animal(es) pesado(s), ${res.conError} con error.` })
      }
    } catch (reason) {
      setMessage({ tone: 'danger', text: normalizeApiError(reason).message })
    }
  }

  async function submit(values: PesajeLoteForm) {
    const miembros = await listMembresias(values.loteId, true)
    const animalIds = miembros.filter((item) => !item.fechaSalida).map((item) => item.animalId)
    if (animalIds.length === 0) {
      setMessage({ tone: 'danger', text: 'El lote seleccionado no tiene animales activos.' })
      return
    }
    await enviar(buildMasivoInput({
      loteId: values.loteId,
      animalIds,
      pesoKg: values.pesoKg,
      fecha: values.fecha,
      observaciones: values.observaciones,
    }))
  }

  async function reintentar() {
    if (!ultimoEnvio || !resultado) return
    const fallidos = failedAnimalIds(resultado.items)
    if (fallidos.size === 0) return
    setReintentando(true)
    try {
      await enviar(retryMasivoInput(ultimoEnvio, fallidos))
    } finally {
      setReintentando(false)
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
        {resultado && resultado.conError > 0 && <div className="form-full">
          <Alert tone="danger">No se pudo registrar a {resultado.conError} animal(es). Revisa el motivo y reintenta solo los fallidos.</Alert>
          <div className="table-wrapper"><table><thead><tr><th>Animal</th><th>Motivo</th></tr></thead><tbody>{resultado.items.filter((item) => !item.ok).map((item) => <tr key={item.animalId}>
            <td><strong>{item.codigoAnimal ?? item.nombreAnimal ?? item.animalId}</strong></td>
            <td>{item.errorMessage ?? item.errorCode}</td>
          </tr>)}</tbody></table></div>
          <Button variant="secondary" onClick={reintentar} loading={reintentando}><RotateCcw size={16} />Reintentar solo los fallidos</Button>
        </div>}
        <div className="form-full form-actions">
          <Button type="submit" loading={isSubmitting}><Save size={18} />Registrar pesaje del lote</Button>
        </div>
      </form>
    </Card>
  )
}
