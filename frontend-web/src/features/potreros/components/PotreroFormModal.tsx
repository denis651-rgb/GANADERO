import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import type { Propiedad } from '@/features/propiedades/api'
import { listSectores } from '@/features/propiedades/api'
import type { TipoPasto } from '@/features/potreros/api'
import { calcularCapacidadRecomendada, cargaRecomendadaUaHa } from '@/features/potreros/capacidad'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'

interface PotreroFormModalProps {
  properties: Propiedad[]
  grasses: TipoPasto[]
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (form: HTMLFormElement) => void
}

export function PotreroFormModal({ properties, grasses, loading, error, onClose, onSubmit }: PotreroFormModalProps) {
  const [propertyId, setPropertyId] = useState('')
  const [sectorId, setSectorId] = useState('')
  const [superficieHa, setSuperficieHa] = useState('')
  const [grassId, setGrassId] = useState('')
  const [tieneAgua, setTieneAgua] = useState(false)
  const pasto = grasses.find((item) => item.id === grassId)
  const capacidad = calcularCapacidadRecomendada(Number(superficieHa), pasto, tieneAgua)
  const sectors = useQuery({ queryKey: ['sectores', propertyId], queryFn: () => listSectores(propertyId), enabled: Boolean(propertyId) })
  // Con la query deshabilitada (sin propiedad elegida), TanStack Query v5 reporta isPending=true
  // aunque no esté cargando nada: por eso el spinner se guía por fetchStatus, no por isPending.
  const loadingSectors = Boolean(propertyId) && sectors.fetchStatus === 'fetching'
  const normalized = error ? normalizeApiError(error) : null

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (loading) return
    onSubmit(event.currentTarget)
  }

  return <Modal open wide title="Nuevo potrero" description="Registra un potrero y su capacidad operativa." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger">{normalized.message}</Alert>}
      <div className="form-grid">
        <Field label="Propiedad" required><select name="propiedadId" required value={propertyId} onChange={(event) => { setPropertyId(event.target.value); setSectorId('') }}><option value="">Selecciona…</option>{properties.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Sector"><select name="sectorId" value={sectorId} disabled={!propertyId || loadingSectors} onChange={(event) => setSectorId(event.target.value)}><option value="">Sin sector</option>{sectors.data?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Nombre" required><input name="nombre" required maxLength={160} /></Field>
        <Field label="Superficie (ha)"><input name="superficieHa" type="number" inputMode="decimal" min="0" step="0.0001" value={superficieHa} onChange={(event) => setSuperficieHa(event.target.value)} /></Field>
        <Field label="Capacidad recomendada (UA)" hint={capacidad === undefined ? 'Selecciona superficie y tipo de pasto.' : `${cargaRecomendadaUaHa(pasto)} UA/ha · ${tieneAgua ? 'con agua' : 'factor 70% sin agua'}`}><input name="capacidadUa" type="number" value={capacidad ?? ''} readOnly /></Field>
        <Field label="Tipo de pasto"><select name="tipoPastoId" value={grassId} onChange={(event) => setGrassId(event.target.value)}><option value="">Sin especificar</option>{grasses.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Estado"><select name="estado" defaultValue="DISPONIBLE"><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select></Field>
        <label className="checkbox-line"><input name="tieneAgua" type="checkbox" checked={tieneAgua} onChange={(event) => setTieneAgua(event.target.checked)} /> Tiene agua</label>
      </div>
      {loadingSectors && <LoadingState message="Cargando sectores…" />}
      {sectors.error && <Alert tone="danger">{normalizeApiError(sectors.error).message}</Alert>}
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button type="submit" loading={loading}><Save size={17} aria-hidden="true" />Crear potrero</Button>
      </div>
    </form>
  </Modal>
}
