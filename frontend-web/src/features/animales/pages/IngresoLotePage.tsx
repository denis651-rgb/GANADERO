import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle2, ClipboardPlus, Copy, Plus, Save, Trash2, Wand2 } from 'lucide-react'
import { getAnimal, listCategorias, listRazas } from '@/features/animales/api'
import { calcularNacimientoEstimado, categoriaSugerida } from '@/features/animales/edad'
import type { AnimalSummary, CategoriaAnimal, UnidadEdadDeclarada } from '@/features/animales/types'
import { DeclararHistorialModal } from '@/features/animales/components/DeclararHistorialModal'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
import { createMovimiento, confirmarMovimiento } from '@/features/movimientos/api'
import { confirmarCompra, crearCompra, getCompraDetalles } from '@/features/compras/api'
import { fechaRecepcionInstant, type ModalidadPrecio } from '@/features/compras/types'
import { distribuirPorTropa, totalPorUnidad } from '@/features/compras/pricing'
import { ProveedorPicker } from '@/features/proveedores/components/ProveedorPicker'
import type { ProveedorSeleccion } from '@/features/proveedores/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'
import { todayInBolivia } from '@/shared/utils/date'

interface AnimalRow {
  key: number
  nombre: string
  sexo: 'MACHO' | 'HEMBRA'
  categoriaActualId: string
  pesoIngresoKg: string
  tipoPeso: string
  precioOverride: string
  tipoNacimiento: 'DESCONOCIDA' | 'EDAD_APROXIMADA' | 'CONOCIDA'
  fechaNacimiento: string
  edadDeclaradaValor: string
  edadDeclaradaUnidad: UnidadEdadDeclarada
  observaciones: string
  categoriaManualMotivo: string
}

let rowKey = 0
const filaVacia = (): AnimalRow => ({ key: rowKey++, nombre: '', sexo: 'HEMBRA', categoriaActualId: '', pesoIngresoKg: '', tipoPeso: 'ESTIMADO', precioOverride: '', tipoNacimiento: 'DESCONOCIDA', fechaNacimiento: '', edadDeclaradaValor: '', edadDeclaradaUnidad: 'MESES', observaciones: '', categoriaManualMotivo: '' })

function categoriaDeFila(fila: AnimalRow, categorias: CategoriaAnimal[] | undefined, fechaIngreso: string) {
  const nacimiento = fila.tipoNacimiento === 'CONOCIDA'
    ? fila.fechaNacimiento
    : fila.tipoNacimiento === 'EDAD_APROXIMADA'
      ? calcularNacimientoEstimado(fechaIngreso, fila.edadDeclaradaValor, fila.edadDeclaradaUnidad)
      : undefined
  return categoriaSugerida(categorias, fila.sexo, nacimiento, fechaIngreso)
}

export function IngresoLotePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const hoy = todayInBolivia()

  const [razaPrincipalId, setRazaPrincipalId] = useState('')
  const [proposito, setProposito] = useState<'CARNE' | 'LECHE' | 'REPRODUCCION' | 'DOBLE_PROPOSITO'>('CARNE')
  const [propiedadActualId, setPropiedadActualId] = useState('')
  const [potreroActualId, setPotreroActualId] = useState('')
  const [fechaIngreso, setFechaIngreso] = useState(() => todayInBolivia())
  const [proveedorSeleccion, setProveedorSeleccion] = useState<ProveedorSeleccion>({})
  const [modalidad, setModalidad] = useState<ModalidadPrecio>('POR_UNIDAD')
  const [precioUnitario, setPrecioUnitario] = useState('')
  const [precioTotal, setPrecioTotal] = useState('')
  const [moneda, setMoneda] = useState('BOB')
  const [enviarCuarentena, setEnviarCuarentena] = useState(false)
  const [cuarentenaPotreroId, setCuarentenaPotreroId] = useState('')
  const [filas, setFilas] = useState<AnimalRow[]>(() => [filaVacia()])
  const [cantidadAgregar, setCantidadAgregar] = useState('1')
  const [bulkSexo, setBulkSexo] = useState<'' | 'MACHO' | 'HEMBRA'>('')
  const [bulkPeso, setBulkPeso] = useState('')
  const [bulkTipoPeso, setBulkTipoPeso] = useState('ESTIMADO')
  const [bulkEdadValor, setBulkEdadValor] = useState('')
  const [bulkEdadUnidad, setBulkEdadUnidad] = useState<UnidadEdadDeclarada>('MESES')
  const focoRef = useRef<number | null>(null)
  const [creados, setCreados] = useState<AnimalSummary[] | null>(null)
  const [declarandoPara, setDeclarandoPara] = useState<AnimalSummary | null>(null)
  const [declarados, setDeclarados] = useState<Set<string>>(new Set())
  const [formError, setFormError] = useState<string | null>(null)

  const catalogs = useQuery({
    queryKey: ['animal-form-catalogs'],
    queryFn: async () => {
      const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listAllPotreros()])
      return { breeds, categories, properties, paddocks }
    },
  })
  const potrerosDeLaPropiedad = catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propiedadActualId) ?? []

  const actualizarFila = (key: number, campo: keyof AnimalRow, valor: string) => {
    setFilas((prev) => prev.map((fila) => (fila.key === key ? { ...fila, [campo]: valor } : fila)))
  }

  const hayBulk = Boolean(bulkSexo || bulkPeso || bulkEdadValor)

  function agregarFilas(n: number) {
    const inicio = filas.length + 1
    setFilas((prev) => [...prev, ...Array.from({ length: n }, () => filaVacia())])
    focoRef.current = inicio
  }

  function duplicarFila(key: number) {
    const indice = filas.findIndex((fila) => fila.key === key)
    if (indice < 0) return
    const nueva: AnimalRow = { ...filas[indice], key: rowKey++, nombre: '' }
    setFilas((prev) => {
      const copia = [...prev]
      copia.splice(indice + 1, 0, nueva)
      return copia
    })
    focoRef.current = indice + 2
  }

  function aplicarLote() {
    if (!hayBulk) return
    const cambios: Partial<AnimalRow> = {}
    if (bulkSexo) cambios.sexo = bulkSexo
    if (bulkPeso) {
      cambios.pesoIngresoKg = bulkPeso
      cambios.tipoPeso = bulkTipoPeso
    }
    if (bulkEdadValor) {
      cambios.tipoNacimiento = 'EDAD_APROXIMADA'
      cambios.edadDeclaradaValor = bulkEdadValor
      cambios.edadDeclaradaUnidad = bulkEdadUnidad
    }
    setFilas((prev) => prev.map((fila) => ({ ...fila, ...cambios })))
  }

  useEffect(() => {
    if (focoRef.current == null) return
    const target = document.querySelector<HTMLInputElement>(`input[aria-label="Nombre del animal ${focoRef.current}"]`)
    target?.focus()
    focoRef.current = null
  }, [filas])

  const overrides = filas.map((fila) => fila.precioOverride ? Number(fila.precioOverride) : undefined)
  const algunOverride = overrides.some((value) => value != null)
  const todosConOverride = overrides.every((value) => value != null)
  const totalCalculado = modalidad === 'POR_UNIDAD'
    ? totalPorUnidad(Number(precioUnitario) || 0, overrides)
    : (algunOverride ? overrides.reduce((sum: number, value) => sum + (value ?? 0), 0) : Number(precioTotal) || 0)
  const distribucionPorTropa = modalidad === 'POR_TROPA' && !algunOverride ? distribuirPorTropa(Number(precioTotal) || 0, filas.length) : null

  const ingresar = useMutation({
    mutationFn: async () => {
      if (!proveedorSeleccion.proveedorId && !proveedorSeleccion.proveedorNuevo?.nombre) {
        throw new Error('Selecciona o registra el proveedor de la compra.')
      }
      const borrador = await crearCompra({
        proveedorId: proveedorSeleccion.proveedorId,
        proveedorNuevo: proveedorSeleccion.proveedorNuevo,
        fechaRecepcion: fechaRecepcionInstant(fechaIngreso),
        modalidad,
        moneda: moneda || 'BOB',
        precioUnitario: modalidad === 'POR_UNIDAD' ? Number(precioUnitario) || 0 : undefined,
        precioTotal: modalidad === 'POR_TROPA' ? Number(precioTotal) || 0 : undefined,
        propiedadId: propiedadActualId,
        potreroId: potreroActualId,
        proposito,
        detalles: filas.map((fila) => ({
          nombre: fila.nombre || undefined,
          sexo: fila.sexo,
          razaId: razaPrincipalId,
          proposito,
          categoriaActualId: categoriaDeFila(fila, catalogs.data?.categories, fechaIngreso)?.id ?? fila.categoriaActualId,
          categoriaManualMotivo: categoriaDeFila(fila, catalogs.data?.categories, fechaIngreso) ? undefined : (fila.categoriaManualMotivo || undefined),
          pesoIngresoKg: fila.pesoIngresoKg ? Number(fila.pesoIngresoKg) : undefined,
          tipoPeso: fila.pesoIngresoKg ? (fila.tipoPeso === 'ESTIMADO' ? 'ESTIMADO' : 'MEDIDO') : undefined,
          fechaNacimiento: fila.tipoNacimiento === 'CONOCIDA' ? fila.fechaNacimiento || undefined : undefined,
          fechaNacimientoEstimada: false,
          edadDeclaradaValor: fila.tipoNacimiento === 'EDAD_APROXIMADA' && fila.edadDeclaradaValor ? Number(fila.edadDeclaradaValor) : undefined,
          edadDeclaradaUnidad: fila.tipoNacimiento === 'EDAD_APROXIMADA' ? fila.edadDeclaradaUnidad : undefined,
          fechaReferenciaEdad: fila.tipoNacimiento === 'EDAD_APROXIMADA' ? fechaIngreso : undefined,
          fuenteEdadDeclarada: fila.tipoNacimiento === 'EDAD_APROXIMADA' ? 'PROVEEDOR' : undefined,
          precioOverride: fila.precioOverride ? Number(fila.precioOverride) : undefined,
          observaciones: fila.observaciones || undefined,
        })),
      })

      const confirmada = await confirmarCompra(borrador.id, borrador.version)
      const detalles = await getCompraDetalles(confirmada.id)
      const nuevos = await Promise.all(detalles.map((detalle) => getAnimal(detalle.animalId!)))

      if (enviarCuarentena) {
        const cuarentena = await createMovimiento({
          tipo: 'CUARENTENA',
          destinoPotreroId: cuarentenaPotreroId,
          animales: nuevos.map((animal) => ({ animalId: animal.id, version: animal.version })),
        })
        await confirmarMovimiento(cuarentena.id, cuarentena.version)
      }

      return nuevos
    },
    onSuccess: (nuevos) => {
      setCreados(nuevos)
      void queryClient.invalidateQueries({ queryKey: ['animals'] })
      void queryClient.invalidateQueries({ queryKey: ['compras'] })
    },
  })

  function submit() {
    setFormError(null)
    if (modalidad === 'POR_TROPA' && algunOverride && !todosConOverride) {
      setFormError('En una compra por tropa con ajustes manuales, todos los animales deben tener un precio asignado.')
      return
    }
    ingresar.mutate()
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Animales" title="Ingreso por lote de compra" description="Registra varios animales comprados juntos, con un solo ingreso de datos comunes."
      actions={<Button variant="ghost" onClick={() => navigate('/animales')}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>} />

    {!creados && <>
      {formError && <Alert tone="danger">{formError}</Alert>}
      {ingresar.error && <Alert tone="danger">{normalizeApiError(ingresar.error).message}</Alert>}
      <Card>
        <div className="form-section-title"><h2>Datos comunes del lote</h2></div>
        <form className="form-grid" onSubmit={(event) => { event.preventDefault(); submit() }}>
          <Field label="Raza" required><select required value={razaPrincipalId} onChange={(event) => setRazaPrincipalId(event.target.value)}><option value="">Selecciona…</option>{catalogs.data?.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Propósito" required><select value={proposito} onChange={(event) => setProposito(event.target.value as typeof proposito)}><option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option></select></Field>
          <Field label="Propiedad" required><select required value={propiedadActualId} onChange={(event) => { setPropiedadActualId(event.target.value); setPotreroActualId(''); setCuarentenaPotreroId('') }}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Potrero" required hint="Debe pertenecer a la propiedad seleccionada."><select required value={potreroActualId} onChange={(event) => setPotreroActualId(event.target.value)}><option value="">Selecciona…</option>{potrerosDeLaPropiedad.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Fecha de ingreso" required><input type="date" required max={hoy} value={fechaIngreso} onChange={(event) => setFechaIngreso(event.target.value)} /></Field>

          <div className="form-section-title form-full"><h2>Proveedor y precios</h2></div>
          <ProveedorPicker value={proveedorSeleccion} onChange={setProveedorSeleccion} />
          <Field label="Modalidad de precio"><select value={modalidad} onChange={(event) => setModalidad(event.target.value as ModalidadPrecio)}><option value="POR_UNIDAD">Por unidad</option><option value="POR_TROPA">Por tropa o punta</option></select></Field>
          <Field label="Moneda"><input value={moneda} onChange={(event) => setMoneda(event.target.value)} maxLength={10} placeholder="BOB" /></Field>
          {modalidad === 'POR_UNIDAD'
            ? <Field label="Precio unitario" hint="Se aplica a todos los animales del lote, salvo ajuste manual por fila."><input type="number" inputMode="decimal" min="0" step="0.01" value={precioUnitario} onChange={(event) => setPrecioUnitario(event.target.value)} /></Field>
            : <Field label="Precio total del lote" hint="Se reparte entre todos los animales; puedes ajustar el precio por fila."><input type="number" inputMode="decimal" min="0" step="0.01" value={precioTotal} onChange={(event) => setPrecioTotal(event.target.value)} /></Field>}
          <Field label="Total calculado"><input value={totalCalculado.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} readOnly /></Field>

          <div className="form-full"><div className="section-heading ingreso-lote-heading"><div><h3>Animales del lote <span className="count-badge">{filas.length}</span></h3><p className="muted">Completa los datos particulares de cada animal. La raza, propiedad y potrero se toman de los datos comunes.</p></div>
          <div className="agregar-filas">
            <input aria-label="Cantidad de animales a agregar" type="number" min="1" max="50" step="1" value={cantidadAgregar} onChange={(event) => setCantidadAgregar(event.target.value)} />
            <Button type="button" variant="secondary" onClick={() => agregarFilas(Math.min(50, Math.max(1, Number(cantidadAgregar) || 1)))}><Plus size={16} aria-hidden="true" />Agregar</Button>
            <div className="agregar-filas-quick">{[1, 5, 10, 20].map((n) => <button key={n} type="button" aria-label={`Agregar ${n} animales`} onClick={() => agregarFilas(n)}>+{n}</button>)}</div>
          </div>
        </div></div>

        <div className="form-full ingreso-lote-toolbar">
          <span className="ingreso-lote-toolbar-label">Aplicar a todas las filas:</span>
          <select aria-label="Sexo a aplicar en lote" value={bulkSexo} onChange={(event) => setBulkSexo(event.target.value as '' | 'MACHO' | 'HEMBRA')}><option value="">Sexo —</option><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select>
          <input aria-label="Peso a aplicar en lote (kg)" type="number" min="0.1" step="0.1" placeholder="Peso (kg)" value={bulkPeso} onChange={(event) => setBulkPeso(event.target.value)} />
          <select aria-label="Tipo de peso a aplicar en lote" value={bulkTipoPeso} onChange={(event) => setBulkTipoPeso(event.target.value)}><option value="ESTIMADO">Estimado</option><option value="MEDIDO">Medido</option></select>
          <input aria-label="Edad a aplicar en lote" type="number" min="1" step="1" placeholder="Edad" value={bulkEdadValor} onChange={(event) => setBulkEdadValor(event.target.value)} />
          <select aria-label="Unidad de edad a aplicar en lote" value={bulkEdadUnidad} onChange={(event) => setBulkEdadUnidad(event.target.value as UnidadEdadDeclarada)}><option value="DIAS">Días</option><option value="MESES">Meses</option><option value="ANIOS">Años</option></select>
          <Button type="button" variant="secondary" onClick={aplicarLote} disabled={!hayBulk}><Wand2 size={16} aria-hidden="true" />Aplicar</Button>
        </div>

          <div className="form-full ingreso-lote-editor">
            <div className="ingreso-lote-tip"><strong>Edad del animal</strong><span>Si el proveedor informa una edad aproximada, regístrala como tal. El sistema calculará el nacimiento estimado desde la fecha de recepción y conservará el dato declarado.</span></div>
            <div className="table-wrapper ingreso-lote-table-wrapper" role="region" aria-label="Tabla editable de animales" tabIndex={0}>
              <table className="ingreso-lote-table">
                <caption className="visually-hidden">Animales del lote</caption>
                <thead><tr>
                  <th scope="col">#</th><th scope="col">Nombre</th>
                  <th scope="col">Sexo *</th><th scope="col">Categoría *</th><th scope="col">Peso al ingreso (kg)</th>
                  <th scope="col">Nacimiento o edad</th><th scope="col">Precio (ajuste)</th><th scope="col">Observaciones</th><th scope="col">Acciones</th>
                </tr></thead>
                <tbody>{filas.map((fila, index) => {
                  const categoriaAutomatica = categoriaDeFila(fila, catalogs.data?.categories, fechaIngreso)
                  return <tr key={fila.key}>
                  <th scope="row"><span className="row-number">{index + 1}</span></th>
                  <td className="animal-name-cell"><input aria-label={`Nombre del animal ${index + 1}`} placeholder="Nombre o identificación…" value={fila.nombre} onChange={(event) => actualizarFila(fila.key, 'nombre', event.target.value)} maxLength={160} /></td>
                  <td className="animal-sex-cell"><select aria-label={`Sexo del animal ${index + 1}`} required value={fila.sexo} onChange={(event) => actualizarFila(fila.key, 'sexo', event.target.value)}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select></td>
                  <td className="animal-category-cell">{categoriaAutomatica
                    ? <><input aria-label={`Categoría del animal ${index + 1}`} value={categoriaAutomatica.nombre} readOnly /><small>Automática por sexo y edad</small></>
                    : <><select aria-label={`Categoría del animal ${index + 1}`} required value={fila.categoriaActualId} onChange={(event) => actualizarFila(fila.key, 'categoriaActualId', event.target.value)}><option value="">Selecciona…</option>{catalogs.data?.categories.filter((item) => item.activo && (item.sexoAplicable === 'AMBOS' || item.sexoAplicable === fila.sexo)).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select><input aria-label={`Motivo de la categoría manual del animal ${index + 1}`} placeholder="Motivo de la categoría manual" value={fila.categoriaManualMotivo} onChange={(event) => actualizarFila(fila.key, 'categoriaManualMotivo', event.target.value)} /></>}</td>
                  <td className="animal-weight-cell"><input aria-label={`Peso al ingreso (kg) del animal ${index + 1}`} placeholder="0,0" type="number" inputMode="decimal" min="0.1" step="0.1" value={fila.pesoIngresoKg} onChange={(event) => actualizarFila(fila.key, 'pesoIngresoKg', event.target.value)} /><select aria-label={`Tipo de peso del animal ${index + 1}`} value={fila.tipoPeso} onChange={(event) => actualizarFila(fila.key, 'tipoPeso', event.target.value)}><option value="ESTIMADO">Estimado</option><option value="MEDIDO">Medido</option></select></td>
                  <td className="animal-age-cell"><select aria-label={`Nacimiento del animal ${index + 1}`} value={fila.tipoNacimiento} onChange={(event) => actualizarFila(fila.key, 'tipoNacimiento', event.target.value)}><option value="DESCONOCIDA">Totalmente desconocido</option><option value="EDAD_APROXIMADA">Edad aproximada</option><option value="CONOCIDA">Fecha conocida</option></select>
                    {fila.tipoNacimiento === 'CONOCIDA' && <input required aria-label={`Fecha de nacimiento del animal ${index + 1}`} type="date" max={fechaIngreso || hoy} value={fila.fechaNacimiento} onChange={(event) => actualizarFila(fila.key, 'fechaNacimiento', event.target.value)} />}
                    {fila.tipoNacimiento === 'EDAD_APROXIMADA' && <><input required aria-label={`Edad aproximada del animal ${index + 1}`} type="number" min="1" step="1" placeholder="Ej. 18" value={fila.edadDeclaradaValor} onChange={(event) => actualizarFila(fila.key, 'edadDeclaradaValor', event.target.value)} /><select aria-label={`Unidad de edad del animal ${index + 1}`} value={fila.edadDeclaradaUnidad} onChange={(event) => actualizarFila(fila.key, 'edadDeclaradaUnidad', event.target.value)}><option value="DIAS">Días</option><option value="MESES">Meses</option><option value="ANIOS">Años</option></select><small>Nacimiento estimado: {calcularNacimientoEstimado(fechaIngreso, fila.edadDeclaradaValor, fila.edadDeclaradaUnidad) ?? '—'}</small></>}
                  </td>
                  <td className="animal-price-cell"><input aria-label={`Precio del animal ${index + 1}`} type="number" inputMode="decimal" min="0" step="0.01" placeholder={distribucionPorTropa ? distribucionPorTropa[index]?.toFixed(2) : 'Sin ajuste'} value={fila.precioOverride} onChange={(event) => actualizarFila(fila.key, 'precioOverride', event.target.value)} /><small>{moneda || 'BOB'} · opcional</small></td>
                  <td className="animal-notes-cell"><input aria-label={`Observaciones del animal ${index + 1}`} placeholder="Observación opcional…" value={fila.observaciones} onChange={(event) => actualizarFila(fila.key, 'observaciones', event.target.value)} maxLength={500} /></td>
                  <td className="animal-actions-cell"><Button type="button" variant="ghost" aria-label={`Duplicar fila ${index + 1}`} title="Duplicar animal" onClick={() => duplicarFila(fila.key)}><Copy size={18} aria-hidden="true" /></Button><Button type="button" variant="ghost" aria-label={`Quitar fila ${index + 1}`} title="Quitar animal" disabled={filas.length === 1} onClick={() => setFilas((prev) => prev.filter((item) => item.key !== fila.key))}><Trash2 size={18} aria-hidden="true" /></Button></td>
                </tr>})}</tbody>
              </table>
            </div>
          </div>

          <div className="form-full">
            <label className="checkbox-line"><input type="checkbox" checked={enviarCuarentena} onChange={(event) => setEnviarCuarentena(event.target.checked)} /> Enviar a cuarentena</label>
          </div>
          {enviarCuarentena && <Field label="Potrero de cuarentena" required hint="Puede ser el mismo potrero de ingreso."><select required value={cuarentenaPotreroId} onChange={(event) => setCuarentenaPotreroId(event.target.value)}><option value="">Selecciona…</option>{potrerosDeLaPropiedad.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}

          <div className="form-full form-actions">
            <Button type="submit" loading={ingresar.isPending}><Save size={18} aria-hidden="true" />Registrar lote</Button>
          </div>
        </form>
      </Card>
    </>}

    {creados && <Card>
      <div className="section-heading"><div className="title-with-icon"><CheckCircle2 size={20} aria-hidden="true" /><h2>{creados.length} animal(es) registrado(s)</h2></div>{creados.length > 0 ? <Button variant="secondary" onClick={() => navigate('/animales/declarar-historial', { state: { animales: creados } })}><ClipboardPlus size={17} aria-hidden="true" />Declarar historial grupal</Button> : null}</div>
      {creados.length === 0
        ? <EmptyState title="Sin animales" description="El lote no generó animales." />
        : <div className="table-wrapper"><table><caption className="visually-hidden">Animales recién registrados</caption><thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Historial sanitario</th></tr></thead><tbody>{creados.map((animal) => <tr key={animal.id}>
            <td><strong>{animal.codigo}</strong></td>
            <td>{animal.nombre || '—'}</td>
            <td>{declarados.has(animal.id)
              ? <span className="status-badge status-activo">Declarado</span>
              : <Button variant="secondary" onClick={() => setDeclarandoPara(animal)}><ClipboardPlus size={16} aria-hidden="true" />Declarar historial sanitario</Button>}</td>
          </tr>)}</tbody></table></div>}
      <div className="form-actions"><Button onClick={() => navigate('/animales')}>Ir a Animales</Button></div>
    </Card>}

    {declarandoPara && <DeclararHistorialModal
      animalId={declarandoPara.id}
      animalLabel={declarandoPara.nombre ? `${declarandoPara.codigo} · ${declarandoPara.nombre}` : declarandoPara.codigo}
      onClose={() => setDeclarandoPara(null)}
      onSuccess={() => { setDeclarados((prev) => new Set(prev).add(declarandoPara.id)); setDeclarandoPara(null) }}
    />}
  </div>
}
