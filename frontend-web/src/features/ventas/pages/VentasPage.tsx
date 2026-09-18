import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useSearchParams } from 'react-router'
import { listVentas, registrarVenta, registrarVentaLote, type ModalidadVenta, type Venta } from '@/features/ventas/api'
import { AnimalMultiPicker } from '@/features/ventas/components/AnimalMultiPicker'
import { AnimalPicker } from '@/features/ventas/components/AnimalPicker'
import { listAnimals } from '@/features/animales/api'
import { getPesajeHistory } from '@/features/pesajes/api'
import type { Pesaje } from '@/features/pesajes/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { formatDate } from '@/shared/utils/date'
import { normalizeApiError } from '@/shared/api/errors'

/** Sin un campo de configuración dedicado a antigüedad de peso en venta (a diferencia de la tolerancia de compra). */
const UMBRAL_DIAS_ANTIGUEDAD_PESO = 30

const modalidadLabel: Record<ModalidadVenta, string> = { EN_PIE: 'En pie', CARNEADO: 'Carneado' }

function diasDeAntiguedad(fecha: string): number {
  return Math.floor((Date.now() - new Date(`${fecha.slice(0, 10)}T00:00:00`).getTime()) / 86_400_000)
}

const invalidacionesVenta = (client: ReturnType<typeof useQueryClient>) => Promise.all([
  client.invalidateQueries({ queryKey: ['ventas'] }),
  client.invalidateQueries({ queryKey: ['ventas-animales'] }),
  client.invalidateQueries({ queryKey: ['animals'] }),
  client.invalidateQueries({ queryKey: ['pesaje-history'] }),
  client.invalidateQueries({ queryKey: ['lote'] }),
  client.invalidateQueries({ queryKey: ['lotes'] }),
  client.invalidateQueries({ queryKey: ['lote-miembros'] }),
  client.invalidateQueries({ queryKey: ['lote-animales-disponibles'] }),
])

export function VentasPage() {
  const client = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const filtroAnimalId = searchParams.get('animalId') || ''
  const quitarFiltroAnimal = () => setSearchParams((params) => { params.delete('animalId'); return params }, { replace: true })
  const [showForm, setShowForm] = useState(false)
  const [modo, setModo] = useState<'individual' | 'lote'>('individual')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  // --- Venta individual ---
  const [animalId, setAnimalIdRaw] = useState('')
  const [modoPesoManual, setModoPesoManual] = useState<'existente' | 'nuevo' | null>(null)
  const [pesajeSeleccionadoId, setPesajeSeleccionadoId] = useState<string | null>(null)
  const [pesoNuevo, setPesoNuevo] = useState('')
  const [tipoPesoNuevo, setTipoPesoNuevo] = useState<'MEDIDO' | 'ESTIMADO'>('MEDIDO')

  function setAnimalId(id: string) {
    setAnimalIdRaw(id)
    setModoPesoManual(null)
    setPesajeSeleccionadoId(null)
  }

  // --- Venta por lote ---
  const [animalesSel, setAnimalesSel] = useState<Set<string>>(new Set())
  const [modalidad, setModalidad] = useState<ModalidadVenta>('EN_PIE')
  const [precioCabeza, setPrecioCabeza] = useState('')
  const [precioKg, setPrecioKg] = useState('')
  const [pesosLote, setPesosLote] = useState<Record<string, string>>({})

  const query = useQuery({
    queryKey: ['ventas', { desde, hasta, animalId: filtroAnimalId }],
    queryFn: () => listVentas({ animalId: filtroAnimalId || undefined, desde: desde || undefined, hasta: hasta || undefined }),
  })
  const animales = useQuery({
    queryKey: ['ventas-animales'],
    queryFn: () => listAnimals({ estado: 'ACTIVO', page: 0, size: 500 }),
  })
  const historialPesos = useQuery({
    queryKey: ['pesaje-history', animalId],
    queryFn: () => getPesajeHistory(animalId),
    enabled: Boolean(animalId),
  })
  const activos = (historialPesos.data ?? []).filter((p) => p.estado === 'ACTIVO')
  const ultimoMedido = [...activos].filter((p) => p.tipoPeso === 'MEDIDO').sort((a, b) => b.fecha.localeCompare(a.fecha))[0]
  const ultimoEstimado = [...activos].filter((p) => p.tipoPeso === 'ESTIMADO').sort((a, b) => b.fecha.localeCompare(a.fecha))[0]
  const opcionesExistentes = [ultimoMedido, ultimoEstimado].filter((p): p is Pesaje => Boolean(p))
  const pesajeElegido = opcionesExistentes.find((p) => p.id === pesajeSeleccionadoId) ?? opcionesExistentes[0]
  const modoPeso: 'existente' | 'nuevo' = opcionesExistentes.length === 0 ? 'nuevo' : (modoPesoManual ?? 'existente')

  const idsLote = useMemo(() => Array.from(animalesSel), [animalesSel])
  const historialesLote = useQueries({
    queries: idsLote.map((id) => ({ queryKey: ['pesaje-history', id], queryFn: () => getPesajeHistory(id) })),
  })

  // Prellena el peso de cada animal recién seleccionado con su último pesaje activo, sin pisar ediciones manuales.
  useEffect(() => {
    idsLote.forEach((id, index) => {
      const historial = historialesLote[index]?.data
      if (!historial) return
      setPesosLote((prev) => {
        if (prev[id] !== undefined) return prev
        const activo = [...historial].filter((p) => p.estado === 'ACTIVO').sort((a, b) => b.fecha.localeCompare(a.fecha))[0]
        return activo ? { ...prev, [id]: String(activo.pesoKg) } : prev
      })
    })
  }, [historialesLote, idsLote])

  const animalLabel = (id: string) => {
    const animal = animales.data?.content.find((item) => item.id === id)
    return animal ? (animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo) : id.slice(0, 8)
  }

  const pesosFaltantes = modalidad === 'CARNEADO'
    && idsLote.some((id) => !pesosLote[id] || Number(pesosLote[id]) <= 0)
  const montoAnimal = (id: string) => modalidad === 'EN_PIE'
    ? Number(precioCabeza) || 0
    : (Number(precioKg) || 0) * (Number(pesosLote[id]) || 0)
  const montoTotal = idsLote.reduce((acc, id) => acc + montoAnimal(id), 0)

  function limpiarFormularioLote() {
    setAnimalesSel(new Set())
    setModalidad('EN_PIE')
    setPrecioCabeza('')
    setPrecioKg('')
    setPesosLote({})
  }

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarVenta({
        animalId,
        fechaVenta: String(data.get('fechaVenta') || '') || undefined,
        comprador: String(data.get('comprador')),
        telefonoComprador: String(data.get('telefonoComprador') || '') || undefined,
        precio: Number(data.get('precio')),
        moneda: String(data.get('moneda') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        pesajeExistenteId: modoPeso === 'existente' ? pesajeElegido?.id : undefined,
        pesoVentaKg: modoPeso === 'nuevo' && pesoNuevo ? Number(pesoNuevo) : undefined,
        tipoPeso: modoPeso === 'nuevo' ? tipoPesoNuevo : undefined,
        dispositivo: modoPeso === 'nuevo' ? 'WEB' : undefined,
      })
    },
    onSuccess: async (venta) => {
      setShowForm(false)
      setAnimalId('')
      setPesoNuevo('')
      await invalidacionesVenta(client)
      client.invalidateQueries({ queryKey: ['animal', venta.animalId] })
      client.invalidateQueries({ queryKey: ['animal-timeline', venta.animalId] })
    },
  })

  const crearLote = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarVentaLote({
        animalIds: idsLote,
        fechaVenta: String(data.get('fechaVentaLote') || '') || undefined,
        comprador: String(data.get('compradorLote')),
        telefonoComprador: String(data.get('telefonoCompradorLote') || '') || undefined,
        modalidad,
        precioCabeza: modalidad === 'EN_PIE' ? Number(precioCabeza) : undefined,
        precioKg: modalidad === 'CARNEADO' ? Number(precioKg) : undefined,
        pesosVentaKg: Object.fromEntries(idsLote.filter((id) => pesosLote[id]).map((id) => [id, Number(pesosLote[id])])),
        observaciones: String(data.get('observacionesLote') || '') || undefined,
      })
    },
    onSuccess: async () => {
      setShowForm(false)
      limpiarFormularioLote()
      await invalidacionesVenta(client)
    },
  })

  const error = query.error ?? animales.error ?? crear.error ?? crearLote.error

  return <div className="page-stack">
    <PageHeader
      eyebrow="Comercial"
      title="Ventas"
      description="Registro de ventas de animales y su historial de precios."
      actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} aria-hidden="true" />Registrar venta</Button>}
    />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

    {showForm && <Card>
      <div className="filter-heading">
        <span>Tipo de venta</span>
        <Button type="button" variant={modo === 'individual' ? 'primary' : 'secondary'} onClick={() => setModo('individual')}>Un animal</Button>
        <Button type="button" variant={modo === 'lote' ? 'primary' : 'secondary'} onClick={() => setModo('lote')}>Varios animales / lote</Button>
      </div>

      {modo === 'individual' && <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalPicker animales={animales.data?.content ?? []} cargando={animales.isPending} value={animalId} onChange={setAnimalId} />
        <Field label="Fecha de venta"><input name="fechaVenta" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
        <Field label="Comprador" required><input name="comprador" required maxLength={200} /></Field>
        <Field label="Teléfono del comprador"><input name="telefonoComprador" type="tel" maxLength={30} placeholder="Ej. 76543210…" /></Field>
        <Field label="Precio" required><input name="precio" type="number" inputMode="decimal" min="0.01" step="0.01" required placeholder="0.00" /></Field>
        <Field label="Moneda" hint="Por defecto BOB."><input name="moneda" maxLength={3} placeholder="BOB" /></Field>

        {animalId && <div className="form-full">
          <div className="section-heading"><h4>Peso de salida</h4></div>
          {historialPesos.isPending && <LoadingState message="Buscando el último peso registrado…" />}
          {!historialPesos.isPending && opcionesExistentes.length > 0 && <>
            <label className="checkbox-line"><input type="radio" name="modoPeso" checked={modoPeso === 'existente'} onChange={() => setModoPesoManual('existente')} /> Usar un peso ya registrado</label>
            {modoPeso === 'existente' && <div className="form-grid">
              <Field label="Peso a usar">
                <select value={pesajeElegido?.id ?? ''} onChange={(event) => setPesajeSeleccionadoId(event.target.value)}>
                  {ultimoMedido && <option value={ultimoMedido.id}>Medido: {ultimoMedido.pesoKg} kg · {formatDate(ultimoMedido.fecha)}</option>}
                  {ultimoEstimado && <option value={ultimoEstimado.id}>Estimado: {ultimoEstimado.pesoKg} kg · {formatDate(ultimoEstimado.fecha)}</option>}
                </select>
              </Field>
              {pesajeElegido && diasDeAntiguedad(pesajeElegido.fecha) > UMBRAL_DIAS_ANTIGUEDAD_PESO && <div className="form-full"><Alert tone="warning">Este peso tiene {diasDeAntiguedad(pesajeElegido.fecha)} días de antigüedad; considera registrar uno nuevo.</Alert></div>}
            </div>}
            <label className="checkbox-line"><input type="radio" name="modoPeso" checked={modoPeso === 'nuevo'} onChange={() => setModoPesoManual('nuevo')} /> Registrar un peso nuevo</label>
          </>}
          {!historialPesos.isPending && opcionesExistentes.length === 0 && <p className="muted">Este animal no tiene pesos previos activos; registra el peso de salida.</p>}
          {modoPeso === 'nuevo' && <div className="form-grid">
            <Field label="Peso de salida (kg)"><input type="number" inputMode="decimal" min="0.1" step="0.1" value={pesoNuevo} onChange={(event) => setPesoNuevo(event.target.value)} /></Field>
            <Field label="Tipo de peso"><select value={tipoPesoNuevo} onChange={(event) => setTipoPesoNuevo(event.target.value as 'MEDIDO' | 'ESTIMADO')}><option value="MEDIDO">Medido</option><option value="ESTIMADO">Estimado</option></select></Field>
          </div>}
        </div>}

        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending} disabled={!animalId}>Guardar venta</Button></div>
      </form>}

      {modo === 'lote' && <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crearLote.mutate(event.currentTarget) }}>
        <AnimalMultiPicker animales={animales.data?.content ?? []} cargando={animales.isPending} seleccionados={animalesSel} onChange={setAnimalesSel} />

        <Field label="Comprador" required><input name="compradorLote" required maxLength={200} /></Field>
        <Field label="Teléfono del comprador"><input name="telefonoCompradorLote" type="tel" maxLength={30} placeholder="Ej. 76543210…" /></Field>
        <Field label="Fecha de venta"><input name="fechaVentaLote" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>

        <div className="form-full">
          <div className="section-heading"><h4>Modalidad de venta</h4></div>
          <label className="checkbox-line"><input type="radio" name="modalidad" checked={modalidad === 'EN_PIE'} onChange={() => setModalidad('EN_PIE')} /> En pie — un precio por cabeza para todos</label>
          <label className="checkbox-line"><input type="radio" name="modalidad" checked={modalidad === 'CARNEADO'} onChange={() => setModalidad('CARNEADO')} /> Carneado — precio por kilo según el peso de cada animal</label>
        </div>
        {modalidad === 'EN_PIE'
          ? <Field label="Precio por cabeza" required><input type="number" inputMode="decimal" min="0.01" step="0.01" value={precioCabeza} onChange={(event) => setPrecioCabeza(event.target.value)} placeholder="0.00" /></Field>
          : <Field label="Precio por kilo" required><input type="number" inputMode="decimal" min="0.01" step="0.01" value={precioKg} onChange={(event) => setPrecioKg(event.target.value)} placeholder="0.00" /></Field>}

        {idsLote.length > 0 && <div className="form-full">
          <div className="section-heading"><h4>Peso y monto por animal</h4></div>
          <div className="table-wrapper"><table><thead><tr>
            <th scope="col">Animal</th><th scope="col">Peso de salida (kg)</th><th scope="col">Monto</th>
          </tr></thead><tbody>{idsLote.map((id) => <tr key={id}>
            <td>{animalLabel(id)}</td>
            <td><input type="number" inputMode="decimal" min="0.1" step="0.1" value={pesosLote[id] ?? ''}
              onChange={(event) => setPesosLote((prev) => ({ ...prev, [id]: event.target.value }))}
              aria-label={`Peso de salida de ${animalLabel(id)}`} /></td>
            <td>{montoAnimal(id).toLocaleString('es-BO', { style: 'currency', currency: 'BOB' })}</td>
          </tr>)}</tbody><tfoot><tr><td colSpan={2}><strong>Total</strong></td><td><strong>{montoTotal.toLocaleString('es-BO', { style: 'currency', currency: 'BOB' })}</strong></td></tr></tfoot></table></div>
          {pesosFaltantes && <Alert tone="warning">En modalidad carneado, todos los animales necesitan un peso de salida mayor a cero.</Alert>}
        </div>}

        <div className="form-full"><Field label="Observaciones"><textarea name="observacionesLote" rows={2} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crearLote.isPending} disabled={idsLote.length === 0 || pesosFaltantes}>Guardar venta de {idsLote.length || ''} animal(es)</Button></div>
      </form>}
    </Card>}

    <Card>
      <div className="filter-heading">
        <span>Filtros</span>
        <input aria-label="Desde" type="date" value={desde} onChange={(event) => setDesde(event.target.value)} />
        <input aria-label="Hasta" type="date" value={hasta} onChange={(event) => setHasta(event.target.value)} />
        {filtroAnimalId && <>
          <span>Animal: {animalLabel(filtroAnimalId)}</span>
          <Button type="button" variant="ghost" onClick={quitarFiltroAnimal}>Quitar filtro</Button>
        </>}
      </div>
      {query.isPending && <LoadingState message="Cargando ventas…" />}
      {query.data?.length === 0 && <EmptyState title="Sin ventas registradas" description="Registra la primera venta de un animal." />}
      {query.data && query.data.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Ventas registradas</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Animal</th><th scope="col">Comprador</th><th scope="col">Teléfono</th><th scope="col">Modalidad</th><th scope="col">Precio</th><th scope="col">Peso (kg)</th></tr></thead><tbody>{query.data.map((venta: Venta) => <tr key={venta.id}>
          <td>{formatDate(venta.fechaVenta)}</td>
          <td>{animalLabel(venta.animalId)}</td>
          <td>{venta.comprador}</td>
          <td>{venta.telefonoComprador ?? '—'}</td>
          <td>{modalidadLabel[venta.modalidad] ?? venta.modalidad}</td>
          <td>{venta.precio.toLocaleString('es-BO', { style: 'currency', currency: venta.moneda || 'BOB' })}</td>
          <td>{venta.pesoVentaKg ?? '—'}</td>
        </tr>)}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{query.data.map((venta: Venta) => <MobileEntityCard
          key={venta.id}
          title={animalLabel(venta.animalId)}
          subtitle={venta.comprador}
          metadata={<><span>{formatDate(venta.fechaVenta)}</span><span>{venta.precio.toLocaleString('es-BO', { style: 'currency', currency: venta.moneda || 'BOB' })}</span></>}
        />)}</div></div>
      </>}
    </Card>
  </div>
}
