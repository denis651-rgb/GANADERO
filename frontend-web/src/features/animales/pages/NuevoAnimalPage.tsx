import { useState } from 'react'
import { todayInBolivia } from '@/shared/utils/date'
import { useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Save } from 'lucide-react'
import { createAnimalSchema } from '@/features/animales/schema'
import { createAnimal, listCategorias, listRazas } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import type { AnimalSummary, CreateAnimalInput } from '@/features/animales/types'
import type { Page } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { PageHeader } from '@/shared/components/PageHeader'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import { useUnsavedChanges } from '@/shared/hooks/useUnsavedChanges'
import { UnsavedChangesDialog } from '@/shared/components/UnsavedChangesDialog'

export function NuevoAnimalPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [message, setMessage] = useState<{ tone: 'success' | 'info' | 'danger'; text: string } | null>(null)
  const { register, handleSubmit, control, formState: { errors, isSubmitting, isDirty } } = useForm<CreateAnimalInput>({
    resolver: zodResolver(createAnimalSchema),
    shouldFocusError: true,
    defaultValues: {
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
      fechaIngreso: todayInBolivia(),
      pesoIngresoEstimado: true,
    },
  })
  const [tipoNacimiento, setTipoNacimiento] = useState('DESCONOCIDA')
  const unsaved = useUnsavedChanges(isDirty || tipoNacimiento !== 'DESCONOCIDA')
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const catalogs = useQuery({ queryKey: ['animal-form-catalogs'], queryFn: async () => {
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
    return { breeds, categories, properties, paddocks }
  } })

  async function submit(values: CreateAnimalInput) {
    setMessage(null)
    try {
      if (tipoNacimiento !== 'DESCONOCIDA' && !values.fechaNacimiento) {
        setMessage({ tone: 'danger', text: 'Indica la fecha de nacimiento o selecciona Desconocido.' })
        return
      }
      const created = await createAnimal({ ...values,
        fechaNacimiento: tipoNacimiento === 'DESCONOCIDA' ? undefined : values.fechaNacimiento,
        fechaNacimientoEstimada: tipoNacimiento === 'ESTIMADA',
        pesoIngresoEstimado: values.pesoIngresoKg != null ? values.pesoIngresoEstimado : undefined,
      })
      queryClient.setQueriesData<Page<AnimalSummary>>({ queryKey: ['animals'] }, (current) => current ? {
        ...current,
        content: [created, ...current.content.filter((animal) => animal.id !== created.id)].slice(0, current.size),
        totalElements: current.totalElements + (current.content.some((animal) => animal.id === created.id) ? 0 : 1),
        totalPages: Math.ceil((current.totalElements + (current.content.some((animal) => animal.id === created.id) ? 0 : 1)) / current.size),
      } : {
        content: [created],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      navigate('/animales')
      void queryClient.invalidateQueries({ queryKey: ['animals'] })
    } catch (reason) {
      setMessage({ tone: 'danger', text: normalizeApiError(reason).message })
    }
  }

  return (
    <div className="page-stack narrow-page">
      <PageHeader
        eyebrow="Animales"
        title="Registrar animal"
        description="Completa la ficha del animal y guárdala para sumarlo al hato de tu empresa."
        actions={<Button variant="ghost" onClick={() => unsaved.requestLeave(() => navigate('/animales'))}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>}
      />
      <Card>
        <form className="form-grid" onSubmit={handleSubmit(submit)} noValidate>
          {message && <div className="form-full"><Alert tone={message.tone}>{message.text}</Alert></div>}
          <div className="form-section-title form-full"><h2>Información básica</h2></div>
          <Field label="Nombre opcional" error={errors.nombre?.message}>
            <input {...register('nombre')} placeholder="Lucera" />
          </Field>
          <Field label="Sexo" error={errors.sexo?.message}>
            <select {...register('sexo')}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select>
          </Field>
          <Field label="Nacimiento"><select value={tipoNacimiento} onChange={(event) => setTipoNacimiento(event.target.value)}><option value="DESCONOCIDA">Desconocido</option><option value="ESTIMADA">Fecha estimada</option><option value="CONOCIDA">Fecha conocida</option></select></Field>
          <Field label="Fecha de nacimiento" error={errors.fechaNacimiento?.message}>
            <input type="date" max={todayInBolivia()} disabled={tipoNacimiento === 'DESCONOCIDA'} {...register('fechaNacimiento')} />
          </Field>
          <div className="form-section-title form-full"><h2>Clasificación</h2></div>
          <Field label="Propósito" error={errors.proposito?.message}>
            <select {...register('proposito')}>
              <option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option>
            </select>
          </Field>
          <Field label="Origen" error={errors.origen?.message}>
            <select {...register('origen')}><option value="NACIDO">Nacido</option><option value="COMPRADO">Comprado</option><option value="TRANSFERIDO">Transferido</option></select>
          </Field>
          <Field label="Raza" error={errors.razaPrincipalId?.message}>
            <select {...register('razaPrincipalId')}><option value="">Selecciona…</option>{catalogs.data?.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <Field label="Categoría" error={errors.categoriaActualId?.message}>
            <select {...register('categoriaActualId')}><option value="">Selecciona…</option>{catalogs.data?.categories.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <div className="form-section-title form-full"><h2>Ubicación</h2></div>
          <Field label="Propiedad" error={errors.propiedadActualId?.message}>
            <select {...register('propiedadActualId')}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <Field label="Potrero" error={errors.potreroActualId?.message} hint="Debe pertenecer a la propiedad seleccionada.">
            <select {...register('potreroActualId')}><option value="">Selecciona…</option>{catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <div className="form-section-title form-full"><h2>Información adicional</h2></div>
          <Field label="Fecha de ingreso"><input type="date" max={todayInBolivia()} {...register('fechaIngreso')} /></Field>
          <Field label="Peso al ingreso (kg)" hint="No corresponde al peso al nacer." error={errors.pesoIngresoKg?.message}><input type="number" min="0.001" step="0.001" {...register('pesoIngresoKg', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field>
          <Field label="Tipo de peso al ingreso"><select {...register('pesoIngresoEstimado', { setValueAs: (value) => value === true || value === 'true' })}><option value="true">Estimado</option><option value="false">Medido</option></select></Field>
          <div className="form-full">
            <Field label="Observaciones" error={errors.observaciones?.message}>
              <textarea rows={4} {...register('observaciones')} />
            </Field>
          </div>
          <div className="form-full form-actions">
            <Button type="submit" loading={isSubmitting}><Save size={18} />Guardar animal</Button>
          </div>
        </form>
      </Card>
      <UnsavedChangesDialog open={unsaved.open} onStay={unsaved.cancelLeave} onLeave={unsaved.discardAndLeave} />
    </div>
  )
}
