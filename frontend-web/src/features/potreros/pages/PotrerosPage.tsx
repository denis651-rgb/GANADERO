import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { createPotrero, listPotreros, listTiposPasto, updatePotrero, type Potrero } from '@/features/potreros/api'
import { listPropiedades, listSectores } from '@/features/propiedades/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function PotrerosPage() {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [propertyId, setPropertyId] = useState('')
  const [stateTarget, setStateTarget] = useState<{ potrero: Potrero; estado: Potrero['estado'] } | null>(null)
  const paddocks = useQuery({ queryKey: ['potreros'], queryFn: listPotreros })
  const catalogs = useQuery({ queryKey: ['potrero-catalogos'], queryFn: async () => { const [properties, grasses] = await Promise.all([listPropiedades(), listTiposPasto()]); return { properties, grasses } } })
  const sectors = useQuery({ queryKey: ['sectores', propertyId], queryFn: () => listSectores(propertyId), enabled: Boolean(propertyId) })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createPotrero({ propiedadId: String(data.get('propiedadId')), sectorId: String(data.get('sectorId') || '') || undefined, codigo: String(data.get('codigo')), nombre: String(data.get('nombre')), superficieHa: Number(data.get('superficieHa')) || undefined, tipoPastoId: String(data.get('tipoPastoId') || '') || undefined, capacidadUa: Number(data.get('capacidadUa')) || undefined, tieneAgua: data.get('tieneAgua') === 'on', estado: String(data.get('estado')) as 'DISPONIBLE' | 'OCUPADO' | 'DESCANSO' | 'MANTENIMIENTO' })
    },
    onSuccess: () => { setShowForm(false); client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const changeState = useMutation({
    mutationFn: ({ id, estado, version }: { id: string; estado: 'DISPONIBLE' | 'OCUPADO' | 'DESCANSO' | 'MANTENIMIENTO'; version: number }) => updatePotrero(id, { estado, version }),
    onSuccess: () => { setStateTarget(null); client.invalidateQueries({ queryKey: ['potreros'] }) },
  })
  const error = paddocks.error ?? catalogs.error ?? sectors.error ?? create.error ?? changeState.error

  return <div className="page-stack">
    <PageHeader eyebrow="Campo" title="Potreros" description="Controla capacidad, pastura, agua y disponibilidad." actions={<Button onClick={() => setShowForm((value) => !value)}><Plus size={18} />Nuevo potrero</Button>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {showForm && <Card><form className="form-grid" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
      <Field label="Propiedad"><select name="propiedadId" required value={propertyId} onChange={(event) => setPropertyId(event.target.value)}><option value="">Selecciona…</option>{catalogs.data?.properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Sector"><select name="sectorId"><option value="">Sin sector</option>{sectors.data?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Código"><input name="codigo" required /></Field><Field label="Nombre"><input name="nombre" required /></Field>
      <Field label="Superficie (ha)"><input name="superficieHa" type="number" min="0" step="0.0001" /></Field><Field label="Capacidad (UA)"><input name="capacidadUa" type="number" min="0" step="0.01" /></Field>
      <Field label="Tipo de pasto"><select name="tipoPastoId"><option value="">Sin especificar</option>{catalogs.data?.grasses.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
      <Field label="Estado"><select name="estado" defaultValue="DISPONIBLE"><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select></Field>
      <label className="checkbox-line"><input name="tieneAgua" type="checkbox" /> Tiene agua</label><div className="form-actions"><Button type="submit" loading={create.isPending}>Crear potrero</Button></div>
    </form></Card>}
    <Card>{paddocks.isPending && <LoadingState message="Consultando potreros…" />}{paddocks.data?.length === 0 && <EmptyState title="No hay potreros" description="Crea una propiedad y registra su primer potrero." />}
      {paddocks.data && paddocks.data.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Potrero</th><th>Propiedad</th><th>Superficie</th><th>Capacidad</th><th>Agua</th><th>Estado</th></tr></thead><tbody>{paddocks.data.map((item) => <tr key={item.id}><td><strong>{item.codigo}</strong><span className="table-secondary">{item.nombre}</span></td><td>{catalogs.data?.properties.find((property) => property.id === item.propiedadId)?.nombre ?? item.propiedadId}</td><td>{item.superficieHa ? `${item.superficieHa} ha` : '—'}</td><td>{item.capacidadUa ?? '—'}</td><td>{item.tieneAgua ? 'Sí' : 'No'}</td><td><select aria-label={`Estado de ${item.nombre}`} value={item.estado} disabled={changeState.isPending} onChange={(event) => setStateTarget({ potrero: item, estado: event.target.value as Potrero['estado'] })}><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select></td></tr>)}</tbody></table></div>}
    </Card>
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
  </div>
}
