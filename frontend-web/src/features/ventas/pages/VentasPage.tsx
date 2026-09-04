import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { listVentas, registrarVenta, type Venta } from '@/features/ventas/api'
import { listAnimals } from '@/features/animales/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { normalizeApiError } from '@/shared/api/errors'

export function VentasPage() {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  const query = useQuery({
    queryKey: ['ventas', { desde, hasta }],
    queryFn: () => listVentas({ desde: desde || undefined, hasta: hasta || undefined }),
  })
  const animales = useQuery({
    queryKey: ['ventas-animales'],
    queryFn: () => listAnimals({ estado: 'ACTIVO', page: 0, size: 500 }),
  })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarVenta({
        animalId: String(data.get('animalId')),
        fechaVenta: String(data.get('fechaVenta') || '') || undefined,
        comprador: String(data.get('comprador')),
        precio: Number(data.get('precio')),
        moneda: String(data.get('moneda') || '') || undefined,
        pesoVentaKg: data.get('pesoVentaKg') ? Number(data.get('pesoVentaKg')) : undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      })
    },
    onSuccess: async (venta) => {
      setShowForm(false)
      await Promise.all([
        client.invalidateQueries({ queryKey: ['ventas'] }),
        client.invalidateQueries({ queryKey: ['ventas-animales'] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
        client.invalidateQueries({ queryKey: ['animal', venta.animalId] }),
        client.invalidateQueries({ queryKey: ['animal-timeline', venta.animalId] }),
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
          <select name="animalId" required disabled={animales.isPending}>
            <option value="">Selecciona…</option>
            {animales.data?.content.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}
          </select>
        </Field>
        <Field label="Fecha de venta"><input name="fechaVenta" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
        <Field label="Comprador" required><input name="comprador" required maxLength={200} /></Field>
        <Field label="Precio" required><input name="precio" type="number" inputMode="decimal" min="0.01" step="0.01" required placeholder="0.00" /></Field>
        <Field label="Moneda" hint="Por defecto BOB."><input name="moneda" maxLength={3} placeholder="BOB" /></Field>
        <Field label="Peso al momento de venta (kg)"><input name="pesoVentaKg" type="number" inputMode="decimal" min="0.1" step="0.1" /></Field>
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
