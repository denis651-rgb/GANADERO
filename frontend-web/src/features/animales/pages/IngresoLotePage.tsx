import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle2, ClipboardPlus, Plus, Save, Trash2 } from 'lucide-react'
import { createAnimalesLote, getAnimal, listCategorias, listRazas } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { DeclararHistorialModal } from '@/features/animales/components/DeclararHistorialModal'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { createMovimiento, confirmarMovimiento } from '@/features/movimientos/api'
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
  tipoNacimiento: string
  fechaNacimiento: string
  observaciones: string
}

let rowKey = 0
const filaVacia = (): AnimalRow => ({ key: rowKey++, nombre: '', sexo: 'HEMBRA', categoriaActualId: '', pesoIngresoKg: '', tipoPeso: 'ESTIMADO', tipoNacimiento: 'DESCONOCIDA', fechaNacimiento: '', observaciones: '' })

export function IngresoLotePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const hoy = todayInBolivia()

  const [razaPrincipalId, setRazaPrincipalId] = useState('')
  const [proposito, setProposito] = useState<'CARNE' | 'LECHE' | 'REPRODUCCION' | 'DOBLE_PROPOSITO'>('CARNE')
  const [propiedadActualId, setPropiedadActualId] = useState('')
  const [potreroActualId, setPotreroActualId] = useState('')
  const [fechaIngreso, setFechaIngreso] = useState(() => todayInBolivia())
  const [precioAdquisicion, setPrecioAdquisicion] = useState('')
  const [proveedor, setProveedor] = useState('')
  const [enviarCuarentena, setEnviarCuarentena] = useState(false)
  const [cuarentenaPotreroId, setCuarentenaPotreroId] = useState('')
  const [filas, setFilas] = useState<AnimalRow[]>(() => [filaVacia()])
  const [creados, setCreados] = useState<AnimalSummary[] | null>(null)
  const [declarandoPara, setDeclarandoPara] = useState<AnimalSummary | null>(null)
  const [declarados, setDeclarados] = useState<Set<string>>(new Set())

  const catalogs = useQuery({
    queryKey: ['animal-form-catalogs'],
    queryFn: async () => {
      const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listPotreros()])
      return { breeds, categories, properties, paddocks }
    },
  })
  const potrerosDeLaPropiedad = catalogs.data?.paddocks.filter((item) => item.activo && item.propiedadId === propiedadActualId) ?? []

  const actualizarFila = (key: number, campo: keyof AnimalRow, valor: string) => {
    setFilas((prev) => prev.map((fila) => (fila.key === key ? { ...fila, [campo]: valor } : fila)))
  }

  const ingresar = useMutation({
    mutationFn: async () => {
      const nuevos = await createAnimalesLote({
        razaPrincipalId,
        proposito,
        propiedadActualId,
        potreroActualId,
        fechaIngreso: fechaIngreso || undefined,
        precioAdquisicion: precioAdquisicion ? Number(precioAdquisicion) : undefined,
        animales: filas.map((fila) => ({
          nombre: fila.nombre || undefined,
          sexo: fila.sexo,
          categoriaActualId: fila.categoriaActualId,
          pesoIngresoKg: fila.pesoIngresoKg ? Number(fila.pesoIngresoKg) : undefined,
          pesoIngresoEstimado: fila.pesoIngresoKg ? fila.tipoPeso === 'ESTIMADO' : undefined,
          fechaNacimiento: fila.tipoNacimiento === 'DESCONOCIDA' ? undefined : fila.fechaNacimiento || undefined,
          fechaNacimientoEstimada: fila.tipoNacimiento === 'ESTIMADA' && Boolean(fila.fechaNacimiento),
          observaciones: [proveedor ? `Proveedor: ${proveedor}` : null, fila.observaciones || null].filter(Boolean).join(' — ') || undefined,
        })),
      })

      const ingreso = await createMovimiento({
        tipo: 'INGRESO_COMPRA',
        destinoPropiedadId: propiedadActualId,
        destinoPotreroId: potreroActualId,
        animales: nuevos.map((animal) => ({ animalId: animal.id, version: animal.version })),
      })
      await confirmarMovimiento(ingreso.id, ingreso.version)

      if (enviarCuarentena) {
        const actualizados = await Promise.all(nuevos.map((animal) => getAnimal(animal.id)))
        const cuarentena = await createMovimiento({
          tipo: 'CUARENTENA',
          destinoPotreroId: cuarentenaPotreroId,
          animales: actualizados.map((animal) => ({ animalId: animal.id, version: animal.version })),
        })
        await confirmarMovimiento(cuarentena.id, cuarentena.version)
      }

      return nuevos
    },
    onSuccess: (nuevos) => {
      setCreados(nuevos)
      void queryClient.invalidateQueries({ queryKey: ['animals'] })
    },
  })

  return <div className="page-stack">
    <PageHeader eyebrow="Animales" title="Ingreso por lote de compra" description="Registra varios animales comprados juntos, con un solo ingreso de datos comunes."
      actions={<Button variant="ghost" onClick={() => navigate('/animales')}><ArrowLeft size={18} aria-hidden="true" />Volver</Button>} />

    {!creados && <>
      {ingresar.error && <Alert tone="danger">{normalizeApiError(ingresar.error).message}</Alert>}
      <Card>
        <div className="form-section-title"><h2>Datos comunes del lote</h2></div>
        <form className="form-grid" onSubmit={(event) => { event.preventDefault(); ingresar.mutate() }}>
          <Field label="Raza" required><select required value={razaPrincipalId} onChange={(event) => setRazaPrincipalId(event.target.value)}><option value="">Selecciona…</option>{catalogs.data?.breeds.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Propósito" required><select value={proposito} onChange={(event) => setProposito(event.target.value as typeof proposito)}><option value="CARNE">Carne</option><option value="LECHE">Leche</option><option value="REPRODUCCION">Reproducción</option><option value="DOBLE_PROPOSITO">Doble propósito</option></select></Field>
          <Field label="Propiedad" required><select required value={propiedadActualId} onChange={(event) => { setPropiedadActualId(event.target.value); setPotreroActualId(''); setCuarentenaPotreroId('') }}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Potrero" required hint="Debe pertenecer a la propiedad seleccionada."><select required value={potreroActualId} onChange={(event) => setPotreroActualId(event.target.value)}><option value="">Selecciona…</option>{potrerosDeLaPropiedad.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          <Field label="Proveedor" hint="Se anota en las observaciones de cada animal."><input value={proveedor} onChange={(event) => setProveedor(event.target.value)} placeholder="Estancia El Roble" maxLength={160} /></Field>
          <Field label="Fecha de ingreso" required><input type="date" required max={hoy} value={fechaIngreso} onChange={(event) => setFechaIngreso(event.target.value)} /></Field>
          <Field label="Precio unitario" hint="Se aplica a todos los animales del lote."><input type="number" inputMode="decimal" min="0" step="0.01" value={precioAdquisicion} onChange={(event) => setPrecioAdquisicion(event.target.value)} /></Field>

          <div className="form-full"><div className="section-heading"><h3>Animales del lote ({filas.length})</h3><Button type="button" variant="secondary" onClick={() => setFilas((prev) => [...prev, filaVacia()])}><Plus size={16} aria-hidden="true" />Agregar fila</Button></div></div>

          <div className="form-full ingreso-lote-editor">
            <p className="muted">El código se asigna automáticamente al registrar. El peso corresponde al ingreso, no al nacimiento. Si desconoces el nacimiento, no inventes una fecha.</p>
            <div className="table-wrapper ingreso-lote-table-wrapper" role="region" aria-label="Tabla editable de animales" tabIndex={0}>
              <table className="ingreso-lote-table">
                <caption className="visually-hidden">Animales del lote</caption>
                <thead><tr>
                  <th scope="col">#</th><th scope="col">Nombre</th>
                  <th scope="col">Sexo *</th><th scope="col">Categoría *</th><th scope="col">Peso al ingreso (kg)</th>
                  <th scope="col">Fecha de nacimiento</th><th scope="col">Observaciones</th><th scope="col">Acciones</th>
                </tr></thead>
                <tbody>{filas.map((fila, index) => <tr key={fila.key}>
                  <th scope="row">{index + 1}</th>
                  <td><input aria-label={`Nombre del animal ${index + 1}`} value={fila.nombre} onChange={(event) => actualizarFila(fila.key, 'nombre', event.target.value)} maxLength={160} /></td>
                  <td><select aria-label={`Sexo del animal ${index + 1}`} required value={fila.sexo} onChange={(event) => actualizarFila(fila.key, 'sexo', event.target.value)}><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option></select></td>
                  <td><select aria-label={`Categoría del animal ${index + 1}`} required value={fila.categoriaActualId} onChange={(event) => actualizarFila(fila.key, 'categoriaActualId', event.target.value)}><option value="">Selecciona…</option>{catalogs.data?.categories.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></td>
                  <td><input aria-label={`Peso al ingreso (kg) del animal ${index + 1}`} type="number" inputMode="decimal" min="0.1" step="0.1" value={fila.pesoIngresoKg} onChange={(event) => actualizarFila(fila.key, 'pesoIngresoKg', event.target.value)} /><select aria-label={`Tipo de peso del animal ${index + 1}`} value={fila.tipoPeso} onChange={(event) => actualizarFila(fila.key, 'tipoPeso', event.target.value)}><option value="ESTIMADO">Estimado</option><option value="MEDIDO">Medido</option></select></td>
                  <td><select aria-label={`Nacimiento del animal ${index + 1}`} value={fila.tipoNacimiento} onChange={(event) => actualizarFila(fila.key, 'tipoNacimiento', event.target.value)}><option value="DESCONOCIDA">Desconocido</option><option value="ESTIMADA">Fecha estimada</option><option value="CONOCIDA">Fecha conocida</option></select><input disabled={fila.tipoNacimiento === 'DESCONOCIDA'} required={fila.tipoNacimiento !== 'DESCONOCIDA'} aria-label={`Fecha de nacimiento del animal ${index + 1}`} type="date" max={hoy} value={fila.fechaNacimiento} onChange={(event) => actualizarFila(fila.key, 'fechaNacimiento', event.target.value)} /></td>
                  <td><input aria-label={`Observaciones del animal ${index + 1}`} value={fila.observaciones} onChange={(event) => actualizarFila(fila.key, 'observaciones', event.target.value)} maxLength={500} /></td>
                  <td><Button type="button" variant="ghost" aria-label={`Quitar fila ${index + 1}`} title="Quitar fila" disabled={filas.length === 1} onClick={() => setFilas((prev) => prev.filter((item) => item.key !== fila.key))}><Trash2 size={16} aria-hidden="true" /></Button></td>
                </tr>)}</tbody>
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
      <div className="section-heading"><CheckCircle2 size={20} aria-hidden="true" /><h2>{creados.length} animal(es) registrado(s)</h2></div>
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
