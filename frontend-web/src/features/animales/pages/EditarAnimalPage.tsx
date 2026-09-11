import { useEffect, useState } from 'react'
import { todayInBolivia } from '@/shared/utils/date'
import { useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import { ArrowLeft, Save } from 'lucide-react'
import { getAnimal, listCategorias, listRazas, updateAnimal } from '@/features/animales/api'
import { calcularNacimientoEstimado, categoriaSugerida } from '@/features/animales/edad'
import type { AnimalSummary, UpdateAnimalInput } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
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

type TipoNacimiento = 'CONOCIDA' | 'ESTIMADA_FECHA' | 'ESTIMADA_EDAD' | 'DESCONOCIDA'

function AnimalEditForm({ animal, catalogs }: { animal: AnimalSummary; catalogs: Awaited<ReturnType<typeof loadCatalogShape>> }) {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const { register, handleSubmit, control, setValue, formState: { errors, isDirty } } = useForm<UpdateAnimalInput>({ shouldFocusError: true, defaultValues: {
    nombre: animal.nombre, sexo: animal.sexo, fechaNacimiento: animal.fechaNacimiento,
    fechaNacimientoEstimada: animal.fechaNacimientoEstimada, proposito: animal.proposito, razaPrincipalId: animal.razaPrincipalId,
    categoriaActualId: animal.categoriaActualId, propiedadActualId: animal.propiedadActualId, potreroActualId: animal.potreroActualId,
    color: animal.color, fechaIngreso: animal.fechaIngreso, precioAdquisicion: animal.precioAdquisicion,
    pesoIngresoKg: animal.pesoIngresoKg, pesoIngresoEstimado: animal.pesoIngresoEstimado ?? true,
    pesoNacimientoKg: animal.pesoNacimientoKg, condicionCorporalActual: animal.condicionCorporalActual,
    fotoPrincipalPath: animal.fotoPrincipalPath, observaciones: animal.observaciones, version: animal.version,
    edadDeclaradaValor: animal.edadDeclaradaValor, edadDeclaradaUnidad: animal.edadDeclaradaUnidad ?? 'MESES',
    observacionEstimacion: animal.observacionEstimacion,
  } })
  const initialBirth: TipoNacimiento = !animal.fechaNacimiento
    ? 'DESCONOCIDA'
    : !animal.fechaNacimientoEstimada
      ? 'CONOCIDA'
      : animal.edadDeclaradaValor != null ? 'ESTIMADA_EDAD' : 'ESTIMADA_FECHA'
  const [tipoNacimiento, setTipoNacimiento] = useState<TipoNacimiento>(initialBirth)
  const corregirPeso = useWatch({ control, name: 'corregirPesoCompra' })
  const pesoIngreso = useWatch({ control, name: 'pesoIngresoKg' })
  const fechaNacimiento = useWatch({ control, name: 'fechaNacimiento' })
  const fechaIngreso = useWatch({ control, name: 'fechaIngreso' })
  const sexo = useWatch({ control, name: 'sexo' })
  const edadDeclaradaValor = useWatch({ control, name: 'edadDeclaradaValor' })
  const edadDeclaradaUnidad = useWatch({ control, name: 'edadDeclaradaUnidad' })
  const unsaved = useUnsavedChanges(isDirty || tipoNacimiento !== initialBirth)
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const referenciaEdad = animal.origen === 'NACIDO' ? todayInBolivia() : (fechaIngreso || todayInBolivia())
  const nacimientoCalculado = calcularNacimientoEstimado(referenciaEdad, edadDeclaradaValor, edadDeclaradaUnidad)
  const nacimientoClasificacion = tipoNacimiento === 'CONOCIDA' || tipoNacimiento === 'ESTIMADA_FECHA' ? fechaNacimiento
    : tipoNacimiento === 'ESTIMADA_EDAD' ? nacimientoCalculado : undefined
  const categoriaManual = catalogs.categories.find((item) => item.id === animal.categoriaActualId && !item.clasificacionAutomatica)
  const categoriaAutomatica = categoriaManual ?? categoriaSugerida(catalogs.categories, sexo, nacimientoClasificacion, todayInBolivia())
  const categoriaAutomaticaId = categoriaAutomatica?.id
  useEffect(() => {
    if (categoriaAutomaticaId) setValue('categoriaActualId', categoriaAutomaticaId, { shouldValidate: true })
  }, [categoriaAutomaticaId, setValue])
  const mutation = useMutation({ mutationFn: (input: UpdateAnimalInput) => updateAnimal(id, input), onSuccess: async () => { await Promise.all([client.invalidateQueries({ queryKey: ['animal', id] }), client.invalidateQueries({ queryKey: ['animal-timeline', id] }), client.invalidateQueries({ queryKey: ['animal-historial-categorias', id] }), client.invalidateQueries({ queryKey: ['animals'] })]); navigate(`/animales/${id}`) } })

  function submit(input: UpdateAnimalInput) {
    const fuenteEdad = animal.origen === 'COMPRADO' ? 'PROVEEDOR' : 'ESTIMACION_CAMPO'
    const esEdadDeclarada = tipoNacimiento === 'ESTIMADA_EDAD'
    const fechaDirecta = tipoNacimiento === 'CONOCIDA' || tipoNacimiento === 'ESTIMADA_FECHA'
    mutation.mutate({
      ...input,
      fechaNacimiento: fechaDirecta ? input.fechaNacimiento : undefined,
      fechaNacimientoEstimada: tipoNacimiento === 'ESTIMADA_FECHA' || esEdadDeclarada,
      fechaIngreso: animal.origen === 'NACIDO' ? (fechaDirecta ? input.fechaNacimiento : nacimientoCalculado) : input.fechaIngreso,
      quitarFechaNacimiento: tipoNacimiento === 'DESCONOCIDA',
      confirmarFechaNacimiento: tipoNacimiento === 'CONOCIDA',
      edadDeclaradaValor: esEdadDeclarada ? input.edadDeclaradaValor : undefined,
      edadDeclaradaUnidad: esEdadDeclarada ? input.edadDeclaradaUnidad : undefined,
      fechaReferenciaEdad: esEdadDeclarada ? referenciaEdad : undefined,
      fuenteEdad: esEdadDeclarada ? fuenteEdad : undefined,
      observacionEstimacion: esEdadDeclarada ? input.observacionEstimacion : undefined,
      categoriaManualMotivo: categoriaManual ? input.categoriaManualMotivo : undefined,
      pesoIngresoEstimado: input.pesoIngresoKg != null ? input.pesoIngresoEstimado : undefined,
    })
  }

  return <div className="page-stack narrow-page"><PageHeader eyebrow="Animales" title={`Editar ${animal.codigo}`} description="Actualiza identificación, características y ubicación." actions={<Button variant="ghost" onClick={() => unsaved.requestLeave(() => navigate(`/animales/${id}`))}><ArrowLeft size={18} aria-hidden="true" />Cancelar</Button>} />
    {mutation.error && <Alert tone="danger">{normalizeApiError(mutation.error).message}</Alert>}
    <Card><form className="form-grid" onSubmit={handleSubmit(submit)}>
      <div className="form-section-title form-full"><h2>Información básica</h2></div>
      <Field label="Código" hint="Identificador interno permanente"><input value={animal.codigo} readOnly /></Field><Field label="Nombre"><input {...register('nombre')} /></Field>
      <Field label="Sexo"><select {...register('sexo')}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select></Field>
      <Field label="Nacimiento"><select value={tipoNacimiento} onChange={(event) => setTipoNacimiento(event.target.value as TipoNacimiento)}><option value="DESCONOCIDA" disabled={animal.origen === 'NACIDO'}>Totalmente desconocido</option><option value="ESTIMADA_FECHA">Fecha estimada</option><option value="ESTIMADA_EDAD">Edad aproximada declarada</option><option value="CONOCIDA">Fecha confirmada</option></select></Field>
      {(tipoNacimiento === 'CONOCIDA' || tipoNacimiento === 'ESTIMADA_FECHA') && <Field label="Fecha de nacimiento" hint={tipoNacimiento === 'ESTIMADA_FECHA' ? 'Se guarda marcada como estimada.' : undefined}><input type="date" max={todayInBolivia()} required {...register('fechaNacimiento')} /></Field>}
      {tipoNacimiento === 'ESTIMADA_EDAD' && <>
        <Field label="Edad aproximada" error={errors.edadDeclaradaValor?.message} hint="Ejemplo: para un año y medio indica 18 meses."><input type="number" min="1" step="1" required {...register('edadDeclaradaValor', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field>
        <Field label="Unidad"><select {...register('edadDeclaradaUnidad')}><option value="DIAS">Días</option><option value="MESES">Meses</option><option value="ANIOS">Años</option></select></Field>
        <Field label="Nacimiento calculado" hint="Se guarda expresamente como fecha estimada."><input value={nacimientoCalculado ?? ''} readOnly placeholder="Se calcula con la edad" /></Field>
        <Field label="Detalle de la estimación"><input {...register('observacionEstimacion')} placeholder={animal.origen === 'COMPRADO' ? 'Dato informado por el proveedor' : 'Criterio usado en campo'} /></Field>
      </>}
      <Field label="Propósito"><select {...register('proposito')}><option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option></select></Field>
      <div className="form-section-title form-full"><h2>Clasificación</h2></div>
      <Field label="Raza"><select {...register('razaPrincipalId')} required>{catalogs.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Categoría" hint={categoriaManual ? 'Clasificación manual: por ejemplo, Buey requiere confirmar castración.' : categoriaAutomatica ? 'Actualizada automáticamente según sexo y edad.' : 'Selecciona manualmente porque no se conoce la edad.'}>{categoriaAutomatica ? [<input key="categoria-visible" value={categoriaAutomatica.nombre} readOnly />, <input key="categoria-valor" type="hidden" {...register('categoriaActualId')} />] : <select {...register('categoriaActualId')} required>{catalogs.categories.filter((item) => item.activo && (item.sexoAplicable === 'AMBOS' || item.sexoAplicable === sexo)).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>}</Field>
      {categoriaManual && <Field label="Motivo de la categoría manual" hint="Queda registrado en el historial de categorías del animal."><input {...register('categoriaManualMotivo')} placeholder="Ej. castración confirmada por el veterinario" /></Field>}
      <div className="form-section-title form-full"><h2>Ubicación</h2></div>
      <Field label="Propiedad"><select {...register('propiedadActualId')} required>{catalogs.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field><Field label="Potrero"><select {...register('potreroActualId')} required>{catalogs.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <div className="form-section-title form-full"><h2>Información adicional</h2></div>
      <Field label="Color"><input {...register('color')} /></Field>{animal.origen === 'NACIDO'
        ? <Field label="Ingreso al hato" hint="Se actualiza automáticamente con la fecha de nacimiento."><input value={(tipoNacimiento === 'CONOCIDA' || tipoNacimiento === 'ESTIMADA_FECHA' ? fechaNacimiento : nacimientoCalculado) ?? ''} readOnly /></Field>
        : <Field label="Fecha de recepción"><input type="date" max={todayInBolivia()} required {...register('fechaIngreso')} /></Field>}
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
async function loadCatalogShape() { const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listAllPotreros()]); return { breeds, categories, properties, paddocks } }
