import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Pencil, Plus, Power } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { PotreroEditModal } from '@/features/potreros/components/PotreroEditModal'
import { PotreroFormModal } from '@/features/potreros/components/PotreroFormModal'
import { createPotrero, listPotreros, listTiposPasto, updatePotrero, type Potrero } from '@/features/potreros/api'
import { listPropiedades } from '@/features/propiedades/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

/**
 * Reutiliza clases de estado ya existentes en index.css en vez de inventar colores nuevos:
 * DISPONIBLE = positivo (verde), OCUPADO = informativo (azul), DESCANSO = estado especial
 * planificado (violeta, igual que "revertido"), MANTENIMIENTO = requiere atención (ámbar).
 */
const ESTADO_BADGE_CLASS: Record<Potrero['estado'], string> = {
  DISPONIBLE: 'status-activo',
  OCUPADO: 'status-syncing',
  DESCANSO: 'status-badge-reverted',
  MANTENIMIENTO: 'status-badge-warning',
}

export function PotrerosPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [stateTarget, setStateTarget] = useState<{ potrero: Potrero; estado: Potrero['estado'] } | null>(null)
  const [editTarget, setEditTarget] = useState<Potrero | null>(null)
  const [activeTarget, setActiveTarget] = useState<Potrero | null>(null)
  const size = 20
  const paddocks = useQuery({
    queryKey: ['potreros', { page, size }],
    queryFn: () => listPotreros({ page, size }),
    placeholderData: keepPreviousData,
  })
  const catalogs = useQuery({ queryKey: ['potrero-catalogos'], queryFn: async () => { const [properties, grasses] = await Promise.all([listPropiedades(), listTiposPasto()]); return { properties, grasses } } })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createPotrero({ propiedadId: String(data.get('propiedadId')), sectorId: String(data.get('sectorId') || '') || undefined, nombre: String(data.get('nombre')), superficieHa: Number(data.get('superficieHa')) || undefined, tipoPastoId: String(data.get('tipoPastoId') || '') || undefined, capacidadUa: Number(data.get('capacidadUa')) || undefined, tieneAgua: data.get('tieneAgua') === 'on', estado: String(data.get('estado')) as 'DISPONIBLE' | 'OCUPADO' | 'DESCANSO' | 'MANTENIMIENTO' })
    },
    onSuccess: () => { setShowForm(false); client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const changeState = useMutation({
    mutationFn: ({ id, estado, version }: { id: string; estado: 'DISPONIBLE' | 'OCUPADO' | 'DESCANSO' | 'MANTENIMIENTO'; version: number }) => updatePotrero(id, { estado, version }),
    onSuccess: () => { setStateTarget(null); client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const edit = useMutation({
    mutationFn: ({ id, input }: { id: string; input: Parameters<typeof updatePotrero>[1] }) => updatePotrero(id, input),
    onSuccess: () => { setEditTarget(null); void client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const changeActive = useMutation({
    mutationFn: (potrero: Potrero) => updatePotrero(potrero.id, { activo: !potrero.activo, version: potrero.version }),
    onSuccess: () => { setActiveTarget(null); void client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const error = paddocks.error ?? catalogs.error ?? create.error ?? changeState.error ?? changeActive.error
  const canEdit = can('POTRERO_EDITAR')
  const canCreate = can('POTRERO_CREAR')

  return <div className="page-stack">
    <PageHeader eyebrow="Campo" title="Potreros" description="Controla capacidad, pastura, agua y disponibilidad." actions={canCreate ? <Button onClick={() => setShowForm(true)}><Plus size={18} aria-hidden="true" />Nuevo potrero</Button> : undefined} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>{paddocks.isPending && <LoadingState message="Consultando potreros…" />}{paddocks.data?.content.length === 0 && <EmptyState title="No hay potreros" description="Crea una propiedad y registra su primer potrero." />}
      {paddocks.data && paddocks.data.content.length > 0 && <>
        <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Potreros registrados</caption><thead><tr><th scope="col">Potrero</th><th scope="col">Propiedad</th><th scope="col" className="numeric">Superficie</th><th scope="col" className="numeric">Capacidad</th><th scope="col">Agua</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{paddocks.data.content.map((item) => <tr key={item.id}><td><strong className="potrero-name">{item.nombre}</strong><span className="potrero-code">{item.codigo}</span></td><td>{catalogs.data?.properties.find((property) => property.id === item.propiedadId)?.nombre ?? item.propiedadId}</td><td className="numeric">{item.superficieHa !== undefined ? `${item.superficieHa} ha` : '—'}</td><td className="numeric">{item.capacidadUa ?? '—'}</td><td>{item.tieneAgua ? 'Sí' : 'No'}</td><td><div className="potrero-estado-cell"><select className="potrero-estado-select" aria-label={`Estado de ${item.nombre}`} value={item.estado} disabled={!canEdit || changeState.isPending} onChange={(event) => setStateTarget({ potrero: item, estado: event.target.value as Potrero['estado'] })}><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select>{!item.activo && <span className="status-badge status-inactivo">INACTIVO</span>}</div></td><td>{canEdit && <div className="inline-actions"><Button variant="ghost" onClick={() => setEditTarget(item)}><Pencil size={16} aria-hidden="true" />Editar</Button><Button variant="ghost" onClick={() => setActiveTarget(item)}><Power size={16} aria-hidden="true" />{item.activo ? 'Desactivar' : 'Activar'}</Button></div>}</td></tr>)}</tbody></table></div>
        <div className="mobile-only"><div className="mobile-entity-list">{paddocks.data.content.map((item) => <MobileEntityCard key={item.id} title={<div><strong className="potrero-name">{item.nombre}</strong><span className="potrero-code">{item.codigo}</span></div>} status={item.activo ? <span className={`status-badge ${ESTADO_BADGE_CLASS[item.estado]}`}>{item.estado}</span> : <span className="status-badge status-inactivo">INACTIVO</span>} subtitle={catalogs.data?.properties.find((property) => property.id === item.propiedadId)?.nombre ?? item.propiedadId} metadata={<><span>{item.superficieHa !== undefined ? `${item.superficieHa} ha` : 'Superficie no registrada'} · {item.capacidadUa ?? '—'} UA</span><span>Agua: {item.tieneAgua ? 'Sí' : 'No'}</span></>} action={canEdit ? <><select className="potrero-estado-select" aria-label={`Cambiar estado de ${item.nombre}`} value={item.estado} disabled={changeState.isPending} onChange={(event) => setStateTarget({ potrero: item, estado: event.target.value as Potrero['estado'] })}><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select><Button variant="ghost" onClick={() => setEditTarget(item)}>Editar</Button><Button variant="ghost" onClick={() => setActiveTarget(item)}>{item.activo ? 'Desactivar' : 'Activar'}</Button></> : undefined} />)}</div></div>
        <div className="pagination"><span>Página {paddocks.data.page + 1} de {Math.max(paddocks.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || paddocks.isFetching} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} />Anterior</Button><Button variant="ghost" disabled={page + 1 >= paddocks.data.totalPages || paddocks.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente<ChevronRight size={17} /></Button></div></div>
      </>}
    </Card>
    {showForm && <PotreroFormModal properties={catalogs.data?.properties ?? []} grasses={catalogs.data?.grasses ?? []} loading={create.isPending} error={create.error} onClose={() => setShowForm(false)} onSubmit={(form) => create.mutate(form)} />}
    <ConfirmDialog
      open={Boolean(stateTarget)}
      title="Confirmar estado del potrero"
      confirmLabel="Confirmar cambio"
      variant={stateTarget?.estado === 'MANTENIMIENTO' ? 'danger' : 'warning'}
      loading={changeState.isPending}
      error={changeState.error}
      onClose={() => setStateTarget(null)}
      onConfirm={() => { if (stateTarget && !changeState.isPending) changeState.mutate({ id: stateTarget.potrero.id, estado: stateTarget.estado, version: stateTarget.potrero.version }) }}
    >
      {stateTarget && <div className="page-stack"><dl className="detail-list">
        <div><dt>Potrero</dt><dd>{stateTarget.potrero.codigo} · {stateTarget.potrero.nombre}</dd></div>
        <div><dt>Estado actual</dt><dd>{stateTarget.potrero.estado}</dd></div>
        <div><dt>Estado nuevo</dt><dd>{stateTarget.estado}</dd></div>
      </dl><p className="muted">El nuevo estado modificará cómo se presenta la disponibilidad operativa de este potrero.</p></div>}
    </ConfirmDialog>
    {editTarget && catalogs.data && <PotreroEditModal key={`${editTarget.id}-${editTarget.version}`} potrero={editTarget} properties={catalogs.data.properties} grasses={catalogs.data.grasses} loading={edit.isPending} error={edit.error} onClose={() => setEditTarget(null)} onSubmit={(input) => edit.mutate({ id: editTarget.id, input })} onReload={() => { setEditTarget(null); void client.invalidateQueries({ queryKey: ['potreros'] }) }} />}
    <ConfirmDialog
      open={Boolean(activeTarget)}
      title={activeTarget?.activo ? 'Desactivar potrero' : 'Activar potrero'}
      confirmLabel={activeTarget?.activo ? 'Desactivar potrero' : 'Activar potrero'}
      variant={activeTarget?.activo ? 'danger' : 'warning'}
      loading={changeActive.isPending}
      error={changeActive.error}
      onClose={() => setActiveTarget(null)}
      onConfirm={() => { if (activeTarget && !changeActive.isPending) changeActive.mutate(activeTarget) }}
    >
      {activeTarget && <div className="page-stack"><dl className="detail-list"><div><dt>Potrero</dt><dd>{activeTarget.codigo} · {activeTarget.nombre}</dd></div><div><dt>Estado nuevo</dt><dd>{activeTarget.activo ? 'INACTIVO' : 'ACTIVO'}</dd></div></dl><p className="muted">{activeTarget.activo ? 'No se puede desactivar un potrero que todavía tenga animales activos asignados.' : 'El potrero volverá a estar disponible para las operaciones permitidas.'}</p></div>}
    </ConfirmDialog>
  </div>
}
