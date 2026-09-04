import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import type { Propiedad } from '@/features/propiedades/api'
import { listSectores } from '@/features/propiedades/api'
import type { Potrero, TipoPasto, UpdatePotreroInput } from '@/features/potreros/api'
import { calcularCapacidadRecomendada, cargaRecomendadaUaHa } from '@/features/potreros/capacidad'
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

export function PotreroEditModal({ potrero, properties, grasses, loading, error, onClose, onSubmit, onReload }: PotreroEditModalProps) {
  const [propertyId, setPropertyId] = useState(potrero.propiedadId)
  const [sectorId, setSectorId] = useState(potrero.sectorId ?? '')
  const [grassId, setGrassId] = useState(potrero.tipoPastoId ?? '')
  const [superficieValue, setSuperficieValue] = useState(potrero.superficieHa?.toString() ?? '')
  const [tieneAgua, setTieneAgua] = useState(potrero.tieneAgua)
  const pasto = grasses.find((item) => item.id === grassId)
  const capacidadRecomendada = calcularCapacidadRecomendada(Number(superficieValue), pasto, tieneAgua)
  const sectors = useQuery({ queryKey: ['sectores', propertyId], queryFn: () => listSectores(propertyId), enabled: Boolean(propertyId) })
  const normalized = error ? normalizeApiError(error) : null
  const conflict = normalized?.code === 'VERSION_CONFLICT'

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (loading) return
    const data = new FormData(event.currentTarget)
    const superficieHa = optionalNumber(data, 'superficieHa')
    const capacidadUa = capacidadRecomendada
    onSubmit({
      propiedadId: propertyId,
      sectorId: sectorId || undefined,
      quitarSector: !sectorId && Boolean(potrero.sectorId),
      nombre: String(data.get('nombre') ?? '').trim(),
      superficieHa,
      quitarSuperficie: superficieHa === undefined && potrero.superficieHa !== undefined,
      tipoPastoId: grassId || undefined,
      quitarTipoPasto: !grassId && Boolean(potrero.tipoPastoId),
      capacidadUa,
      quitarCapacidad: capacidadUa === undefined && potrero.capacidadUa !== undefined,
      tieneAgua,
      estado: String(data.get('estado')) as Potrero['estado'],
      activo: potrero.activo,
      version: potrero.version,
    })
  }

  return <Modal open wide title={`Editar ${potrero.nombre}`} description="Actualiza la ubicación y capacidad operativa del potrero." onClose={onClose}>
    <form className="page-stack" onSubmit={submit}>
      {normalized && <Alert tone="danger" title={conflict ? 'El potrero cambió mientras lo editabas' : undefined}>{conflict ? 'Recarga la información antes de volver a guardar.' : normalized.message}</Alert>}
      <div className="form-grid">
        <Field label="Propiedad" required><select name="propiedadId" required value={propertyId} onChange={(event) => { setPropertyId(event.target.value); setSectorId('') }}>{properties.filter((item) => item.activo || item.id === potrero.propiedadId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Sector"><select name="sectorId" value={sectorId} disabled={sectors.isPending} onChange={(event) => setSectorId(event.target.value)}><option value="">Sin sector</option>{sectors.data?.filter((item) => item.activo || item.id === potrero.sectorId).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Código" hint="Identificador interno permanente"><input value={potrero.codigo} readOnly /></Field>
        <Field label="Nombre" required><input name="nombre" defaultValue={potrero.nombre} required maxLength={160} /></Field>
        <Field label="Superficie (ha)"><input name="superficieHa" type="number" inputMode="decimal" min="0" step="0.0001" value={superficieValue} onChange={(event) => setSuperficieValue(event.target.value)} /></Field>
        <Field label="Capacidad recomendada (UA)" hint={capacidadRecomendada === undefined ? 'Selecciona superficie y tipo de pasto.' : `${cargaRecomendadaUaHa(pasto)} UA/ha · ${tieneAgua ? 'con agua' : 'factor 70% sin agua'}`}><input name="capacidadUa" type="number" value={capacidadRecomendada ?? ''} readOnly /></Field>
        <Field label="Tipo de pasto"><select name="tipoPastoId" value={grassId} onChange={(event) => setGrassId(event.target.value)}><option value="">Sin especificar</option>{grasses.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
        <Field label="Estado"><select name="estado" defaultValue={potrero.estado}><option>DISPONIBLE</option><option>OCUPADO</option><option>DESCANSO</option><option>MANTENIMIENTO</option></select></Field>
        <label className="checkbox-line"><input name="tieneAgua" type="checkbox" checked={tieneAgua} onChange={(event) => setTieneAgua(event.target.checked)} /> Tiene agua disponible</label>
      </div>
      {sectors.isPending && <LoadingState message="Cargando sectores…" />}
      {sectors.error && <Alert tone="danger">{normalizeApiError(sectors.error).message}</Alert>}
      <div className="form-actions"><Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>{conflict && <Button type="button" variant="secondary" onClick={onReload}>Recargar datos</Button>}<Button type="submit" loading={loading} disabled={conflict || sectors.isPending}><Save size={17} aria-hidden="true" />Guardar potrero</Button></div>
    </form>
  </Modal>
}
