import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Plus, Trash2 } from 'lucide-react'
import { addAnimales, cerrarLote, getLote, listMembresias, retirarAnimales } from '@/features/lotes/api'
import { listAnimals } from '@/features/animales/api'
import { listPropiedades } from '@/features/propiedades/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function LoteDetailPage() {
  const { id = '' } = useParams()
  const client = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const lote = useQuery({ queryKey: ['lote', id], queryFn: () => getLote(id), enabled: Boolean(id) })
  const miembros = useQuery({ queryKey: ['lote-miembros', id, true], queryFn: () => listMembresias(id, true), enabled: Boolean(id) })
  const historicos = useQuery({ queryKey: ['lote-miembros', id, false], queryFn: () => listMembresias(id, false), enabled: Boolean(id) })
  const catalogs = useQuery({ queryKey: ['lote-catalogs'], queryFn: async () => {
    const [propiedades, animales] = await Promise.all([listPropiedades(), listAnimals({ search: undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 500 })])
    return { propiedades, animales: animales.content }
  } })
  const add = useMutation({
    mutationFn: (form: HTMLFormElement) => addAnimales(id, Array.from(form.querySelectorAll<HTMLInputElement>('input[name="animales"]:checked')).map((input) => input.value)),
    onSuccess: async () => { setShowAdd(false); await client.invalidateQueries({ queryKey: ['lote-miembros', id] }); await client.invalidateQueries({ queryKey: ['animals'] }) },
  })
  const remove = useMutation({
    mutationFn: (animalIds: string[]) => {
      const motivo = window.prompt('Motivo de la salida') ?? ''
      if (!motivo.trim()) return Promise.reject(new Error('Motivo requerido'))
      return retirarAnimales(id, animalIds, motivo)
    },
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['lote-miembros', id] }); await client.invalidateQueries({ queryKey: ['animals'] }) },
  })
  const close = useMutation({
    mutationFn: () => { if (!window.confirm('¿Cerrar este lote?')) return Promise.reject(new Error('Cancelado')); return cerrarLote(id, lote.data!.version) },
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['lote', id] }); await client.invalidateQueries({ queryKey: ['lotes'] }) },
  })
  const error = lote.error ?? miembros.error ?? catalogs.error ?? add.error ?? remove.error ?? close.error

  const disponibles = useMemo(() => {
    const asignados = new Set(miembros.data?.map((item) => item.animalId) ?? [])
    return catalogs.data?.animales.filter((animal) => !asignados.has(animal.id)) ?? []
  }, [catalogs.data, miembros.data])

  if (lote.isPending) return <LoadingState message="Cargando lote…" />
  if (!lote.data) return <Alert tone="danger">No se encontró el lote solicitado.</Alert>
  const value = lote.data

  return <div className="page-stack">
    <PageHeader eyebrow="Lotes" title={`${value.codigo} · ${value.nombre}`} description={value.descripcion || 'Sin descripción.'} actions={<><Link to="/lotes"><Button variant="ghost"><ArrowLeft size={18} />Volver</Button></Link>{value.estado === 'ABIERTO' && <Button variant="danger" loading={close.isPending} onClick={() => close.mutate()}>Cerrar lote</Button>}</>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <div className="two-column-grid">
      <Card><dl className="detail-list"><div><dt>Propiedad</dt><dd>{catalogs.data?.propiedades.find((item) => item.id === value.propiedadId)?.nombre ?? '—'}</dd></div><div><dt>Estado</dt><dd>{value.estado}</dd></div><div><dt>Apertura</dt><dd>{new Date(value.fechaApertura).toLocaleDateString('es-BO')}</dd></div><div><dt>Cierre</dt><dd>{value.fechaCierre ? new Date(value.fechaCierre).toLocaleDateString('es-BO') : '—'}</dd></div></dl></Card>
      <Card><h3>Miembros activos</h3>{miembros.isPending && <LoadingState message="Cargando animales…" />}{miembros.data && <p className="muted">{miembros.data.length} animal(es) en el lote.</p>}</Card>
    </div>
    <Card>
      <div className="filter-heading"><span><Plus size={18} />Animales del lote</span>{value.estado === 'ABIERTO' && <Button variant="secondary" onClick={() => setShowAdd((current) => !current)}>{showAdd ? 'Cancelar' : 'Agregar animales'}</Button>}</div>
      {showAdd && <form className="compact-form" onSubmit={(event) => { event.preventDefault(); add.mutate(event.currentTarget) }}>
        {disponibles.length === 0 && <EmptyState title="Sin animales disponibles" description="Todos los animales activos ya pertenecen a un lote o no existen animales activos." />}
        {disponibles.length > 0 && <div className="checkbox-stack">{disponibles.map((animal) => <label key={animal.id}><input type="checkbox" name="animales" value={animal.id} /> {animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</label>)}</div>}
        {disponibles.length > 0 && <div className="form-actions"><Button type="submit" loading={add.isPending}>Agregar seleccionados</Button></div>}
      </form>}
      {miembros.isPending && <LoadingState message="Cargando miembros…" />}
      {miembros.data?.length === 0 && !showAdd && <EmptyState title="Lote vacío" description="Agrega animales al lote para empezar a trabajar con ellos." />}
      {miembros.data && miembros.data.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Animal</th><th>Ingreso</th><th></th></tr></thead><tbody>{miembros.data.map((item) => {
        const animal = catalogs.data?.animales.find((a) => a.id === item.animalId)
        return <tr key={item.id}><td><strong>{animal?.codigo ?? '—'}</strong>{animal?.nombre ? ` · ${animal.nombre}` : ''}</td><td>{new Date(item.fechaIngreso).toLocaleString('es-BO')}</td><td>{value.estado === 'ABIERTO' && <Button variant="danger" onClick={() => remove.mutate([item.animalId])}><Trash2 size={16} />Retirar</Button>}</td></tr>
      })}</tbody></table></div>}
    </Card>
    {historicos.data && historicos.data.length > 0 && <Card><h3>Historial de membresías</h3><div className="table-wrapper"><table><thead><tr><th>Animal</th><th>Ingreso</th><th>Salida</th><th>Motivo</th></tr></thead><tbody>{historicos.data.filter((item) => item.fechaSalida).map((item) => { const animal = catalogs.data?.animales.find((a) => a.id === item.animalId); return <tr key={item.id}><td><strong>{animal?.codigo ?? '—'}</strong></td><td>{new Date(item.fechaIngreso).toLocaleString('es-BO')}</td><td>{item.fechaSalida ? new Date(item.fechaSalida).toLocaleString('es-BO') : '—'}</td><td>{item.motivoSalida ?? '—'}</td></tr> })}</tbody></table></div></Card>}
    {value.estado === 'CERRADO' && <Alert tone="info">Este lote está cerrado y no admite más animales.</Alert>}
  </div>
}
