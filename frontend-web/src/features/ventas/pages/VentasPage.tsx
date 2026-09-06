import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { listVentas, registrarVenta, type Venta } from '@/features/ventas/api'
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

function diasDeAntiguedad(fecha: string): number {
  return Math.floor((Date.now() - new Date(`${fecha.slice(0, 10)}T00:00:00`).getTime()) / 86_400_000)
}

export function VentasPage() {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
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

  const query = useQuery({
    queryKey: ['ventas', { desde, hasta }],
    queryFn: () => listVentas({ desde: desde || undefined, hasta: hasta || undefined }),
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

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarVenta({
        animalId,
        fechaVenta: String(data.get('fechaVenta') || '') || undefined,
        comprador: String(data.get('comprador')),
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
      await Promise.all([
        client.invalidateQueries({ queryKey: ['ventas'] }),
        client.invalidateQueries({ queryKey: ['ventas-animales'] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
        client.invalidateQueries({ queryKey: ['animal', venta.animalId] }),
        client.invalidateQueries({ queryKey: ['animal-timeline', venta.animalId] }),
        client.invalidateQueries({ queryKey: ['pesaje-history', venta.animalId] }),
        client.invalidateQueries({ queryKey: ['lote'] }),
        client.invalidateQueries({ queryKey: ['lotes'] }),
        client.invalidateQueries({ queryKey: ['lote-miembros'] }),
        client.invalidateQueries({ queryKey: ['lote-animales-disponibles'] }),
      ])
    },
  })

  const animalLabel = (id: string) => {
    const animal = animales.data?.content.find((item) => item.id === id)
    return animal ? (animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo) : id.slice(0, 8)
  }

  const error = query.error ?? animales.error ?? crear.error

  return <div className="page-stack">
    <PageHeader
      eyebrow="Comercial"
      title="Ventas"
      description="Registro de ventas de animales y su historial de precios."
      actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} aria-hidden="true" />Registrar venta</Button>}
    />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

    {showForm && <Card>
      <h3>Registrar venta</h3>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required>
          <select value={animalId} onChange={(event) => setAnimalId(event.target.value)} required disabled={animales.isPending}>
            <option value="">Selecciona…</option>
            {animales.data?.content.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}
          </select>
        </Field>
        <Field label="Fecha de venta"><input name="fechaVenta" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
        <Field label="Comprador" required><input name="comprador" required maxLength={200} /></Field>
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
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Guardar venta</Button></div>
      </form>
    </Card>}

    <Card>
      <div className="filter-heading">
        <span>Filtros</span>
        <input aria-label="Desde" type="date" value={desde} onChange={(event) => setDesde(event.target.value)} />
        <input aria-label="Hasta" type="date" value={hasta} onChange={(event) => setHasta(event.target.value)} />
      </div>
      {query.isPending && <LoadingState message="Cargando ventas…" />}
      {query.data?.length === 0 && <EmptyState title="Sin ventas registradas" description="Registra la primera venta de un animal." />}
      {query.data && query.data.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Ventas registradas</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Animal</th><th scope="col">Comprador</th><th scope="col">Precio</th><th scope="col">Peso (kg)</th></tr></thead><tbody>{query.data.map((venta: Venta) => <tr key={venta.id}>
          <td>{new Date(venta.fechaVenta).toLocaleDateString('es-BO')}</td>
          <td>{animalLabel(venta.animalId)}</td>
          <td>{venta.comprador}</td>
          <td>{venta.precio.toLocaleString('es-BO', { style: 'currency', currency: venta.moneda || 'BOB' })}</td>
          <td>{venta.pesoVentaKg ?? '—'}</td>
        </tr>)}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{query.data.map((venta: Venta) => <MobileEntityCard
          key={venta.id}
          title={animalLabel(venta.animalId)}
          subtitle={venta.comprador}
          metadata={<><span>{new Date(venta.fechaVenta).toLocaleDateString('es-BO')}</span><span>{venta.precio.toLocaleString('es-BO', { style: 'currency', currency: venta.moneda || 'BOB' })}</span></>}
        />)}</div></div>
      </>}
    </Card>
  </div>
}
