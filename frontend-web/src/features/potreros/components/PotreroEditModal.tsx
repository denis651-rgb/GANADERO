import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import type { Propiedad } from '@/features/propiedades/api'
import { listSectores } from '@/features/propiedades/api'
import type { Potrero, TipoPasto, UpdatePotreroInput } from '@/features/potreros/api'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'

interface PotreroEditModalProps {
  potrero: Potrero
  properties: Propiedad[]
  grasses: TipoPasto[]
  online: boolean
  loading: boolean
  error: unknown
  onClose: () => void
  onSubmit: (input: UpdatePotreroInput) => void
  onReload: () => void
}

function optionalNumber(data: FormData, name: string) {
  const value = String(data.get(name) ?? '').trim()
  return value === '' ? undefined : Number(value)
}

export function PotreroEditModal({ potrero, properties, grasses, online, loading, error, onClose, onSubmit, onReload }: PotreroEditModalProps) {
  const [propertyId, setPropertyId] = useState(potrero.propiedadId)
  const [sectorId, setSectorId] = useState(potrero.sectorId ?? '')
  const [grassId, setGrassId] = useState(potrero.tipoPastoId ?? '')
  const sectors = useQuery({ queryKey: ['sectores', propertyId], queryFn: () => listSectores(propertyId), enabled: Boolean(propertyId) && online })
  const normalized = error ? normalizeApiError(error) : null
  const conflict = normalized?.code === 'VERSION_CONFLICT'

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!online || loading) return
    const data = new FormData(event.currentTarget)
    const superficieHa = optionalNumber(data, 'superficieHa')
    const capacidadUa = optionalNumber(data, 'capacidadUa')
    onSubmit({
      propiedadId: propertyId,
      sectorId: sectorId || undefined,
      quitarSector: !sectorId && Boolean(potrero.sectorId),
      codigo: String(data.get('codigo') ?? '').trim(),
      nombre: String(data.get('nombre') ?? '').trim(),
      superficieHa,
      quitarSuperficie: superficieHa === undefined && potrero.superficieHa !== undefined,
      tipoPastoId: grassId || undefined,
      quitarTipoPasto: !grassId && Boolean(potrero.tipoPastoId),
      capacidadUa,
      quitarCapacidad: capacidadUa === undefined && potrero.capacidadUa !== undefined,
      tieneAgua: data.get('tieneAgua') === 'on',
      estado: String(data.get('estado')) as Potrero['estado'],
      activo: potrero.activo,
      version: potrero.version,
    })
  }

  return <Modal open wide title={`Editar ${potrero.nombre}`} description="Actualiza la ubicación y capacidad operativa del potrero." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {!online && <Alert tone="info">Necesitas conexión para editar este potrero.</Alert>}
      {normalized && <Alert tone="danger" title={conflict ? 'El potrero cambió mientras lo editabas' : undefined}>{conflict ? 'Recarga la información antes de volver a guardar.' : normalized.message}</Alert>}
      <div className="form-grid">
        <Field label="Propiedad" required disabled={!online}><select name="propiedadId" required value={propertyId} disabled={!online} onChange={(event) => { setPropertyId(event.target.value); setSectorId('') }}>{properties.filter((item) => item.activo || item.id === potrero.propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Sector" disabled={!online}><select name="sectorId" value={sectorId} disabled={!online || sectors.isPending} onChange={(event) => setSectorId(event.target.value)}><option value="">Sin sector</option>{sectors.data?.filter((item) => item.activo || item.id === potrero.sectorId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Código" required disabled={!online}><input name="codigo" defaultValue={potrero.codigo} required maxLength={60} disabled={!online} /></Field>
        <Field label="Nombre" required disabled={!online}><input name="nombre" defaultValue={potrero.nombre} required maxLength={160} disabled={!online} /></Field>
        <Field label="Superficie (ha)" disabled={!online}><input name="superficieHa" type="number" inputMode="decimal" min="0" step="0.0001" defaultValue={potrero.superficieHa ?? ''} disabled={!online} /></Field>
        <Field label="Capacidad (UA)" disabled={!online}><input name="capacidadUa" type="number" inputMode="decimal" min="0" step="0.01" defaultValue={potrero.capacidadUa ?? ''} disabled={!online} /></Field>
        <Field label="Tipo de pasto" disabled={!online}><select name="tipoPastoId" value={grassId} disabled={!online} onChange={(event) => setGrassId(event.target.value)}><option value="">Sin especificar</option>{grasses.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Estado" disabled={!online}><select name="estado" defaultValue={potrero.estado} disabled={!online}><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select></Field>
        <label className="checkbox-line"><input name="tieneAgua" type="checkbox" defaultChecked={potrero.tieneAgua} disabled={!online} /> Tiene agua disponible</label>
      </div>
      {sectors.isPending && <LoadingState message="Cargando sectores…" />}
      {sectors.error && <Alert tone="danger">{normalizeApiError(sectors.error).message}</Alert>}
      <div className="form-actions"><Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>{conflict && <Button type="button" variant="secondary" onClick={onReload}>Recargar datos</Button>}<Button type="submit" loading={loading} disabled={!online || conflict || sectors.isPending}><Save size={17} aria-hidden="true" />Guardar potrero</Button></div>
    </form>
  </Modal>
}
