import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Network, Trash2 } from 'lucide-react'
import { crearParentesco, eliminarParentesco, listAnimals, listParentescos, listRazas } from '@/features/animales/api'
import type { Parentesco, TipoParentesco } from '@/features/animales/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { MobileEntityCard } from '@/shared/components/MobileEntityCard'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

export function GenealogiaTab({ animalId }: { animalId: string }) {
  const client = useQueryClient()
  const { showToast } = useToast()
  const [showForm, setShowForm] = useState(false)
  const [esExterno, setEsExterno] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<Parentesco | null>(null)
  const query = useQuery({ queryKey: ['animal-parentescos', animalId], queryFn: () => listParentescos(animalId), enabled: Boolean(animalId) })
  const catalogs = useQuery({ queryKey: ['genealogia-catalogs'], queryFn: async () => {
    const [razas, animales] = await Promise.all([listRazas(), listAnimals({ search: undefined, estado: '', sexo: '', page: 0, size: 500 })])
    return { razas, animales: animales.content }
  } })
  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return crearParentesco(animalId, {
        tipo: String(data.get('tipo')) as TipoParentesco,
        animalPadreId: esExterno ? undefined : (String(data.get('animalPadreId')) || undefined),
        nombreExterno: esExterno ? (String(data.get('nombreExterno')) || undefined) : undefined,
        razaExternaId: esExterno ? (String(data.get('razaExternaId')) || undefined) : undefined,
        registroGenealogico: esExterno ? (String(data.get('registroGenealogico')) || undefined) : undefined,
      })
    },
    onSuccess: async () => { setShowForm(false); showToast('Parentesco registrado correctamente.'); await client.invalidateQueries({ queryKey: ['animal-parentescos', animalId] }) },
  })
  const remove = useMutation({
    mutationFn: (parentescoId: string) => eliminarParentesco(animalId, parentescoId),
    onSuccess: async () => {
      setRemoveTarget(null)
      await client.invalidateQueries({ queryKey: ['animal-parentescos', animalId] })
    },
  })
  const error = query.error ?? catalogs.error ?? create.error ?? remove.error

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <Card>
      <div className="filter-heading"><span><Network size={18} />Genealogía</span><Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Cancelar' : 'Registrar progenitor'}</Button></div>
      {showForm && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(event.currentTarget) }}>
        <Field label="Tipo de progenitor"><select name="tipo" required><option value="MADRE">Madre</option><option value="PADRE">Padre</option></select></Field>
        <label className="checkbox-line"><input type="checkbox" checked={esExterno} onChange={(event) => setEsExterno(event.target.checked)} /> Progenitor externo (sin ficha en el sistema)</label>
        {esExterno ? <>
          <Field label="Nombre externo"><input name="nombreExterno" maxLength={160} placeholder="Ej. Toro de la Hacienda San Roque" /></Field>
          <Field label="Raza"><select name="razaExternaId"><option value="">Sin especificar</option>{catalogs.data?.razas.map((raza) => <option key={raza.id} value={raza.id}>{raza.nombre}</option>)}</select></Field>
          <Field label="Registro genealógico"><input name="registroGenealogico" maxLength={160} /></Field>
        </> : <>
          <Field label="Animal progenitor"><select name="animalPadreId" required><option value="">Selecciona un animal…</option>{catalogs.data?.animales.filter((animal) => animal.id !== animalId).map((animal) => <option key={animal.id} value={animal.id}>{animal.codigo}{animal.nombre ? ` · ${animal.nombre}` : ''}</option>)}</select></Field>
        </>}
        <div className="form-actions"><Button type="submit" loading={create.isPending}>Guardar parentesco</Button></div>
      </form>}
      {query.isPending && <LoadingState message="Cargando genealogía…" />}
      {query.data?.length === 0 && !showForm && <EmptyState title="Sin progenitores" description="Registra la madre o el padre del animal." />}
      {query.data && query.data.length > 0 && <><div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Relaciones genealógicas del animal</caption><thead><tr><th scope="col">Rol</th><th scope="col">Progenitor</th><th scope="col">Registro genealógico</th><th scope="col">Fecha</th><th scope="col">Acciones</th></tr></thead><tbody>{query.data.map((item) => <tr key={item.id}>
        <td><span className="status-badge">{item.tipo}</span></td>
        <td>{item.animalPadreId ? (() => { const padre = catalogs.data?.animales.find((animal) => animal.id === item.animalPadreId); return <strong>{padre ? `${padre.codigo}${padre.nombre ? ` · ${padre.nombre}` : ''}` : 'Animal registrado'}</strong> })() : <strong>{item.nombreExterno ?? 'Progenitor externo'}</strong>}{item.razaExternaId ? ` · ${catalogs.data?.razas.find((raza) => raza.id === item.razaExternaId)?.nombre ?? 'Raza'}` : ''}</td>
        <td>{item.registroGenealogico ?? '—'}</td>
        <td>{new Date(item.fechaRegistro).toLocaleDateString('es-BO')}</td>
        <td><Button variant="danger" loading={remove.isPending && remove.variables === item.id} onClick={() => setRemoveTarget(item)}><Trash2 size={16} />Eliminar</Button></td>
      </tr>)}</tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{query.data.map((item) => {
        const padre = item.animalPadreId ? catalogs.data?.animales.find((animal) => animal.id === item.animalPadreId) : undefined
        const progenitor = item.animalPadreId ? (padre ? `${padre.codigo}${padre.nombre ? ` · ${padre.nombre}` : ''}` : 'Animal registrado') : (item.nombreExterno ?? 'Progenitor externo')
        const raza = item.razaExternaId ? catalogs.data?.razas.find((value) => value.id === item.razaExternaId)?.nombre : undefined
        return <MobileEntityCard key={item.id} title={progenitor} status={<span className="status-badge">{item.tipo}</span>} subtitle={raza} metadata={<><span>Registro: {item.registroGenealogico ?? 'Sin registro'}</span><span>Registrado: {new Date(item.fechaRegistro).toLocaleDateString('es-BO')}</span></>} action={<Button variant="danger" loading={remove.isPending && remove.variables === item.id} onClick={() => setRemoveTarget(item)}><Trash2 size={16} aria-hidden="true" />Eliminar</Button>} />
      })}</div></div></>}
    </Card>

    <ConfirmDialog
      open={Boolean(removeTarget)}
      title="Eliminar parentesco"
      confirmLabel="Eliminar parentesco"
      confirmIcon={<Trash2 size={16} />}
      loading={remove.isPending}
      error={remove.error}
      onClose={() => setRemoveTarget(null)}
      onConfirm={() => { if (removeTarget) remove.mutate(removeTarget.id) }}
    >
      {removeTarget && (
        <>
          <p className="muted">
            ¿Eliminar el parentesco de <strong>{removeTarget.tipo === 'MADRE' ? 'madre' : 'padre'}</strong>:{' '}
            <strong>{removeTarget.animalPadreId
              ? (() => { const padre = catalogs.data?.animales.find((animal) => animal.id === removeTarget.animalPadreId); return padre ? `${padre.codigo}${padre.nombre ? ` · ${padre.nombre}` : ''}` : 'Animal registrado' })()
              : (removeTarget.nombreExterno ?? 'Progenitor externo')}</strong>?
          </p>
          <p className="muted">Esta acción no se puede deshacer.</p>
        </>
      )}
    </ConfirmDialog>
  </div>
}
