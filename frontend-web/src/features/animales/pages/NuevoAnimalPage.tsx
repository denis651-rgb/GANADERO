import { useEffect, useState } from 'react'
import { todayInBolivia } from '@/shared/utils/date'
import { useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Save } from 'lucide-react'
import { createAnimalSchema } from '@/features/animales/schema'
import { calcularNacimientoEstimado, categoriaSugerida } from '@/features/animales/edad'
import { createAnimal, listCategorias, listRazas } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import type { AnimalSummary, CreateAnimalInput } from '@/features/animales/types'
import type { Page } from '@/shared/api/types'
import { crearCompra, confirmarCompra, getCompraDetalles } from '@/features/compras/api'
import { fechaRecepcionInstant } from '@/features/compras/types'
import { ProveedorPicker } from '@/features/proveedores/components/ProveedorPicker'
import type { ProveedorSeleccion } from '@/features/proveedores/types'
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
  const { register, handleSubmit, control, setValue, formState: { errors, isSubmitting, isDirty } } = useForm<CreateAnimalInput>({
    resolver: zodResolver(createAnimalSchema),
    shouldFocusError: true,
    defaultValues: {
      sexo: 'HEMBRA',
      proposito: 'CARNE',
      origen: 'NACIDO',
      fechaIngreso: todayInBolivia(),
      pesoIngresoEstimado: true,
      edadDeclaradaUnidad: 'MESES',
    },
  })
  const [tipoNacimiento, setTipoNacimiento] = useState<'CONOCIDA' | 'EDAD_APROXIMADA' | 'DESCONOCIDA'>('CONOCIDA')
  const [proveedorSeleccion, setProveedorSeleccion] = useState<ProveedorSeleccion>({})
  const [precioCompra, setPrecioCompra] = useState('')
  const [monedaCompra, setMonedaCompra] = useState('BOB')
  const unsaved = useUnsavedChanges(isDirty || tipoNacimiento !== 'CONOCIDA')
  const propertyId = useWatch({ control, name: 'propiedadActualId' })
  const sexo = useWatch({ control, name: 'sexo' })
  const origen = useWatch({ control, name: 'origen' })
  const fechaNacimiento = useWatch({ control, name: 'fechaNacimiento' })
  const fechaIngreso = useWatch({ control, name: 'fechaIngreso' })
  const edadDeclaradaValor = useWatch({ control, name: 'edadDeclaradaValor' })
  const edadDeclaradaUnidad = useWatch({ control, name: 'edadDeclaradaUnidad' })
  const referenciaEdad = origen === 'NACIDO' ? todayInBolivia() : fechaIngreso
  const nacimientoCalculado = calcularNacimientoEstimado(referenciaEdad, edadDeclaradaValor, edadDeclaradaUnidad)
  useEffect(() => {
    if (tipoNacimiento !== 'EDAD_APROXIMADA') {
      setValue('fechaReferenciaEdad', undefined)
      setValue('fuenteEdad', undefined)
      return
    }
    setValue('fechaReferenciaEdad', referenciaEdad)
    setValue('fuenteEdad', origen === 'COMPRADO' ? 'PROVEEDOR' : 'ESTIMACION_CAMPO')
  }, [origen, referenciaEdad, setValue, tipoNacimiento])
  const catalogs = useQuery({ queryKey: ['animal-form-catalogs'], queryFn: async () => {
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
    return { breeds, categories, properties, paddocks }
  } })
  const nacimientoClasificacion = tipoNacimiento === 'CONOCIDA' ? fechaNacimiento : nacimientoCalculado
  const categoriaAutomatica = categoriaSugerida(catalogs.data?.categories, sexo, nacimientoClasificacion, todayInBolivia())
  const categoriaAutomaticaId = categoriaAutomatica?.id
  useEffect(() => {
    if (categoriaAutomaticaId) setValue('categoriaActualId', categoriaAutomaticaId, { shouldValidate: true })
  }, [categoriaAutomaticaId, setValue])

  async function submit(values: CreateAnimalInput) {
    setMessage(null)
    try {
      if (tipoNacimiento === 'CONOCIDA' && !values.fechaNacimiento) {
        setMessage({ tone: 'danger', text: 'Indica la fecha de nacimiento.' })
        return
      }
      if (tipoNacimiento === 'EDAD_APROXIMADA' && !values.edadDeclaradaValor) {
        setMessage({ tone: 'danger', text: 'Indica la edad aproximada del animal.' })
        return
      }
      const common: Pick<CreateAnimalInput, 'fechaNacimiento' | 'fechaNacimientoEstimada' | 'edadDeclaradaValor' | 'edadDeclaradaUnidad' | 'fechaReferenciaEdad' | 'fuenteEdad' | 'observacionEstimacion'> = {
        fechaNacimiento: tipoNacimiento === 'CONOCIDA' ? values.fechaNacimiento : undefined,
        fechaNacimientoEstimada: false,
        edadDeclaradaValor: tipoNacimiento === 'EDAD_APROXIMADA' ? values.edadDeclaradaValor : undefined,
        edadDeclaradaUnidad: tipoNacimiento === 'EDAD_APROXIMADA' ? values.edadDeclaradaUnidad : undefined,
        fechaReferenciaEdad: tipoNacimiento === 'EDAD_APROXIMADA' ? (values.origen === 'NACIDO' ? todayInBolivia() : values.fechaIngreso) : undefined,
        fuenteEdad: tipoNacimiento === 'EDAD_APROXIMADA' ? (values.origen === 'COMPRADO' ? 'PROVEEDOR' : 'ESTIMACION_CAMPO') : undefined,
        observacionEstimacion: tipoNacimiento === 'EDAD_APROXIMADA' ? values.observacionEstimacion : undefined,
      }

      if (values.origen === 'COMPRADO') {
        if (!proveedorSeleccion.proveedorId && !proveedorSeleccion.proveedorNuevo?.nombre) {
          setMessage({ tone: 'danger', text: 'Selecciona o registra el proveedor de la compra.' })
          return
        }
        if (!values.fechaIngreso) {
          setMessage({ tone: 'danger', text: 'Indica la fecha de recepción.' })
          return
        }
        const borrador = await crearCompra({
          proveedorId: proveedorSeleccion.proveedorId,
          proveedorNuevo: proveedorSeleccion.proveedorNuevo,
          fechaRecepcion: fechaRecepcionInstant(values.fechaIngreso),
          modalidad: 'POR_UNIDAD',
          moneda: monedaCompra || 'BOB',
          precioUnitario: precioCompra ? Number(precioCompra) : 0,
          propiedadId: values.propiedadActualId,
          potreroId: values.potreroActualId,
          proposito: values.proposito,
          observaciones: values.observaciones,
          detalles: [{
            nombre: values.nombre,
            sexo: values.sexo,
            razaId: values.razaPrincipalId,
            proposito: values.proposito,
            categoriaActualId: values.categoriaActualId,
            categoriaManualMotivo: values.categoriaManualMotivo,
            pesoIngresoKg: values.pesoIngresoKg,
            tipoPeso: values.pesoIngresoKg != null ? (values.pesoIngresoEstimado ? 'ESTIMADO' : 'MEDIDO') : undefined,
            observaciones: values.observaciones,
            fechaNacimiento: common.fechaNacimiento,
            fechaNacimientoEstimada: common.fechaNacimientoEstimada,
            edadDeclaradaValor: common.edadDeclaradaValor,
            edadDeclaradaUnidad: common.edadDeclaradaUnidad,
            fechaReferenciaEdad: common.fechaReferenciaEdad,
            fuenteEdadDeclarada: common.fuenteEdad,
            observacionEstimacion: common.observacionEstimacion,
          }],
        })
        const confirmada = await confirmarCompra(borrador.id, borrador.version)
        const detalles = await getCompraDetalles(confirmada.id)
        const animalId = detalles[0]?.animalId
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['animals'] }),
          queryClient.invalidateQueries({ queryKey: ['compras'] }),
        ])
        navigate(animalId ? `/animales/${animalId}` : '/animales')
        return
      }

      const created = await createAnimal({ ...values,
        ...common,
        fechaIngreso: values.origen === 'NACIDO' ? values.fechaNacimiento : values.fechaIngreso,
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
          <Field label="Nacimiento"><select value={tipoNacimiento} onChange={(event) => setTipoNacimiento(event.target.value as typeof tipoNacimiento)}><option value="CONOCIDA">Fecha conocida</option><option value="EDAD_APROXIMADA">Edad aproximada</option><option value="DESCONOCIDA" disabled={origen === 'NACIDO'}>Totalmente desconocido</option></select></Field>
          <Field label="Fecha de nacimiento" error={errors.fechaNacimiento?.message}>
            <input type="date" max={todayInBolivia()} disabled={tipoNacimiento !== 'CONOCIDA'} required={tipoNacimiento === 'CONOCIDA'} {...register('fechaNacimiento')} />
          </Field>
          {tipoNacimiento === 'EDAD_APROXIMADA' && <>
            <Field label="Edad aproximada" error={errors.edadDeclaradaValor?.message} hint="Ejemplo: para un año y medio indica 18 meses."><input type="number" min="1" step="1" required {...register('edadDeclaradaValor', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field>
            <Field label="Unidad"><select {...register('edadDeclaradaUnidad')}><option value="DIAS">Días</option><option value="MESES">Meses</option><option value="ANIOS">Años</option></select></Field>
            <Field label="Nacimiento calculado" hint="Se guardará expresamente como fecha estimada."><input value={nacimientoCalculado ?? ''} readOnly placeholder="Se calcula con la edad" /></Field>
            <Field label="Detalle de la estimación"><input {...register('observacionEstimacion')} placeholder={origen === 'COMPRADO' ? 'Dato informado por el proveedor' : 'Criterio usado en campo'} /></Field>
          </>}
          <div className="form-section-title form-full"><h2>Clasificación</h2></div>
          <Field label="Propósito" error={errors.proposito?.message}>
            <select {...register('proposito')}>
              <option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option>
            </select>
          </Field>
          <Field label="Origen" error={errors.origen?.message}>
            <select {...register('origen', { onChange: (event) => {
              if (event.target.value === 'NACIDO' && tipoNacimiento === 'DESCONOCIDA') setTipoNacimiento('EDAD_APROXIMADA')
              if (event.target.value !== 'NACIDO' && !fechaNacimiento) setTipoNacimiento('DESCONOCIDA')
            } })}><option value="NACIDO">Nacido</option><option value="COMPRADO">Comprado</option><option value="TRANSFERIDO">Transferido</option></select>
          </Field>
          <Field label="Raza" error={errors.razaPrincipalId?.message}>
            <select {...register('razaPrincipalId')}><option value="">Selecciona…</option>{catalogs.data?.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <Field label="Categoría" error={errors.categoriaActualId?.message} hint={categoriaAutomatica ? `Asignada automáticamente por sexo y edad (${categoriaAutomatica.edadMinMeses ?? 0}${categoriaAutomatica.edadMaxMeses == null ? '+ meses' : `–${categoriaAutomatica.edadMaxMeses} meses`}).` : 'Si la edad es desconocida, selecciona la categoría manualmente.'}>
            {categoriaAutomatica
              ? [<input key="categoria-visible" value={categoriaAutomatica.nombre} readOnly />, <input key="categoria-valor" type="hidden" {...register('categoriaActualId')} />]
              : <select {...register('categoriaActualId')}><option value="">Selecciona…</option>{catalogs.data?.categories.filter((item) => item.activo && (item.sexoAplicable === 'AMBOS' || item.sexoAplicable === sexo)).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>}
          </Field>
          {!categoriaAutomatica && <Field label="Motivo de la categoría manual" hint="Queda registrado en el historial de categorías del animal."><input {...register('categoriaManualMotivo')} placeholder="Ej. edad desconocida, criterio del encargado" /></Field>}
          <div className="form-section-title form-full"><h2>Ubicación</h2></div>
          <Field label="Propiedad" error={errors.propiedadActualId?.message}>
            <select {...register('propiedadActualId')}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <Field label="Potrero" error={errors.potreroActualId?.message} hint="Debe pertenecer a la propiedad seleccionada.">
            <select {...register('potreroActualId')}><option value="">Selecciona…</option>{catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          </Field>
          <div className="form-section-title form-full"><h2>Información adicional</h2></div>
          {origen === 'NACIDO'
            ? <Field label="Ingreso al hato" hint="En animales nacidos en la finca coincide con el nacimiento."><input value={fechaNacimiento ?? ''} readOnly /></Field>
            : <Field label="Fecha de recepción" error={errors.fechaIngreso?.message}><input type="date" max={todayInBolivia()} defaultValue={todayInBolivia()} required {...register('fechaIngreso')} /></Field>}
          <Field label="Peso al ingreso (kg)" hint="No corresponde al peso al nacer." error={errors.pesoIngresoKg?.message}><input type="number" min="0.001" step="0.001" {...register('pesoIngresoKg', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field>
          <Field label="Tipo de peso al ingreso"><select {...register('pesoIngresoEstimado', { setValueAs: (value) => value === true || value === 'true' })}><option value="true">Estimado</option><option value="false">Medido</option></select></Field>
          <div className="form-full">
            <Field label="Observaciones" error={errors.observaciones?.message}>
              <textarea rows={4} {...register('observaciones')} />
            </Field>
          </div>
          {origen === 'COMPRADO' && <>
            <div className="form-section-title form-full"><h2>Datos de la compra</h2></div>
            <ProveedorPicker value={proveedorSeleccion} onChange={setProveedorSeleccion} />
            <Field label="Precio de compra" hint="Precio pagado por este animal."><input type="number" inputMode="decimal" min="0" step="0.01" value={precioCompra} onChange={(event) => setPrecioCompra(event.target.value)} /></Field>
            <Field label="Moneda"><input value={monedaCompra} onChange={(event) => setMonedaCompra(event.target.value)} maxLength={10} placeholder="BOB" /></Field>
          </>}
          <div className="form-full form-actions">
            <Button type="submit" loading={isSubmitting}><Save size={18} />Guardar animal</Button>
          </div>
        </form>
      </Card>
      <UnsavedChangesDialog open={unsaved.open} onStay={unsaved.cancelLeave} onLeave={unsaved.discardAndLeave} />
    </div>
  )
}
