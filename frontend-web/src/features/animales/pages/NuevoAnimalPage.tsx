import { useEffect, useState } from 'react'
import { todayInBolivia } from '@/shared/utils/date'
import { useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Check, FileText, Info, ListChecks, Save } from 'lucide-react'
import { createAnimalSchema } from '@/features/animales/schema'
import { calcularNacimientoEstimado, categoriaSugerida } from '@/features/animales/edad'
import { createAnimal, listCategorias, listRazas } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
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

const PROPOSITO_LABEL: Record<CreateAnimalInput['proposito'], string> = {
  CARNE: 'Carne',
  LECHE: 'Leche',
  REPRODUCCION: 'Reproducción',
  DOBLE_PROPOSITO: 'Doble propósito',
}

const ORIGEN_LABEL: Record<CreateAnimalInput['origen'], string> = {
  NACIDO: 'Nacido',
  COMPRADO: 'Comprado',
  TRANSFERIDO: 'Transferido',
}

function formatFecha(value?: string) {
  if (!value) return '—'
  const date = new Date(`${value}T12:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('es-BO', { day: '2-digit', month: 'short', year: 'numeric' })
}

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
  const nombre = useWatch({ control, name: 'nombre' })
  const proposito = useWatch({ control, name: 'proposito' })
  const razaPrincipalId = useWatch({ control, name: 'razaPrincipalId' })
  const potreroActualId = useWatch({ control, name: 'potreroActualId' })
  const fechaNacimiento = useWatch({ control, name: 'fechaNacimiento' })
  const fechaIngreso = useWatch({ control, name: 'fechaIngreso' })
  const edadDeclaradaValor = useWatch({ control, name: 'edadDeclaradaValor' })
  const edadDeclaradaUnidad = useWatch({ control, name: 'edadDeclaradaUnidad' })
  const categoriaActualId = useWatch({ control, name: 'categoriaActualId' })
  const color = useWatch({ control, name: 'color' })
  const pesoNacimientoKg = useWatch({ control, name: 'pesoNacimientoKg' })
  const condicionCorporalActual = useWatch({ control, name: 'condicionCorporalActual' })
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
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listAllPotreros()])
    return { breeds, categories, properties, paddocks }
  } })
  const nacimientoClasificacion = tipoNacimiento === 'CONOCIDA' ? fechaNacimiento : nacimientoCalculado
  const categoriaAutomatica = categoriaSugerida(catalogs.data?.categories, sexo, nacimientoClasificacion, todayInBolivia())
  const categoriaAutomaticaId = categoriaAutomatica?.id
  useEffect(() => {
    if (categoriaAutomaticaId) setValue('categoriaActualId', categoriaAutomaticaId, { shouldValidate: true })
  }, [categoriaAutomaticaId, setValue])

  const razaNombre = catalogs.data?.breeds.find((item) => item.id === razaPrincipalId)?.nombre
  const propiedadNombre = catalogs.data?.properties.find((item) => item.id === propertyId)?.nombre
  const potreroNombre = catalogs.data?.paddocks.find((item) => item.id === potreroActualId)?.nombre

  const proveedorDefinido = Boolean(proveedorSeleccion.proveedorId || proveedorSeleccion.proveedorNuevo?.nombre)
  const camposObligatorios = 7 + (origen === 'COMPRADO' ? 2 : 0)
  const camposCompletos = [
    sexo, origen, proposito, razaPrincipalId, categoriaActualId, propertyId, potreroActualId,
    fechaNacimiento || edadDeclaradaValor,
    ...(origen === 'COMPRADO' ? [fechaIngreso, proveedorDefinido] : []),
  ].filter(Boolean).length
  const progreso = Math.min(100, Math.round((camposCompletos / camposObligatorios) * 100))

  const nacimientoParaFicha = tipoNacimiento === 'CONOCIDA' ? fechaNacimiento : nacimientoCalculado
  const nombreParaFicha = nombre?.trim() || 'Sin nombre'

  // color, peso al nacer y condición corporal solo viajan en POST /animales: el alta por
  // compra pasa por CompraDetalleRequest, que no tiene esos campos (ver CompraDetalleRequest.java).
  const datosNacimientoCompraSoportados = origen !== 'COMPRADO'
  const steps = [
    { id: 'paso-identificacion', label: 'Identificación básica', done: Boolean(sexo && (tipoNacimiento === 'CONOCIDA' ? fechaNacimiento : tipoNacimiento === 'EDAD_APROXIMADA' ? edadDeclaradaValor : true)) },
    { id: 'paso-clasificacion', label: 'Clasificación y raza', done: Boolean(proposito && origen && razaPrincipalId && categoriaActualId) },
    { id: 'paso-ubicacion', label: 'Ubicación y potrero', done: Boolean(propertyId && potreroActualId) },
    { id: 'paso-ingreso', label: 'Ingreso y peso', done: origen === 'NACIDO' ? true : Boolean(fechaIngreso) },
    ...(origen === 'COMPRADO' ? [{ id: 'paso-compra', label: 'Datos de la compra', done: proveedorDefinido }] : []),
  ]
  const activeStepIndex = steps.findIndex((step) => !step.done)

  function scrollToStep(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

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
    <div className="page-stack">
      <PageHeader
        eyebrow="Animales"
        title="Registrar animal"
        description="Completa la ficha del animal y guárdala para sumarlo al hato de tu empresa."
        actions={<Button variant="ghost" onClick={() => unsaved.requestLeave(() => navigate('/animales'))}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>}
      />
      {message && <Alert tone={message.tone}>{message.text}</Alert>}
      <form onSubmit={handleSubmit(submit)} noValidate>
        <div className="record-layout">
          <div className="record-main">
            <Card className="record-steps-panel">
              <div className="record-steps-panel-head">
                <span className="record-steps-panel-title"><ListChecks size={16} aria-hidden="true" />Progreso del registro</span>
                <span className="record-steps-panel-pct">{progreso}% completado</span>
              </div>
              <div className="record-progress-track" role="progressbar" aria-valuenow={progreso} aria-valuemin={0} aria-valuemax={100}>
                <span className="record-progress-bar" style={{ width: `${progreso}%` }} />
              </div>
              <ol className="record-steps">
                {steps.map((step, index) => {
                  const state = activeStepIndex === -1 || index < activeStepIndex ? 'is-done' : index === activeStepIndex ? 'is-current' : ''
                  return (
                    <li key={step.id} className={`record-steps-item ${state}`}>
                      <button type="button" onClick={() => scrollToStep(step.id)} aria-current={state === 'is-current' ? 'step' : undefined}>
                        <span className="record-steps-index">{state === 'is-done' ? <Check size={13} aria-hidden="true" /> : index + 1}</span>
                        <span className="record-steps-label">{step.label}</span>
                      </button>
                    </li>
                  )
                })}
              </ol>
            </Card>

            <Card className="record-card" id="paso-identificacion">
              <div className="record-card-head">
                <span className="record-step" aria-hidden="true">01</span>
                <div><h2>Información básica</h2><p>Nombre, sexo y datos de nacimiento del animal.</p></div>
              </div>
              <div className="form-grid">
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
                {datosNacimientoCompraSoportados && <Field label="Peso al nacer (kg)" hint="Opcional. Si no lo conoces, déjalo vacío." error={errors.pesoNacimientoKg?.message}>
                  <input type="number" min="0" step="0.01" {...register('pesoNacimientoKg', { setValueAs: (value) => value === '' ? undefined : Number(value) })} />
                </Field>}
                {datosNacimientoCompraSoportados && <Field label="Color" hint="Opcional, para identificarlo a simple vista." error={errors.color?.message}>
                  <input {...register('color')} placeholder="Ej. Colorado, Overo negro" />
                </Field>}
              </div>
            </Card>

            <Card className="record-card" id="paso-clasificacion">
              <div className="record-card-head">
                <span className="record-step" aria-hidden="true">02</span>
                <div><h2>Clasificación</h2><p>Propósito, origen, raza y categoría del animal.</p></div>
              </div>
              <div className="form-grid">
                <Field label="Propósito" error={errors.proposito?.message}>
                  <select {...register('proposito')}>
                    <option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option>
                  </select>
                </Field>
                <Field label="Origen" error={errors.origen?.message}>
                  <select {...register('origen', { onChange: (event) => {
                    if (event.target.value === 'NACIDO' && tipoNacimiento === 'DESCONOCIDA') setTipoNacimiento('EDAD_APROXIMADA')
                    if (event.target.value !== 'NACIDO' && tipoNacimiento === 'CONOCIDA' && !fechaNacimiento) setTipoNacimiento('DESCONOCIDA')
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
                {!categoriaAutomatica && <Field label="Motivo de la categoría manual" hint="Queda registrado en el historial de categorías del animal." error={errors.categoriaManualMotivo?.message}><input {...register('categoriaManualMotivo')} placeholder="Ej. edad desconocida, criterio del encargado" /></Field>}
              </div>
            </Card>

            <Card className="record-card" id="paso-ubicacion">
              <div className="record-card-head">
                <span className="record-step" aria-hidden="true">03</span>
                <div><h2>Ubicación</h2><p>Propiedad y potrero donde quedará asignado el animal.</p></div>
              </div>
              <div className="form-grid">
                <Field label="Propiedad" error={errors.propiedadActualId?.message}>
                  <select {...register('propiedadActualId')}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
                </Field>
                <Field label="Potrero" error={errors.potreroActualId?.message} hint="Debe pertenecer a la propiedad seleccionada.">
                  <select {...register('potreroActualId')}><option value="">Selecciona…</option>{catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propertyId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
                </Field>
              </div>
            </Card>

            <Card className="record-card" id="paso-ingreso">
              <div className="record-card-head">
                <span className="record-step" aria-hidden="true">04</span>
                <div><h2>Información adicional</h2><p>Ingreso al hato, peso y observaciones complementarias.</p></div>
              </div>
              <div className="form-grid">
                {origen === 'NACIDO'
                  ? <Field label="Ingreso al hato" hint="En animales nacidos en la finca coincide con el nacimiento."><input value={fechaNacimiento ?? ''} readOnly /></Field>
                  : <Field label="Fecha de recepción" error={errors.fechaIngreso?.message}><input type="date" max={todayInBolivia()} defaultValue={todayInBolivia()} required {...register('fechaIngreso')} /></Field>}
                <Field label="Peso al ingreso (kg)" hint="No corresponde al peso al nacer." error={errors.pesoIngresoKg?.message}><input type="number" min="0.001" step="0.001" {...register('pesoIngresoKg', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field>
                <Field label="Tipo de peso al ingreso"><select {...register('pesoIngresoEstimado', { setValueAs: (value) => value === true || value === 'true' })}><option value="true">Estimado</option><option value="false">Medido</option></select></Field>
                {datosNacimientoCompraSoportados && <Field label="Condición corporal (1 a 5)" hint="Escala de condición corporal (BCS). Opcional." error={errors.condicionCorporalActual?.message}>
                  <input type="number" min="1" max="5" step="0.5" {...register('condicionCorporalActual', { setValueAs: (value) => value === '' ? undefined : Number(value) })} />
                </Field>}
                <div className="form-full">
                  <Field label="Observaciones" error={errors.observaciones?.message}>
                    <textarea rows={4} {...register('observaciones')} />
                  </Field>
                </div>
              </div>
            </Card>

            {origen === 'COMPRADO' && <Card className="record-card" id="paso-compra">
              <div className="record-card-head">
                <span className="record-step" aria-hidden="true">05</span>
                <div><h2>Datos de la compra</h2><p>Proveedor y precio pagado por este animal.</p></div>
              </div>
              <div className="form-grid">
                <div className="form-full"><ProveedorPicker value={proveedorSeleccion} onChange={setProveedorSeleccion} /></div>
                <Field label="Precio de compra" hint="Precio pagado por este animal."><input type="number" inputMode="decimal" min="0" step="0.01" value={precioCompra} onChange={(event) => setPrecioCompra(event.target.value)} /></Field>
                <Field label="Moneda"><input value={monedaCompra} onChange={(event) => setMonedaCompra(event.target.value)} maxLength={10} placeholder="BOB" /></Field>
              </div>
            </Card>}
          </div>

          <aside className="record-aside">
            <Card className="record-aside-card">
              <div className="record-aside-head"><span className="record-aside-icon"><FileText size={16} aria-hidden="true" /></span><div><h3>Ficha del registro</h3><p>Vista previa en vivo</p></div></div>
              <dl className="definition-list record-summary">
                <div><dt>Nombre</dt><dd>{nombreParaFicha}</dd></div>
                <div><dt>Sexo</dt><dd>{sexo === 'MACHO' ? 'Macho' : 'Hembra'}</dd></div>
                <div><dt>Origen</dt><dd>{ORIGEN_LABEL[origen ?? 'NACIDO']}</dd></div>
                <div><dt>Propósito</dt><dd>{PROPOSITO_LABEL[proposito ?? 'CARNE']}</dd></div>
                <div><dt>Raza</dt><dd>{razaNombre ?? '—'}</dd></div>
                {color?.trim() && <div><dt>Color</dt><dd>{color}</dd></div>}
                <div><dt>Categoría</dt><dd>{categoriaAutomatica?.nombre ?? 'Por definir'}</dd></div>
                <div><dt>Nacimiento</dt><dd>{formatFecha(nacimientoParaFicha)}</dd></div>
                {pesoNacimientoKg != null && <div><dt>Peso al nacer</dt><dd>{pesoNacimientoKg} kg</dd></div>}
                {condicionCorporalActual != null && <div><dt>Condición corporal</dt><dd>{condicionCorporalActual}</dd></div>}
                <div><dt>Ubicación</dt><dd>{propiedadNombre ? `${propiedadNombre}${potreroNombre ? ` · ${potreroNombre}` : ''}` : '—'}</dd></div>
              </dl>
            </Card>

            <div className="record-help">
              <span className="record-help-title"><Info size={15} aria-hidden="true" />Nota</span>
              <p>La categoría se asigna sola según sexo y edad. Si el origen es una compra, se genera una compra confirmada al guardar.</p>
            </div>
          </aside>
        </div>

        <div className="save-bar">
          <div className="save-hint"><span className="save-hint-dot" aria-hidden="true" />Los campos marcados con * son obligatorios.</div>
          <div className="save-actions">
            <Button type="submit" loading={isSubmitting}><Save size={18} />Guardar animal</Button>
          </div>
        </div>
      </form>
      <UnsavedChangesDialog open={unsaved.open} onStay={unsaved.cancelLeave} onLeave={unsaved.discardAndLeave} />
    </div>
  )
}