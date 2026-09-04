import { useState } from 'react'
import { todayInBolivia } from '@/shared/utils/date'
import { useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import { ArrowLeft, Save } from 'lucide-react'
import { getAnimal, listCategorias, listRazas, updateAnimal } from '@/features/animales/api'
import type { AnimalSummary, UpdateAnimalInput } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'
import { useUnsavedChanges } from '@/shared/hooks/useUnsavedChanges'
import { UnsavedChangesDialog } from '@/shared/components/UnsavedChangesDialog'

export function EditarAnimalPage() {
  const { id = '' } = useParams()
  const animal = useQuery({ queryKey: ['animal', id], queryFn: () => getAnimal(id), enabled: Boolean(id) })
  const catalogs = useQuery({ queryKey: ['animal-form-catalogs'], queryFn: loadCatalogShape })
  if (animal.isPending || catalogs.isPending) return <LoadingState message="Preparando edición…" />
  if (!animal.data || !catalogs.data) return <Alert tone="danger">No se pudo cargar el animal.</Alert>
  return <AnimalEditForm animal={animal.data} catalogs={catalogs.data} />
}

function AnimalEditForm({ animal, catalogs }: { animal: AnimalSummary; catalogs: Awaited<ReturnType<typeof loadCatalogShape>> }) {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const { register, handleSubmit, control, setValue, formState: { isDirty } } = useForm<UpdateAnimalInput>({ shouldFocusError: true, defaultValues: {
    nombre: animal.nombre, sexo: animal.sexo, fechaNacimiento: animal.fechaNacimiento,
    fechaNacimientoEstimada: animal.fechaNacimientoEstimada, proposito: animal.proposito, razaPrincipalId: animal.razaPrincipalId,
    categoriaActualId: animal.categoriaActualId, propiedadActualId: animal.propiedadActualId, potreroActualId: animal.potreroActualId,
    color: animal.color, fechaIngreso: animal.fechaIngreso, precioAdquisicion: animal.precioAdquisicion,
    pesoIngresoKg: animal.pesoIngresoKg, pesoIngresoEstimado: animal.pesoIngresoEstimado ?? true,
    pesoNacimientoKg: animal.pesoNacimientoKg, condicionCorporalActual: animal.condicionCorporalActual,
    fotoPrincipalPath: animal.fotoPrincipalPath, observaciones: animal.observaciones, version: animal.version,
  } })
  const initialBirth = animal.fechaNacimiento ? (animal.fechaNacimientoEstimada ? 'ESTIMADA' : 'CONOCIDA') : 'DESCONOCIDA'
  const [tipoNacimiento, setTipoNacimiento] = useState(initialBirth)
  const corregirPeso = useWatch({ control, name: 'corregirPesoCompra' })
  const pesoIngreso = useWatch({ control, name: 'pesoIngresoKg' })
  const unsaved = useUnsavedChanges(isDirty || tipoNacimiento !== initialBirth)
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const mutation = useMutation({ mutationFn: (input: UpdateAnimalInput) => updateAnimal(id, input), onSuccess: async () => { await Promise.all([client.invalidateQueries({ queryKey: ['animal', id] }), client.invalidateQueries({ queryKey: ['animal-timeline', id] }), client.invalidateQueries({ queryKey: ['animals'] })]); navigate(`/animales/${id}`) } })

  return <div className="page-stack narrow-page"><PageHeader eyebrow="Animales" title={`Editar ${animal.codigo}`} description="Actualiza identificación, características y ubicación." actions={<Button variant="ghost" onClick={() => unsaved.requestLeave(() => navigate(`/animales/${id}`))}><ArrowLeft size={18} aria-hidden="true" />Cancelar</Button>} />
    {mutation.error && <Alert tone="danger">{normalizeApiError(mutation.error).message}</Alert>}
    <Card><form className="form-grid" onSubmit={handleSubmit((input) => mutation.mutate({ ...input, fechaNacimiento: tipoNacimiento === 'DESCONOCIDA' ? undefined : input.fechaNacimiento, fechaNacimientoEstimada: tipoNacimiento === 'ESTIMADA', quitarFechaNacimiento: tipoNacimiento === 'DESCONOCIDA', pesoIngresoEstimado: input.pesoIngresoKg != null ? input.pesoIngresoEstimado : undefined }))}>
      <div className="form-section-title form-full"><h2>Información básica</h2></div>
      <Field label="Código" hint="Identificador interno permanente"><input value={animal.codigo} readOnly /></Field><Field label="Nombre"><input {...register('nombre')} /></Field>
      <Field label="Sexo"><select {...register('sexo')}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select></Field><Field label="Nacimiento"><select value={tipoNacimiento} onChange={(event) => setTipoNacimiento(event.target.value)}><option value="DESCONOCIDA">Desconocido</option><option value="ESTIMADA">Fecha estimada</option><option value="CONOCIDA">Fecha conocida</option></select></Field>
      <Field label="Fecha de nacimiento"><input type="date" max={todayInBolivia()} disabled={tipoNacimiento === 'DESCONOCIDA'} required={tipoNacimiento !== 'DESCONOCIDA'} {...register('fechaNacimiento')} /></Field>
      <Field label="Propósito"><select {...register('proposito')}><option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option></select></Field>
      <div className="form-section-title form-full"><h2>Clasificación</h2></div>
      <Field label="Raza"><select {...register('razaPrincipalId')} required>{catalogs.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Categoría"><select {...register('categoriaActualId')} required>{catalogs.categories.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <div className="form-section-title form-full"><h2>Ubicación</h2></div>
      <Field label="Propiedad"><select {...register('propiedadActualId')} required>{catalogs.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Potrero"><select {...register('potreroActualId')} required>{catalogs.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <div className="form-section-title form-full"><h2>Información adicional</h2></div>
      <Field label="Color"><input {...register('color')} /></Field><Field label="Fecha de ingreso"><input type="date" max={todayInBolivia()} {...register('fechaIngreso')} /></Field>
      <Field label="Precio de adquisición"><input type="number" inputMode="decimal" min="0" step="0.01" {...register('precioAdquisicion', { setValueAs: optionalNumber })} /></Field><Field label="Peso al nacer (kg)" hint="Solo si conoces el peso real al nacimiento."><input disabled={corregirPeso} type="number" inputMode="decimal" min="0" step="0.001" {...register('pesoNacimientoKg', { setValueAs: optionalNumber })} /></Field>
      {animal.origen === 'COMPRADO' && animal.pesoNacimientoKg != null && animal.pesoIngresoKg == null && <div className="form-full">
        <p>Revisa el peso antiguo ({animal.pesoNacimientoKg} kg): si era de compra, confirma la corrección. Se conservará constancia en el historial.</p>
        <label className="checkbox-line"><input type="checkbox" {...register('corregirPesoCompra', { onChange: (event) => { if (event.target.checked) setValue('pesoIngresoKg', animal.pesoNacimientoKg, { shouldDirty: true }) } })} />Este peso corresponde a la compra, no al nacimiento</label>
      </div>}
      <Field label="Peso al ingreso (kg)" hint="Es independiente del peso al nacer."><input required={corregirPeso} type="number" min="0.001" step="0.001" {...register('pesoIngresoKg', { setValueAs: optionalNumber })} /></Field>
      <Field label="Tipo de peso al ingreso"><select disabled={pesoIngreso == null} {...register('pesoIngresoEstimado', { setValueAs: (value) => value === true || value === 'true' })}><option value="true">Estimado</option><option value="false">Medido</option></select></Field>
      <Field label="Condición corporal"><input type="number" inputMode="decimal" min="1" max="5" step="0.1" {...register('condicionCorporalActual', { setValueAs: optionalNumber })} /></Field><div className="form-full"><Field label="Observaciones"><textarea rows={4} {...register('observaciones')} /></Field></div>
      <div className="form-full form-actions"><Button type="submit" loading={mutation.isPending}><Save size={18} />Guardar cambios</Button></div>
    </form></Card>
    <UnsavedChangesDialog open={unsaved.open} onStay={unsaved.cancelLeave} onLeave={unsaved.discardAndLeave} />
  </div>
}

function optionalNumber(value: string | number | null | undefined) {
  return value == null || (typeof value === 'string' && value.trim() === '') ? undefined : Number(value)
}
async function loadCatalogShape() { const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()]); return { breeds, categories, properties, paddocks } }
