import { useState } from 'react'
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
import { queueSyncOperation } from '@/offline/operationQueue'
import { offlineFormCatalogs } from '@/offline/catalogs'
import { createUuid } from '@/shared/utils/uuid'
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
  const { register, handleSubmit, control, reset, formState: { errors, isSubmitting, isDirty } } = useForm<CreateAnimalInput>({
    resolver: zodResolver(createAnimalSchema),
    shouldFocusError: true,
    defaultValues: {
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
    },
  })
  const unsaved = useUnsavedChanges(isDirty)
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const catalogs = useQuery({ queryKey: ['animal-form-catalogs'], queryFn: async () => {
    if (!navigator.onLine) return offlineFormCatalogs()
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
    return { breeds, categories, properties, paddocks }
  } })

  async function submit(values: CreateAnimalInput) {
    setMessage(null)
    try {
      if (!navigator.onLine) {
        const id = createUuid()
        await queueSyncOperation({
          tipo: 'ANIMAL_CREAR',
          entidad: 'ANIMAL',
          idempotencyKey: id,
          datos: { ...values, id },
        })
        setMessage({ tone: 'info', text: 'Animal guardado en el dispositivo. Quedó pendiente de sincronización.' })
        reset()
        return
      }
      const created = await createAnimal(values)
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
          {!navigator.onLine && catalogs.data && (!catalogs.data.breeds.length || !catalogs.data.categories.length || !catalogs.data.properties.length) && (
            <div className="form-full"><Alert tone="info" title="Faltan datos para registrar animales sin conexión">Este dispositivo no descargó los catálogos necesarios. Conéctalo a internet y utiliza “Preparar datos offline” en Sincronización.</Alert></div>
          )}
          <div className="form-section-title form-full"><h2>Información básica</h2></div>
          <Field label="Código interno" hint="Se asigna al guardar">
            <input value="Automático · ANI-######" readOnly aria-label="Código interno automático" />
          </Field>
          <Field label="Nombre opcional" error={errors.nombre?.message}>
            <input {...register('nombre')} placeholder="Lucera" />
          </Field>
          <Field label="Sexo" error={errors.sexo?.message}>
            <select {...register('sexo')}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select>
          </Field>
          <Field label="Fecha de nacimiento" error={errors.fechaNacimiento?.message}>
            <input type="date" {...register('fechaNacimiento')} />
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
