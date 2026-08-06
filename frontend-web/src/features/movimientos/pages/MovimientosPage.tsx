import { useDeferredValue, useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, ChevronLeft, ChevronRight, Plus, Search, X } from 'lucide-react'
import { anularMovimiento, confirmarMovimiento, createMovimiento, listMovimientos } from '@/features/movimientos/api'
import type { EstadoMovimiento, TipoMovimiento } from '@/features/movimientos/api'
import { listAnimals } from '@/features/animales/api'
import { listLotes } from '@/features/lotes/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

const tipos: TipoMovimiento[] = ['CAMBIO_POTRERO', 'CAMBIO_LOTE', 'TRANSFERENCIA_PROPIEDAD', 'INGRESO_COMPRA', 'SALIDA_VENTA', 'CUARENTENA', 'RETORNO_CUARENTENA']
const estados: EstadoMovimiento[] = ['PENDIENTE', 'CONFIRMADO', 'ANULADO']

function destinoRequerido(tipo: TipoMovimiento): 'propiedad' | 'potrero' | 'lote' | 'potrero-o-lote' {
  if (tipo === 'CAMBIO_LOTE') return 'lote'
  if (tipo === 'CAMBIO_POTRERO' || tipo === 'CUARENTENA' || tipo === 'RETORNO_CUARENTENA') return 'potrero'
  if (tipo === 'INGRESO_COMPRA' || tipo === 'TRANSFERENCIA_PROPIEDAD' || tipo === 'SALIDA_VENTA') return 'propiedad'
  return 'potrero-o-lote'
}

export function MovimientosPage() {
  const client = useQueryClient()
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [page, setPage] = useState(0)
  const [estado, setEstado] = useState<EstadoMovimiento | ''>('')
  const [tipoFiltro, setTipoFiltro] = useState<TipoMovimiento | ''>('')
  const [showForm, setShowForm] = useState(false)
  const size = 10

  const query = useQuery({
    queryKey: ['movimientos', { search: deferredSearch, estado: estado, tipo: tipoFiltro, page, size }],
    queryFn: () => listMovimientos({ estado, tipo: tipoFiltro, page, size }),
    placeholderData: keepPreviousData,
  })
  const catalogs = useQuery({
    queryKey: ['movimientos-catalogs'],
    queryFn: async () => {
      const [propiedades, potreros, lotesPage, animales] = await Promise.all([
        listPropiedades(), listPotreros(), listLotes({ search: undefined, estado: 'ABIERTO', page: 0, size: 500 }),
        listAnimals({ search: undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 500 }),
      ])
      return { propiedades, potreros, lotes: lotesPage.content, animales: animales.content }
    },
  })

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const tipo = String(data.get('tipo')) as TipoMovimiento
      return createMovimiento({
        tipo,
        fechaMovimiento: String(data.get('fecha') ?? '') || undefined,
        motivo: String(data.get('motivo') ?? '') || undefined,
        origenPropiedadId: String(data.get('origenPropiedadId') ?? '') || undefined,
        origenPotreroId: String(data.get('origenPotreroId') ?? '') || undefined,
        origenLoteId: String(data.get('origenLoteId') ?? '') || undefined,
        destinoPropiedadId: String(data.get('destinoPropiedadId') ?? '') || undefined,
        destinoPotreroId: String(data.get('destinoPotreroId') ?? '') || undefined,
        destinoLoteId: String(data.get('destinoLoteId') ?? '') || undefined,
        animalIds: Array.from(form.querySelectorAll<HTMLInputElement>('input[name="animales"]:checked')).map((input) => input.value),
      })
    },
    onSuccess: async () => { setShowForm(false); await client.invalidateQueries({ queryKey: ['movimientos'] }); await client.invalidateQueries({ queryKey: ['animals'] }) },
  })
  const confirm = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => { if (!window.confirm('¿Confirmar este movimiento?')) return Promise.reject(new Error('Cancelado')); return confirmarMovimiento(id, version) },
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['movimientos'] }); await client.invalidateQueries({ queryKey: ['animals'] }) },
  })
  const annul = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => {
      const motivo = window.prompt('Motivo de la anulación') ?? ''
      if (!motivo.trim()) return Promise.reject(new Error('Motivo requerido'))
      return anularMovimiento(id, motivo, version)
    },
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['movimientos'] }) },
  })
  const error = query.error ?? catalogs.error ?? create.error ?? confirm.error ?? annul.error

  const [tipoForm, setTipoForm] = useState<TipoMovimiento>('CAMBIO_POTRERO')
  const req = useMemo(() => destinoRequerido(tipoForm), [tipoForm])

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Movimientos" description="Traslados entre propiedades, potreros y lotes." actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} />Nuevo movimiento</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {create.isSuccess && <Alert tone="success">Movimiento creado correctamente.</Alert>}
    {showForm && <Card><h3>Crear movimiento</h3><form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="Tipo"><select name="tipo" required value={tipoForm} onChange={(event) => setTipoForm(event.target.value as TipoMovimiento)}>{tipos.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
      <Field label="Fecha"><input name="fecha" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
      <Field label="Motivo"><input name="motivo" maxLength={1000} /></Field>
      <Field label="Origen (propiedad)"><select name="origenPropiedadId"><option value="">Sin especificar</option>{catalogs.data?.propiedades.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (potrero)"><select name="origenPotreroId"><option value="">Sin especificar</option>{catalogs.data?.potreros.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (lote)"><select name="origenLoteId"><option value="">Sin especificar</option>{catalogs.data?.lotes.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      {(req === 'propiedad' || req === 'potrero-o-lote') && <Field label="Destino (propiedad)" hint={req === 'propiedad' ? 'Requerido' : undefined}><select name="destinoPropiedadId"><option value="">Sin especificar</option>{catalogs.data?.propiedades.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'potrero' || req === 'potrero-o-lote' || req === 'propiedad') && <Field label="Destino (potrero)" hint={req === 'potrero' ? 'Requerido' : undefined}><select name="destinoPotreroId"><option value="">Sin especificar</option>{catalogs.data?.potreros.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'lote' || req === 'potrero-o-lote') && <Field label="Destino (lote)" hint={req === 'lote' ? 'Requerido' : undefined}><select name="destinoLoteId"><option value="">Sin especificar</option>{catalogs.data?.lotes.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      <Field label="Animales a mover"><div className="checkbox-stack">{catalogs.data?.animales.map((animal) => <label key={animal.id}><input type="checkbox" name="animales" value={animal.id} /> {animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</label>)}</div></Field>
      <div className="form-actions"><Button type="submit" loading={create.isPending}>Crear movimiento</Button></div>
    </form></Card>}
    <Card>
      <div className="filter-heading"><span className="search-box"><Search size={18} /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Filtrar" /></span>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as EstadoMovimiento | ''); setPage(0) }}><option value="">Todos los estados</option>{estados.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por tipo" value={tipoFiltro} onChange={(event) => { setTipoFiltro(event.target.value as TipoMovimiento | ''); setPage(0) }}><option value="">Todos los tipos</option>{tipos.map((value) => <option key={value}>{value}</option>)}</select></div>
      {query.isPending && <LoadingState message="Consultando movimientos…" />}
      {query.data?.content.length === 0 && <EmptyState title="No hay movimientos" description="Crea el primer movimiento de animales." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Tipo</th><th>Estado</th><th>Fecha</th><th>Origen</th><th>Destino</th><th></th></tr></thead><tbody>{query.data.content.map((item) => {
          const origen = [item.origenPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.origenPropiedadId)?.nombre : null, item.origenPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.origenPotreroId)?.nombre : null, item.origenLoteId ? catalogs.data?.lotes.find((l) => l.id === item.origenLoteId)?.nombre : null].filter(Boolean).join(' / ')
          const destino = [item.destinoPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.destinoPropiedadId)?.nombre : null, item.destinoPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.destinoPotreroId)?.nombre : null, item.destinoLoteId ? catalogs.data?.lotes.find((l) => l.id === item.destinoLoteId)?.nombre : null].filter(Boolean).join(' / ')
          return <tr key={item.id}><td><strong>{item.tipo.replaceAll('_', ' ')}</strong></td><td><span className="status-badge">{item.estado}</span></td><td>{new Date(item.fechaMovimiento).toLocaleDateString('es-BO')}</td><td className="table-secondary">{origen || '—'}</td><td className="table-secondary">{destino || '—'}</td>
            <td>{item.estado === 'PENDIENTE' && <div className="row-actions"><Button variant="secondary" loading={confirm.isPending} onClick={() => confirm.mutate({ id: item.id, version: item.version })}><Check size={16} />Confirmar</Button><Button variant="danger" loading={annul.isPending} onClick={() => annul.mutate({ id: item.id, version: item.version })}><X size={16} />Anular</Button></div>}</td>
          </tr>
        })}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
  </div>
}
