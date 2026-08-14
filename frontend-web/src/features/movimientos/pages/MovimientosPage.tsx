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
import type { AnimalSummary } from '@/features/animales/types'
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
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
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

interface FiltrosOrigen {
  propiedadId: string
  potreroId: string
  loteId: string
}

type AnimalFiltrable = Pick<AnimalSummary, 'propiedadActualId' | 'potreroActualId' | 'loteActualId' | 'codigo' | 'nombre'>

export function filtrarAnimalesPorOrigen<T extends AnimalFiltrable>(animales: T[], filtros: FiltrosOrigen, busqueda = ''): T[] {
  if (!filtros.propiedadId) return []
  const termino = busqueda.trim().toLocaleLowerCase('es-BO')
  return animales.filter((animal) => {
    if (animal.propiedadActualId !== filtros.propiedadId) return false
    if (filtros.potreroId && animal.potreroActualId !== filtros.potreroId) return false
    if (filtros.loteId && animal.loteActualId !== filtros.loteId) return false
    if (!termino) return true
    return animal.codigo.toLocaleLowerCase('es-BO').includes(termino)
      || (animal.nombre ?? '').toLocaleLowerCase('es-BO').includes(termino)
  })
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
  const [origenPropiedadId, setOrigenPropiedadId] = useState('')
  const [origenPotreroId, setOrigenPotreroId] = useState('')
  const [origenLoteId, setOrigenLoteId] = useState('')
  const [destinoPropiedadId, setDestinoPropiedadId] = useState('')
  const [destinoPotreroId, setDestinoPotreroId] = useState('')
  const [destinoLoteId, setDestinoLoteId] = useState('')
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
      const [propiedades, potreros, lotesPage, animalesPage] = await Promise.all([
        listPropiedades(), listPotreros(), listLotes({ search: undefined, estado: 'ACTIVO', page: 0, size: 500 }),
        listAnimals({ search: undefined, estado: 'ACTIVO', sexo: '', page: 0, size: 500 }),
      ])
      return { propiedades, potreros, lotes: lotesPage.content, animales: animalesPage.content }
    },
  })
  const animalesOrigen = useQuery({
    queryKey: ['movimientos-animales-origen', origenPropiedadId, origenPotreroId],
    queryFn: () => listAnimals({
      search: undefined,
      estado: 'ACTIVO',
      propiedadId: origenPropiedadId,
      potreroId: origenPotreroId || undefined,
      sexo: '',
      page: 0,
      size: 500,
    }),
    enabled: showForm && !!origenPropiedadId,
  })
  const detalles = useQuery({
    queryKey: ['movimientos', selected?.id, 'detalles'],
    queryFn: () => (selected ? listDetalles(selected.id) : Promise.resolve([])),
    enabled: !!selected,
  })

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const animales = Array.from(animalesSeleccionados).map((id) => ({ animalId: id, version: animalesOrigen.data?.content.find((animal) => animal.id === id)?.version ?? 0 }))
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
  const error = query.error ?? catalogs.error ?? animalesOrigen.error ?? create.error ?? validar.error ?? confirm.error ?? annul.error ?? revert.error

  const req = destinoRequerido(tipoForm)
  const requiereOrigen = tipoForm !== 'INGRESO_COMPRA'
  const potrerosOrigen = useMemo(() => (catalogs.data?.potreros ?? [])
    .filter((item) => item.activo && item.propiedadId === origenPropiedadId), [catalogs.data?.potreros, origenPropiedadId])
  const lotesOrigen = useMemo(() => (catalogs.data?.lotes ?? [])
    .filter((item) => item.propiedadId === origenPropiedadId), [catalogs.data?.lotes, origenPropiedadId])
  const propiedadDestinoEfectiva = destinoPropiedadId || (req === 'potrero' || req === 'lote' || req === 'potrero-o-lote' ? origenPropiedadId : '')
  const propiedadesDestino = useMemo(() => (catalogs.data?.propiedades ?? [])
    .filter((item) => item.activo && (tipoForm !== 'TRANSFERENCIA_PROPIEDAD' || item.id !== origenPropiedadId)), [catalogs.data?.propiedades, origenPropiedadId, tipoForm])
  const potrerosDestino = useMemo(() => (catalogs.data?.potreros ?? [])
    .filter((item) => item.activo
      && item.propiedadId === propiedadDestinoEfectiva
      && (tipoForm !== 'CAMBIO_POTRERO' || item.id !== origenPotreroId)), [catalogs.data?.potreros, origenPotreroId, propiedadDestinoEfectiva, tipoForm])
  const lotesDestino = useMemo(() => (catalogs.data?.lotes ?? [])
    .filter((item) => item.propiedadId === propiedadDestinoEfectiva
      && (tipoForm !== 'CAMBIO_LOTE' || item.id !== origenLoteId)), [catalogs.data?.lotes, origenLoteId, propiedadDestinoEfectiva, tipoForm])
  const animalesFiltrados = useMemo(() => {
    return filtrarAnimalesPorOrigen(animalesOrigen.data?.content ?? [], {
      propiedadId: origenPropiedadId,
      potreroId: origenPotreroId,
      loteId: origenLoteId,
    }, animalSearch)
  }, [animalesOrigen.data?.content, animalSearch, origenLoteId, origenPotreroId, origenPropiedadId])

  const limpiarAnimalesSeleccionados = () => setAnimalesSeleccionados(new Set())
  const cambiarTipo = (tipo: TipoMovimiento) => {
    setTipoForm(tipo)
    setDestinoPropiedadId('')
    setDestinoPotreroId('')
    setDestinoLoteId('')
    limpiarAnimalesSeleccionados()
  }
  const cambiarPropiedadOrigen = (propiedadId: string) => {
    setOrigenPropiedadId(propiedadId)
    setOrigenPotreroId('')
    setOrigenLoteId('')
    if (req !== 'propiedad' || destinoPropiedadId === propiedadId) {
      setDestinoPropiedadId('')
      setDestinoPotreroId('')
      setDestinoLoteId('')
    }
    limpiarAnimalesSeleccionados()
  }
  const cambiarPotreroOrigen = (potreroId: string) => {
    setOrigenPotreroId(potreroId)
    if (tipoForm === 'CAMBIO_POTRERO') setDestinoPotreroId('')
    limpiarAnimalesSeleccionados()
  }
  const cambiarLoteOrigen = (loteId: string) => {
    setOrigenLoteId(loteId)
    if (tipoForm === 'CAMBIO_LOTE') setDestinoLoteId('')
    limpiarAnimalesSeleccionados()
  }
  const cambiarPropiedadDestino = (propiedadId: string) => {
    setDestinoPropiedadId(propiedadId)
    setDestinoPotreroId('')
    setDestinoLoteId('')
  }

  const onViewRelated = (id: string) => {
    void getMovimiento(id).then((movimiento) => setSelected(movimiento)).catch(() => undefined)
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Ganado" title="Movimientos" description="Traslados entre propiedades, potreros y lotes." actions={<Button onClick={() => { setShowForm((value) => { if (!value) { setAnimalesSeleccionados(new Set()); setAnimalSearch(''); setOrigenPropiedadId(''); setOrigenPotreroId(''); setOrigenLoteId(''); setDestinoPropiedadId(''); setDestinoPotreroId(''); setDestinoLoteId('') } return !value }) }}><Plus size={18} />Nuevo movimiento</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {showForm && <Card><h3>Crear movimiento</h3><form className="form-grid compact-form movement-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="Tipo"><select name="tipo" required value={tipoForm} onChange={(event) => cambiarTipo(event.target.value as TipoMovimiento)}>{tipos.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
      <Field label="Fecha"><input name="fecha" type="date" defaultValue={new Date().toISOString().slice(0, 10)} /></Field>
      <Field label="Motivo"><input name="motivo" maxLength={1000} /></Field>
      <Field label="Observación"><input name="observacion" maxLength={1000} /></Field>
      <Field label="Origen (propiedad)" required={requiereOrigen} hint={requiereOrigen ? 'Define qué animales pueden seleccionarse.' : 'Opcional para ingresos por compra.'}><select name="origenPropiedadId" value={origenPropiedadId} onChange={(event) => cambiarPropiedadOrigen(event.target.value)}><option value="">Sin especificar</option>{catalogs.data?.propiedades.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (potrero)" hint="Opcional: limita los animales al potrero."><select name="origenPotreroId" value={origenPotreroId} disabled={!origenPropiedadId} onChange={(event) => cambiarPotreroOrigen(event.target.value)}><option value="">{origenPropiedadId ? 'Todos los potreros' : 'Selecciona primero una propiedad'}</option>{potrerosOrigen.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Origen (lote)" hint="Opcional: limita los animales al lote."><select name="origenLoteId" value={origenLoteId} disabled={!origenPropiedadId} onChange={(event) => cambiarLoteOrigen(event.target.value)}><option value="">{origenPropiedadId ? 'Todos los lotes' : 'Selecciona primero una propiedad'}</option>{lotesOrigen.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      {(req === 'propiedad' || req === 'potrero-o-lote') && <Field label="Destino (propiedad)" required={req === 'propiedad'}><select name="destinoPropiedadId" value={destinoPropiedadId} onChange={(event) => cambiarPropiedadDestino(event.target.value)}><option value="">Sin especificar</option>{propiedadesDestino.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'potrero' || req === 'potrero-o-lote' || req === 'propiedad') && <Field label="Destino (potrero)" required={req === 'potrero' || tipoForm === 'TRANSFERENCIA_PROPIEDAD'} hint={propiedadDestinoEfectiva ? undefined : 'Selecciona primero la propiedad correspondiente.'}><select name="destinoPotreroId" value={destinoPotreroId} disabled={!propiedadDestinoEfectiva} onChange={(event) => setDestinoPotreroId(event.target.value)}><option value="">Sin especificar</option>{potrerosDestino.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      {(req === 'lote' || req === 'potrero-o-lote') && <Field label="Destino (lote)" required={req === 'lote'} hint={propiedadDestinoEfectiva ? undefined : 'Selecciona primero la propiedad correspondiente.'}><select name="destinoLoteId" value={destinoLoteId} disabled={!propiedadDestinoEfectiva} onChange={(event) => setDestinoLoteId(event.target.value)}><option value="">Sin especificar</option>{lotesDestino.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>}
      <fieldset className="movement-animal-picker form-full">
        <legend>Animales a mover</legend>
        <div className="movement-picker-toolbar">
          <span className="search-box"><Search size={18} aria-hidden="true" /><input name="animalSearch" type="search" autoComplete="off" aria-label="Buscar animales para el movimiento" value={animalSearch} disabled={!origenPropiedadId || animalesOrigen.isPending} onChange={(event) => setAnimalSearch(event.target.value)} placeholder="Buscar por código o nombre…" /></span>
          <span className="movement-selection-summary" aria-live="polite">{animalesSeleccionados.size} seleccionados · {animalesFiltrados.length} disponibles</span>
        </div>
        {!origenPropiedadId
          ? <p className="movement-picker-empty" role="status">Selecciona una propiedad de origen para mostrar sus animales.</p>
          : animalesOrigen.isPending
            ? <p className="movement-picker-empty" role="status">Cargando animales…</p>
            : animalesFiltrados.length > 0
          ? <div className="movement-animal-list">{animalesFiltrados.map((animal) => <label key={animal.id} className="movement-animal-option"><input type="checkbox" name="animales" value={animal.id} checked={animalesSeleccionados.has(animal.id)} onChange={(event) => { setAnimalesSeleccionados((prev) => { const next = new Set(prev); if (event.target.checked) next.add(animal.id); else next.delete(animal.id); return next }) }} /><span><strong>{animal.codigo}</strong>{animal.nombre ? ` · ${animal.nombre}` : ''}</span></label>)}</div>
          : <p className="movement-picker-empty" role="status">No hay animales que coincidan con la búsqueda.</p>}
      </fieldset>
      <div className="form-actions form-full"><Button type="submit" loading={create.isPending}>Crear movimiento</Button></div>
    </form></Card>}
    <Card>
      <div className="filter-heading"><span>Filtros de movimientos</span>
        <select aria-label="Filtrar por estado" value={estado} onChange={(event) => { setEstado(event.target.value as EstadoMovimiento | ''); setPage(0) }}><option value="">Todos los estados</option>{estados.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filtrar por tipo" value={tipoFiltro} onChange={(event) => { setTipoFiltro(event.target.value as TipoMovimiento | ''); setPage(0) }}><option value="">Todos los tipos</option>{tipos.map((value) => <option key={value}>{value}</option>)}</select></div>
      {query.isPending && <TableSkeleton rows={8} columns={6} />}
      {query.data?.content.length === 0 && <EmptyState title="No hay movimientos" description="Crea el primer movimiento de animales." />}
      {query.data && query.data.content.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Movimientos que coinciden con los filtros</caption><thead><tr><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Fecha</th><th scope="col">Origen</th><th scope="col">Destino</th><th scope="col">Acciones</th></tr></thead><tbody>{query.data.content.map((item) => {
          const origen = [item.origenPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.origenPropiedadId)?.nombre : null, item.origenPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.origenPotreroId)?.nombre : null, item.origenLoteId ? catalogs.data?.lotes.find((l) => l.id === item.origenLoteId)?.nombre : null].filter(Boolean).join(' / ')
          const destino = [item.destinoPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.destinoPropiedadId)?.nombre : null, item.destinoPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.destinoPotreroId)?.nombre : null, item.destinoLoteId ? catalogs.data?.lotes.find((l) => l.id === item.destinoLoteId)?.nombre : null].filter(Boolean).join(' / ')
          return <tr key={item.id}><td><strong>{item.tipo.replaceAll('_', ' ')}</strong></td><td><MovimientoStatusBadge estado={item.estado} /></td><td>{new Date(item.fechaMovimiento).toLocaleDateString('es-BO')}</td><td className="table-secondary">{origen || '—'}</td><td className="table-secondary">{destino || '—'}</td>
            <td><Button variant="ghost" aria-label={`Ver detalle del movimiento ${item.tipo.replaceAll('_', ' ')} del ${new Date(item.fechaMovimiento).toLocaleDateString('es-BO')}`} onClick={() => setSelected(item)}><Eye size={16} aria-hidden="true" />Detalle</Button></td>
          </tr>
        })}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{query.data.content.map((item) => {
          const origin = [item.origenPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.origenPropiedadId)?.nombre : null, item.origenPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.origenPotreroId)?.nombre : null, item.origenLoteId ? catalogs.data?.lotes.find((l) => l.id === item.origenLoteId)?.nombre : null].filter(Boolean).join(' / ')
          const destination = [item.destinoPropiedadId ? catalogs.data?.propiedades.find((p) => p.id === item.destinoPropiedadId)?.nombre : null, item.destinoPotreroId ? catalogs.data?.potreros.find((p) => p.id === item.destinoPotreroId)?.nombre : null, item.destinoLoteId ? catalogs.data?.lotes.find((l) => l.id === item.destinoLoteId)?.nombre : null].filter(Boolean).join(' / ')
          return <MobileEntityCard key={item.id} title={`Movimiento ${item.id.slice(0, 8)}`} status={<MovimientoStatusBadge estado={item.estado} />} subtitle={item.tipo.replaceAll('_', ' ')} metadata={<><span>{new Date(item.fechaMovimiento).toLocaleString('es-BO')}</span><span>{origin || 'Sin origen'} → {destination || 'Sin destino'}</span></>} action={<Button variant="ghost" onClick={() => setSelected(item)}>Ver detalle →</Button>} />
        })}</div></div>
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
