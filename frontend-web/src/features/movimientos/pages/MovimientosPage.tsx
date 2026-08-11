import { useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Eye, Plus, Search } from 'lucide-react'
import { anularMovimiento, confirmarMovimiento, createMovimiento, getMovimiento, listDetalles, listMovimientos, revertirMovimiento, validarMovimiento } from '@/features/movimientos/api'
import type { EstadoMovimiento, Movimiento, TipoMovimiento, ValidacionMovimiento } from '@/features/movimientos/api'
import { MovimientoDetailModal } from '@/features/movimientos/components/MovimientoDetailModal'
import { MovimientoStatusBadge } from '@/features/movimientos/components/MovimientoStatusBadge'
import { MovimientoValidationDialog } from '@/features/movimientos/components/MovimientoValidationDialog'
import { MotivoModal } from '@/features/movimientos/components/MotivoModal'
import { listAnimals } from '@/features/animales/api'
import { listLotes } from '@/features/lotes/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listPotreros } from '@/features/potreros/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { TableSkeleton } from '@/shared/components/Skeleton'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

const tipos: TipoMovimiento[] = ['CAMBIO_POTRERO', 'CAMBIO_LOTE', 'TRANSFERENCIA_PROPIEDAD', 'INGRESO_COMPRA', 'SALIDA_VENTA', 'CUARENTENA', 'RETORNO_CUARENTENA']
const estados: EstadoMovimiento[] = ['PENDIENTE', 'CONFIRMADO', 'ANULADO', 'REVERTIDO']
export const movementSearchAvailable = false

function destinoRequerido(tipo: TipoMovimiento): 'propiedad' | 'potrero' | 'lote' | 'potrero-o-lote' {
  if (tipo === 'CAMBIO_LOTE') return 'lote'
  if (tipo === 'CAMBIO_POTRERO' || tipo === 'CUARENTENA' || tipo === 'RETORNO_CUARENTENA') return 'potrero'
  if (tipo === 'INGRESO_COMPRA' || tipo === 'TRANSFERENCIA_PROPIEDAD' || tipo === 'SALIDA_VENTA') return 'propiedad'
  return 'potrero-o-lote'
}

export function MovimientosPage() {
  const client = useQueryClient()
  const { showToast } = useToast()
  const [page, setPage] = useState(0)
  const [estado, setEstado] = useState<EstadoMovimiento | ''>('')
  const [tipoFiltro, setTipoFiltro] = useState<TipoMovimiento | ''>('')
  const [showForm, setShowForm] = useState(false)
  const [tipoForm, setTipoForm] = useState<TipoMovimiento>('CAMBIO_POTRERO')
  const [animalSearch, setAnimalSearch] = useState('')
  const [animalesSeleccionados, setAnimalesSeleccionados] = useState<Set<string>>(new Set())
  const [selected, setSelected] = useState<Movimiento | null>(null)
  const [validation, setValidation] = useState<ValidacionMovimiento | null>(null)
  const [anularTarget, setAnularTarget] = useState<Movimiento | null>(null)
  const [revertirTarget, setRevertirTarget] = useState<Movimiento | null>(null)
  const size = 10

  const invalidateMovimientos = async () => {
    await client.invalidateQueries({ queryKey: ['movimientos'] })
    await client.invalidateQueries({ queryKey: ['animals'] })
  }

  const query = useQuery({
    queryKey: ['movimientos', { estado, tipo: tipoFiltro, page, size }],
    queryFn: () => listMovimientos({ estado, tipo: tipoFiltro, page, size }),
    placeholderData: keepPreviousData,
  })
  const catalogs = useQuery({
    queryKey: ['movimientos-catalogs'],
    queryFn: async () => {
      const [propiedades, potreros, lotesPage, animales] = await Promise.all([
        listPropiedades(), listPotreros(), listLotes({ search: undefined, estado: 'ACTIVO', page: 0, size: 500 }),
        listAnimals({ search: undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 500 }),
      ])
      return { propiedades, potreros, lotes: lotesPage.content, animales: animales.content }
    },
  })
  const detalles = useQuery({
    queryKey: ['movimientos', selected?.id, 'detalles'],
    queryFn: () => (selected ? listDetalles(selected.id) : Promise.resolve([])),
    enabled: !!selected,
  })

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const animales = Array.from(animalesSeleccionados).map((id) => ({ animalId: id, version: catalogs.data?.animales.find((animal) => animal.id === id)?.version ?? 0 }))
      return createMovimiento({
        tipo: String(data.get('tipo')) as TipoMovimiento,
        fechaMovimiento: String(data.get('fecha') ?? '') || undefined,
        motivo: String(data.get('motivo') ?? '') || undefined,
        observacion: String(data.get('observacion') ?? '') || undefined,
        origenPropiedadId: String(data.get('origenPropiedadId') ?? '') || undefined,
        origenPotreroId: String(data.get('origenPotreroId') ?? '') || undefined,
        origenLoteId: String(data.get('origenLoteId') ?? '') || undefined,
        destinoPropiedadId: String(data.get('destinoPropiedadId') ?? '') || undefined,
        destinoPotreroId: String(data.get('destinoPotreroId') ?? '') || undefined,
        destinoLoteId: String(data.get('destinoLoteId') ?? '') || undefined,
        animales,
      })
    },
    onSuccess: async () => { setShowForm(false); setAnimalesSeleccionados(new Set()); showToast('Movimiento creado correctamente.'); await invalidateMovimientos() },
  })
  const validar = useMutation({
    mutationFn: (id: string) => validarMovimiento(id),
    onSuccess: (data) => setValidation(data),
  })
  const confirm = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => confirmarMovimiento(id, version),
    onSuccess: async () => { setValidation(null); setSelected(null); showToast('Movimiento confirmado.'); await invalidateMovimientos() },
  })
  const annul = useMutation({
    mutationFn: ({ id, motivo, version }: { id: string; motivo: string; version: number }) => anularMovimiento(id, motivo, version),
    onSuccess: async () => { setAnularTarget(null); setSelected(null); await invalidateMovimientos() },
  })
  const revert = useMutation({
    mutationFn: ({ id, motivo, version }: { id: string; motivo: string; version: number }) => revertirMovimiento(id, motivo, version),
    onSuccess: async () => { setRevertirTarget(null); setSelected(null); showToast('Movimiento revertido.'); await invalidateMovimientos() },
  })
  const error = query.error ?? catalogs.error ?? create.error ?? validar.error ?? confirm.error ?? annul.error ?? revert.error

  const req = useMemo(() => destinoRequerido(tipoForm), [tipoForm])
  const animalesFiltrados = useMemo(() => {
    const all = catalogs.data?.animales ?? []
    if (!animalSearch.trim()) return all
    const termino = animalSearch.toLowerCase()
    return all.filter((animal) => (animal.codigo ?? '').toLowerCase().includes(termino) || (animal.nombre ?? '').toLowerCase().includes(termino))
  }, [catalogs.data, animalSearch])

  const onViewRelated = (id: string) => {
    void getMovimiento(id).then((movimiento) => setSelected(movimiento)).catch(() => undefined)
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Movimientos" description="Traslados entre propiedades, potreros y lotes." actions={<Button onClick={() => { setShowForm((value) => { if (!value) { setAnimalesSeleccionados(new Set()); setAnimalSearch('') } return !value }) }}><Plus size={18} />Nuevo movimiento</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {showForm && <Card><h3>Crear movimiento</h3><form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="Tipo"><select name="tipo" required value={tipoForm} onChange={(event) => setTipoForm(event.target.value as TipoMovimiento)}>{tipos.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
      <Field label="Fecha"><input name="fecha" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
      <Field label="Motivo"><input name="motivo" maxLength={1000} /></Field>
      <Field label="Observación"><input name="observacion" maxLength={1000} /></Field>
      <Field label="Origen (propiedad)"><select name="origenPropiedadId"><option value="">Sin especificar</option>{catalogs.data?.propiedades.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (potrero)"><select name="origenPotreroId"><option value="">Sin especificar</option>{catalogs.data?.potreros.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (lote)"><select name="origenLoteId"><option value="">Sin especificar</option>{catalogs.data?.lotes.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      {(req === 'propiedad' || req === 'potrero-o-lote') && <Field label="Destino (propiedad)" hint={req === 'propiedad' ? 'Requerido' : undefined}><select name="destinoPropiedadId"><option value="">Sin especificar</option>{catalogs.data?.propiedades.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'potrero' || req === 'potrero-o-lote' || req === 'propiedad') && <Field label="Destino (potrero)" hint={req === 'potrero' ? 'Requerido' : undefined}><select name="destinoPotreroId"><option value="">Sin especificar</option>{catalogs.data?.potreros.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'lote' || req === 'potrero-o-lote') && <Field label="Destino (lote)" hint={req === 'lote' ? 'Requerido' : undefined}><select name="destinoLoteId"><option value="">Sin especificar</option>{catalogs.data?.lotes.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      <Field label={`Animales a mover (${animalesSeleccionados.size} seleccionados)`}>
        <div style={{ display: 'grid', gap: 8 }}>
          <span className="search-box"><Search size={18} /><input value={animalSearch} onChange={(event) => setAnimalSearch(event.target.value)} placeholder="Buscar por código o nombre" /></span>
          <div className="checkbox-stack" style={{ maxHeight: 220, overflowY: 'auto' }}>{animalesFiltrados.map((animal) => <label key={animal.id}><input type="checkbox" name="animales" value={animal.id} checked={animalesSeleccionados.has(animal.id)} onChange={(event) => { setAnimalesSeleccionados((prev) => { const next = new Set(prev); if (event.target.checked) next.add(animal.id); else next.delete(animal.id); return next }) }} /> {animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</label>)}</div>
        </div>
      </Field>
      <div className="form-actions"><Button type="submit" loading={create.isPending}>Crear movimiento</Button></div>
    </form></Card>}
    <Card>
      <div className="filter-heading"><span>Filtros de movimientos</span>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as EstadoMovimiento | ''); setPage(0) }}><option value="">Todos los estados</option>{estados.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por tipo" value={tipoFiltro} onChange={(event) => { setTipoFiltro(event.target.value as TipoMovimiento | ''); setPage(0) }}><option value="">Todos los tipos</option>{tipos.map((value) => <option key={value}>{value}</option>)}</select></div>
      {query.isPending && <TableSkeleton rows={8} columns={6} />}
      {query.data?.content.length === 0 && <EmptyState title="No hay movimientos" description="Crea el primer movimiento de animales." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper"><table><thead><tr><th>Tipo</th><th>Estado</th><th>Fecha</th><th>Origen</th><th>Destino</th><th></th></tr></thead><tbody>{query.data.content.map((item) => {
          const origen = [item.origenPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.origenPropiedadId)?.nombre : null, item.origenPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.origenPotreroId)?.nombre : null, item.origenLoteId ? catalogs.data?.lotes.find((l) => l.id === item.origenLoteId)?.nombre : null].filter(Boolean).join(' / ')
          const destino = [item.destinoPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.destinoPropiedadId)?.nombre : null, item.destinoPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.destinoPotreroId)?.nombre : null, item.destinoLoteId ? catalogs.data?.lotes.find((l) => l.id === item.destinoLoteId)?.nombre : null].filter(Boolean).join(' / ')
          return <tr key={item.id}><td><strong>{item.tipo.replaceAll('_', ' ')}</strong></td><td><MovimientoStatusBadge estado={item.estado} /></td><td>{new Date(item.fechaMovimiento).toLocaleDateString('es-BO')}</td><td className="table-secondary">{origen || '—'}</td><td className="table-secondary">{destino || '—'}</td>
            <td><Button variant="ghost" onClick={() => setSelected(item)}><Eye size={16} />Detalle</Button></td>
          </tr>
        })}</tbody></table></div>
        <div className="pagination"><span>Página {query.data.page + 1} de {Math.max(query.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || query.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>

    <MovimientoDetailModal
      open={!!selected}
      onClose={() => setSelected(null)}
      movimiento={selected}
      detalles={detalles.data}
      catalogs={catalogs.data}
      onValidar={() => { if (selected) validar.mutate(selected.id) }}
      onConfirmar={() => { if (selected) validar.mutate(selected.id) }}
      onAnular={() => { if (selected) setAnularTarget(selected) }}
      onRevertir={() => { if (selected) setRevertirTarget(selected) }}
      onViewRelated={onViewRelated}
      pending={{ validar: validar.isPending, confirmar: confirm.isPending, anular: annul.isPending, revertir: revert.isPending }}
    />

    <MovimientoValidationDialog
      open={!!validation}
      onClose={() => setValidation(null)}
      validation={validation}
      loading={validar.isPending}
      onConfirm={() => { if (validation && selected) confirm.mutate({ id: selected.id, version: selected.version }) }}
    />

    <MotivoModal
      open={!!anularTarget}
      title="Anular movimiento"
      confirmLabel="Anular movimiento"
      loading={annul.isPending}
      error={annul.isError ? normalizeApiError(annul.error).message : undefined}
      onClose={() => setAnularTarget(null)}
      onConfirm={(motivo) => { if (anularTarget) annul.mutate({ id: anularTarget.id, motivo, version: anularTarget.version }) }}
    />
    <MotivoModal
      open={!!revertirTarget}
      title="Revertir movimiento"
      confirmLabel="Revertir movimiento"
      loading={revert.isPending}
      error={revert.isError ? normalizeApiError(revert.error).message : undefined}
      onClose={() => setRevertirTarget(null)}
      onConfirm={(motivo) => { if (revertirTarget) revert.mutate({ id: revertirTarget.id, motivo, version: revertirTarget.version }) }}
    />
  </div>
}
