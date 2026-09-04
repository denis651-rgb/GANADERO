import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, MapPinned, Pencil, Plus, Power, Save } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { PropiedadFormModal } from '@/features/propiedades/components/PropiedadFormModal'
import { SectorAddModal } from '@/features/propiedades/components/SectorAddModal'
import { SectorEditModal } from '@/features/propiedades/components/SectorEditModal'
import { createPropiedad, createSector, listPropiedades, listSectores, updatePropiedad, updateSector, type Propiedad, type Sector } from '@/features/propiedades/api'
import { getConfiguracion, updateConfiguracion } from '@/features/configuracion/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function PropiedadesPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const [showForm, setShowForm] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [toggleTarget, setToggleTarget] = useState<Propiedad | null>(null)
  const [showAddSector, setShowAddSector] = useState(false)
  const [editSector, setEditSector] = useState<Sector | null>(null)
  const [toggleSectorTarget, setToggleSectorTarget] = useState<Sector | null>(null)
  const [pinMessage, setPinMessage] = useState<string | null>(null)

  const query = useQuery({ queryKey: ['propiedades'], queryFn: listPropiedades })
  const sectors = useQuery({ queryKey: ['sectores', selectedId], queryFn: () => listSectores(selectedId!), enabled: Boolean(selectedId) })
  const config = useQuery({ queryKey: ['configuracion'], queryFn: getConfiguracion })

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createPropiedad({ nombre: String(data.get('nombre')), departamento: String(data.get('departamento') ?? ''), municipio: String(data.get('municipio') ?? ''), superficieHa: Number(data.get('superficieHa')) || undefined })
    },
    onSuccess: () => { setShowForm(false); client.invalidateQueries({ queryKey: ['propiedades'] }) },
  })
  const toggle = useMutation({
    mutationFn: ({ id, activo, version }: { id: string; activo: boolean; version: number }) => updatePropiedad(id, { activo, version }),
    onSuccess: () => { setToggleTarget(null); client.invalidateQueries({ queryKey: ['propiedades'] }) },
  })
  const addSector = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return createSector(selectedId!, { nombre: String(data.get('nombre')), descripcion: String(data.get('descripcion') ?? '') })
    },
    onSuccess: () => { setShowAddSector(false); client.invalidateQueries({ queryKey: ['sectores', selectedId] }) },
  })
  const editSectorMutation = useMutation({
    mutationFn: ({ id, input }: { id: string; input: Parameters<typeof updateSector>[1] }) => updateSector(id, input),
    onSuccess: () => { setEditSector(null); void client.invalidateQueries({ queryKey: ['sectores', selectedId] }) },
  })
  const toggleSectorMutation = useMutation({
    mutationFn: (sector: Sector) => updateSector(sector.id, { codigo: sector.codigo, nombre: sector.nombre, descripcion: sector.descripcion, activo: !sector.activo, version: sector.version }),
    onSuccess: () => { setToggleSectorTarget(null); void client.invalidateQueries({ queryKey: ['sectores', selectedId] }) },
  })
  const saveConfig = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return updateConfiguracion({
        unidadPeso: 'KG',
        unidadSuperficie: String(data.get('unidadSuperficie') ?? '') || undefined,
        diasAlertaPreparto: Number(data.get('diasAlertaPreparto')),
        diasAlertaVacunacion: Number(data.get('diasAlertaVacunacion')),
        diasSinPesaje: Number(data.get('diasSinPesaje')),
        diasAlertaDestete: Number(data.get('diasAlertaDestete')),
        diasDiagnosticoPostServicio: Number(data.get('diasDiagnosticoPostServicio')),
        diasGestacionEstimada: Number(data.get('diasGestacionEstimada')),
        comprimirImagenes: data.get('comprimirImagenes') === 'on',
        nombreUsuario: String(data.get('nombreUsuario') ?? '') || undefined,
        version: config.data!.version,
      })
    },
    onSuccess: () => { void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })
  const savePin = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const pin = String(data.get('nuevoPin') ?? '')
      const confirmacion = String(data.get('confirmacionPin') ?? '')
      if (pin !== confirmacion) throw new Error('El PIN y su confirmación no coinciden.')
      return updateConfiguracion({ nuevoPin: pin, version: config.data!.version })
    },
    onSuccess: (_data, form) => { form.reset(); setPinMessage('PIN actualizado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })
  const removePin = useMutation({
    mutationFn: () => updateConfiguracion({ quitarPin: true, version: config.data!.version }),
    onSuccess: () => { setPinMessage('PIN eliminado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })

  const error = query.error ?? create.error ?? toggle.error ?? sectors.error ?? addSector.error ?? toggleSectorMutation.error ?? config.error ?? saveConfig.error
  const canEditSectors = can('PROPIEDAD_EDITAR')
  const canCreateSectors = can('PROPIEDAD_CREAR')
  const canEditConfig = can('CONFIGURACION_EDITAR')

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Campo" title="Propiedades" description="Administra las parcelas de la operación y sus sectores." actions={<Button onClick={() => setShowForm(true)}><Plus size={18} />Nueva propiedad</Button>} />
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      <Card>
        {query.isPending && <LoadingState message="Consultando propiedades…" />}
        {query.data?.length === 0 && <EmptyState title="No hay propiedades" description="Registra el primer establecimiento de la operación." />}
        {query.data && query.data.length > 0 && <><div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Propiedades registradas</caption><thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Ubicación</th><th scope="col" className="numeric">Superficie</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>
          {query.data.map((property) => <tr key={property.id} className={selectedId === property.id ? 'selected-row' : undefined}>
            <td><strong>{property.codigo}</strong></td><td>{property.nombre}</td><td>{[property.departamento, property.municipio].filter(Boolean).join(' / ') || '—'}</td><td className="numeric">{property.superficieHa ? `${property.superficieHa} ha` : '—'}</td><td><span className={`status-badge ${property.activo ? 'status-activo' : 'status-inactivo'}`}>{property.activo ? 'ACTIVA' : 'INACTIVA'}</span></td>
            <td><div className="inline-actions"><Button variant="ghost" onClick={() => setSelectedId(property.id)}><MapPinned size={16} />Sectores</Button><Button variant="ghost" loading={toggle.isPending && toggleTarget?.id === property.id} onClick={() => setToggleTarget(property)}><Power size={16} />{property.activo ? 'Desactivar' : 'Activar'}</Button></div></td>
          </tr>)}
        </tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{query.data.map((property) => <MobileEntityCard key={property.id} title={`${property.codigo} · ${property.nombre}`} status={<span className={`status-badge ${property.activo ? 'status-activo' : 'status-inactivo'}`}>{property.activo ? 'ACTIVA' : 'INACTIVA'}</span>} metadata={<><span>{[property.departamento, property.municipio].filter(Boolean).join(' / ') || 'Ubicación no registrada'}</span><span>{property.superficieHa ? `${property.superficieHa} ha` : 'Superficie no registrada'}</span></>} action={<><Button variant="ghost" onClick={() => setSelectedId(property.id)}>Sectores</Button><Button variant="ghost" onClick={() => setToggleTarget(property)}>{property.activo ? 'Desactivar' : 'Activar'}</Button></>} />)}</div></div></>}
      </Card>
      {selectedId && <Card><div className="section-heading"><h3>Sectores de {query.data?.find((item) => item.id === selectedId)?.nombre}</h3>{canCreateSectors && <Button variant="secondary" onClick={() => setShowAddSector(true)}><Plus size={16} aria-hidden="true" />Añadir sector</Button>}</div>
        {sectors.isPending && <LoadingState message="Cargando sectores…" />}
        {sectors.data?.length === 0 && <EmptyState title="Sin sectores" description="Añade el primer sector de esta propiedad." />}
        {sectors.data && sectors.data.length > 0 && <><div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Sectores de la propiedad seleccionada</caption><thead><tr><th scope="col">Sector</th><th scope="col">Descripción</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{sectors.data.map((sector) => <tr key={sector.id}><td><strong>{sector.codigo}</strong><span className="table-secondary">{sector.nombre}</span></td><td>{sector.descripcion || '—'}</td><td><span className={`status-badge ${sector.activo ? 'status-activo' : 'status-inactivo'}`}>{sector.activo ? 'ACTIVO' : 'INACTIVO'}</span></td><td>{canEditSectors && <div className="inline-actions"><Button variant="ghost" onClick={() => setEditSector(sector)}><Pencil size={16} aria-hidden="true" />Editar</Button><Button variant="ghost" onClick={() => setToggleSectorTarget(sector)}><Power size={16} aria-hidden="true" />{sector.activo ? 'Desactivar' : 'Activar'}</Button></div>}</td></tr>)}</tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{sectors.data.map((sector) => <MobileEntityCard key={sector.id} title={`${sector.codigo} · ${sector.nombre}`} status={<span className={`status-badge ${sector.activo ? 'status-activo' : 'status-inactivo'}`}>{sector.activo ? 'ACTIVO' : 'INACTIVO'}</span>} metadata={<span>{sector.descripcion || 'Sin descripción'}</span>} action={canEditSectors ? <><Button variant="ghost" onClick={() => setEditSector(sector)}>Editar</Button><Button variant="ghost" onClick={() => setToggleSectorTarget(sector)}>{sector.activo ? 'Desactivar' : 'Activar'}</Button></> : undefined} />)}</div></div></>}
      </Card>}

      {config.data && <Card>
        <h3>Configuración general</h3>
        <form className="form-grid" onSubmit={(event) => { event.preventDefault(); saveConfig.mutate(event.currentTarget) }}>
          <Field label="Zona horaria" disabled><input value={config.data.zonaHoraria} disabled /></Field>
          <Field label="Moneda" disabled><input value={config.data.moneda} disabled /></Field>
          <Field label="Unidad de peso" disabled><select value="KG" disabled><option value="KG">Kilogramos (KG)</option></select></Field>
          <Field label="Unidad de superficie" disabled={!canEditConfig}><select name="unidadSuperficie" defaultValue={config.data.unidadSuperficie} disabled={!canEditConfig}><option value="HA">Hectáreas (HA)</option><option value="M2">Metros cuadrados (M2)</option><option value="ACRE">Acres</option></select></Field>
          <Field label="Días de alerta antes del parto" disabled={!canEditConfig}><input name="diasAlertaPreparto" type="number" min="0" defaultValue={config.data.diasAlertaPreparto} disabled={!canEditConfig} /></Field>
          <Field label="Días de alerta de vacunación" disabled={!canEditConfig}><input name="diasAlertaVacunacion" type="number" min="0" defaultValue={config.data.diasAlertaVacunacion} disabled={!canEditConfig} /></Field>
          <Field label="Días sin pesaje para alertar" disabled={!canEditConfig}><input name="diasSinPesaje" type="number" min="0" defaultValue={config.data.diasSinPesaje} disabled={!canEditConfig} /></Field>
          <Field label="Días de alerta de destete" disabled={!canEditConfig}><input name="diasAlertaDestete" type="number" min="0" defaultValue={config.data.diasAlertaDestete} disabled={!canEditConfig} /></Field>
          <Field label="Días para diagnóstico post-servicio" disabled={!canEditConfig}><input name="diasDiagnosticoPostServicio" type="number" min="0" defaultValue={config.data.diasDiagnosticoPostServicio} disabled={!canEditConfig} /></Field>
          <Field label="Días de gestación estimada" disabled={!canEditConfig}><input name="diasGestacionEstimada" type="number" min="1" defaultValue={config.data.diasGestacionEstimada} disabled={!canEditConfig} /></Field>
          <Field label="Calidad de imagen (1-100)" disabled><input type="number" value={config.data.calidadImagen} disabled /></Field>
          <Field label="Nombre de usuario" hint="Se muestra en la aplicación." disabled={!canEditConfig}><input name="nombreUsuario" maxLength={120} defaultValue={config.data.nombreUsuario ?? ''} disabled={!canEditConfig} /></Field>
          <label className="checkbox-line"><input name="comprimirImagenes" type="checkbox" defaultChecked={config.data.comprimirImagenes} disabled={!canEditConfig} /> Comprimir fotos al subirlas</label>
          {canEditConfig && <div className="form-actions form-full"><Button type="submit" loading={saveConfig.isPending}><Save size={17} aria-hidden="true" />Guardar configuración</Button></div>}
        </form>
      </Card>}

      {canEditConfig && config.data && <Card>
        <h3><KeyRound size={17} aria-hidden="true" /> Bloqueo con PIN</h3>
        {config.data.pinConfigurado ? <p className="muted">La aplicación tiene un PIN configurado.</p> : <Alert tone="warning">Ningún PIN configurado: cualquiera puede abrir la aplicación.</Alert>}
        {pinMessage && <Alert tone="success">{pinMessage}</Alert>}
        {savePin.error && <Alert tone="danger">{normalizeApiError(savePin.error).message}</Alert>}
        <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); setPinMessage(null); savePin.mutate(event.currentTarget) }}>
          <Field label="Nuevo PIN"><input name="nuevoPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <Field label="Confirmar PIN"><input name="confirmacionPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <div className="form-actions">
            <Button type="submit" loading={savePin.isPending}>{config.data.pinConfigurado ? 'Cambiar PIN' : 'Configurar PIN'}</Button>
            {config.data.pinConfigurado && <Button type="button" variant="secondary" loading={removePin.isPending} onClick={() => { setPinMessage(null); removePin.mutate() }}>Quitar PIN</Button>}
          </div>
        </form>
      </Card>}

      <ConfirmDialog
        open={Boolean(toggleTarget)}
        title={toggleTarget?.activo ? 'Desactivar propiedad' : 'Activar propiedad'}
        confirmLabel={toggleTarget?.activo ? 'Desactivar propiedad' : 'Activar propiedad'}
        variant={toggleTarget?.activo ? 'danger' : 'warning'}
        loading={toggle.isPending}
        error={toggle.error}
        onClose={() => setToggleTarget(null)}
        onConfirm={() => { if (toggleTarget && !toggle.isPending) toggle.mutate({ id: toggleTarget.id, activo: !toggleTarget.activo, version: toggleTarget.version }) }}
      >
        {toggleTarget && <div className="page-stack"><dl className="detail-list">
          <div><dt>Propiedad</dt><dd>{toggleTarget.codigo} · {toggleTarget.nombre}</dd></div>
          <div><dt>Estado actual</dt><dd>{toggleTarget.activo ? 'ACTIVA' : 'INACTIVA'}</dd></div>
          <div><dt>Estado nuevo</dt><dd>{toggleTarget.activo ? 'INACTIVA' : 'ACTIVA'}</dd></div>
        </dl><p className="muted">{toggleTarget.activo ? 'La propiedad dejará de estar disponible para nuevas operaciones mientras permanezca inactiva.' : 'La propiedad volverá a estar disponible para las operaciones permitidas.'}</p></div>}
      </ConfirmDialog>
      <PropiedadFormModal open={showForm} loading={create.isPending} error={create.error} onClose={() => setShowForm(false)} onSubmit={(form) => create.mutate(form)} />
      <SectorAddModal open={showAddSector} loading={addSector.isPending} error={addSector.error} onClose={() => setShowAddSector(false)} onSubmit={(form) => addSector.mutate(form)} />
      <SectorEditModal sector={editSector} loading={editSectorMutation.isPending} error={editSectorMutation.error} onClose={() => setEditSector(null)} onSubmit={(input) => { if (editSector) editSectorMutation.mutate({ id: editSector.id, input }) }} onReload={() => { setEditSector(null); void client.invalidateQueries({ queryKey: ['sectores', selectedId] }) }} />
      <ConfirmDialog open={Boolean(toggleSectorTarget)} title={toggleSectorTarget?.activo ? 'Desactivar sector' : 'Activar sector'} confirmLabel={toggleSectorTarget?.activo ? 'Desactivar sector' : 'Activar sector'} variant={toggleSectorTarget?.activo ? 'danger' : 'warning'} loading={toggleSectorMutation.isPending} error={toggleSectorMutation.error} onClose={() => setToggleSectorTarget(null)} onConfirm={() => { if (toggleSectorTarget && !toggleSectorMutation.isPending) toggleSectorMutation.mutate(toggleSectorTarget) }}>
        {toggleSectorTarget && <div className="page-stack"><dl className="detail-list"><div><dt>Sector</dt><dd>{toggleSectorTarget.codigo} · {toggleSectorTarget.nombre}</dd></div><div><dt>Estado nuevo</dt><dd>{toggleSectorTarget.activo ? 'INACTIVO' : 'ACTIVO'}</dd></div></dl><p className="muted">Los potreros existentes conservarán su relación con el sector. Un sector inactivo no estará disponible para nuevas asignaciones.</p></div>}
      </ConfirmDialog>
    </div>
  )
}
