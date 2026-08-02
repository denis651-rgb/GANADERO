import { Link, useNavigate, useParams } from 'react-router-dom'
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
  const { register, handleSubmit, control, formState: { errors } } = useForm<UpdateAnimalInput>({ defaultValues: {
    codigo: animal.codigo, nombre: animal.nombre, sexo: animal.sexo, fechaNacimiento: animal.fechaNacimiento,
    fechaNacimientoEstimada: animal.fechaNacimientoEstimada, proposito: animal.proposito, razaPrincipalId: animal.razaPrincipalId,
    categoriaActualId: animal.categoriaActualId, propiedadActualId: animal.propiedadActualId, potreroActualId: animal.potreroActualId,
    color: animal.color, fechaIngreso: animal.fechaIngreso, precioAdquisicion: animal.precioAdquisicion,
    pesoNacimientoKg: animal.pesoNacimientoKg, condicionCorporalActual: animal.condicionCorporalActual,
    fotoPrincipalPath: animal.fotoPrincipalPath, observaciones: animal.observaciones, version: animal.version,
  } })
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const mutation = useMutation({ mutationFn: (input: UpdateAnimalInput) => updateAnimal(id, input), onSuccess: async () => { await Promise.all([client.invalidateQueries({ queryKey: ['animal', id] }), client.invalidateQueries({ queryKey: ['animal-history', id] }), client.invalidateQueries({ queryKey: ['animals'] })]); navigate(`/animales/${id}`) } })

  return <div className="page-stack narrow-page"><PageHeader eyebrow="Animales" title={`Editar ${animal.codigo}`} description="Actualiza identificación, características y ubicación." actions={<Link to={`/animales/${id}`}><Button variant="ghost"><ArrowLeft size={18} />Cancelar</Button></Link>} />
    {mutation.error && <Alert tone="danger">{normalizeApiError(mutation.error).message}</Alert>}
    <Card><form className="form-grid" onSubmit={handleSubmit((input) => mutation.mutate(input))}>
      <Field label="Código" error={errors.codigo?.message}><input {...register('codigo', { required: 'El código es obligatorio.' })} /></Field><Field label="Nombre"><input {...register('nombre')} /></Field>
      <Field label="Sexo"><select {...register('sexo')}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select></Field><Field label="Fecha de nacimiento"><input type="date" {...register('fechaNacimiento')} /></Field>
      <label className="checkbox-line"><input type="checkbox" {...register('fechaNacimientoEstimada')} /> Fecha estimada</label><Field label="Propósito"><select {...register('proposito')}><option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option></select></Field>
      <Field label="Raza"><select {...register('razaPrincipalId')} required>{catalogs.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Categoría"><select {...register('categoriaActualId')} required>{catalogs.categories.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Propiedad"><select {...register('propiedadActualId')} required>{catalogs.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Potrero"><select {...register('potreroActualId')} required>{catalogs.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Color"><input {...register('color')} /></Field><Field label="Fecha de ingreso"><input type="date" {...register('fechaIngreso')} /></Field>
      <Field label="Precio de adquisición"><input type="number" min="0" step="0.01" {...register('precioAdquisicion', { setValueAs: optionalNumber })} /></Field><Field label="Peso al nacer (kg)"><input type="number" min="0" step="0.001" {...register('pesoNacimientoKg', { setValueAs: optionalNumber })} /></Field>
      <Field label="Condición corporal"><input type="number" min="1" max="5" step="0.1" {...register('condicionCorporalActual', { setValueAs: optionalNumber })} /></Field><div className="form-full"><Field label="Observaciones"><textarea rows={4} {...register('observaciones')} /></Field></div>
      <div className="form-full form-actions"><Button type="submit" loading={mutation.isPending}><Save size={18} />Guardar cambios</Button></div>
    </form></Card>
  </div>
}

function optionalNumber(value: string) { return value === '' ? undefined : Number(value) }
async function loadCatalogShape() { const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()]); return { breeds, categories, properties, paddocks } }
