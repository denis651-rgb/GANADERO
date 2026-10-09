import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Hash } from 'lucide-react'
import { asignarIdentificador, hacerPrincipalIdentificador, listIdentificadores, retirarIdentificador } from '@/features/animales/api'
import type { Identificador, TipoIdentificador } from '@/features/animales/types'
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

const tiposManuales: TipoIdentificador[] = ['ARETE', 'RFID', 'TATUAJE', 'OTRO']

export function IdentificadoresTab({ animalId }: { animalId: string; animalCodigo: string }) {
  const client = useQueryClient()
  const { showToast } = useToast()
  const [showForm, setShowForm] = useState(false)
  const [retireTarget, setRetireTarget] = useState<Identificador | null>(null)
  const [retireMotivo, setRetireMotivo] = useState('')
  const query = useQuery({ queryKey: ['animal-identificadores', animalId], queryFn: () => listIdentificadores(animalId), enabled: Boolean(animalId) })
  const assign = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return asignarIdentificador(animalId, {
        tipo: String(data.get('tipo')) as TipoIdentificador,
        valor: String(data.get('valor')),
        principal: data.get('principal') === 'on',
        observaciones: String(data.get('observaciones') ?? '') || undefined,
      })
    },
    onSuccess: async () => { setShowForm(false); showToast('Identificador asignado correctamente.'); await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const retire = useMutation({
    mutationFn: ({ id, version, motivo }: { id: string; version: number; motivo: string }) => retirarIdentificador(animalId, id, motivo, version),
    onSuccess: async () => {
      setRetireTarget(null)
      setRetireMotivo('')
      showToast('Identificador retirado correctamente.')
      await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] })
    },
  })
  const makePrincipal = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => hacerPrincipalIdentificador(animalId, id, version),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const error = query.error ?? assign.error ?? retire.error ?? makePrincipal.error
  const activePrincipal = query.data?.find((item) => item.principal && item.estado === 'ACTIVO')

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {query.data && query.data.length > 0 && !activePrincipal && <Alert>Este animal no tiene un identificador principal activo.</Alert>}
    <Card>
      <div className="filter-heading">
        <span><Hash size={18} />Identificadores</span>
        <div className="row-actions">
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Cancelar' : 'Asignar identificador'}</Button>
        </div>
      </div>
      {showForm && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); assign.mutate(event.currentTarget) }}>
        <Field label="Tipo"><select name="tipo" required defaultValue="ARETE">{tiposManuales.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
        <Field label="Valor"><input name="valor" required maxLength={120} placeholder="Número o código del identificador" /></Field>
        <label className="checkbox-line"><input name="principal" type="checkbox" defaultChecked /> Identificador principal</label>
        <Field label="Observaciones"><input name="observaciones" maxLength={1000} /></Field>
        <div className="form-actions"><Button type="submit" loading={assign.isPending}>Guardar identificador</Button></div>
      </form>}
      {query.isPending && <LoadingState message="Cargando identificadores…" />}
      {query.data?.length === 0 && !showForm && <EmptyState title="Sin identificadores" description="Asigna el primer identificador del animal." />}
      {query.data && query.data.length > 0 && <><div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Identificadores del animal</caption><thead><tr><th scope="col">Tipo</th><th scope="col">Valor</th><th scope="col">Estado</th><th scope="col">Asignación</th><th scope="col">Retiro</th><th scope="col">Acciones</th></tr></thead><tbody>{query.data.map((item) => <tr key={item.id}>
        <td><strong>{item.tipo}</strong></td><td>{item.valor}{item.principal && ' '}<span className="status-badge">Principal</span></td>
        <td><span className="status-badge">{item.estado}</span></td>
        <td>{new Date(item.fechaAsignacion).toLocaleDateString('es-BO')}</td>
        <td>{item.fechaRetiro ? `${new Date(item.fechaRetiro).toLocaleDateString('es-BO')}${item.motivoRetiro ? ` · ${item.motivoRetiro}` : ''}` : '—'}</td>
        <td><div className="row-actions">
          {item.estado === 'ACTIVO' && <>
            {!item.principal && <Button variant="ghost" onClick={() => makePrincipal.mutate({ id: item.id, version: item.version })}>Hacer principal</Button>}
            <Button variant="danger" loading={retire.isPending && retire.variables?.id === item.id} onClick={() => { setRetireTarget(item); setRetireMotivo('') }}>Retirar</Button>
          </>}
        </div></td>
      </tr>)}</tbody></table></div><div className="mobile-only"><div className="mobile-entity-list">{query.data.map((item) => <MobileEntityCard key={item.id} title={item.valor} subtitle={item.tipo} status={<span className="status-badge">{item.estado}</span>} metadata={<><span>{item.principal ? 'Identificador principal' : 'Identificador secundario'}</span><span>Asignado: {new Date(item.fechaAsignacion).toLocaleDateString('es-BO')}</span>{item.fechaRetiro && <span>Retirado: {new Date(item.fechaRetiro).toLocaleDateString('es-BO')}{item.motivoRetiro ? ` · ${item.motivoRetiro}` : ''}</span>}</>} action={<>{item.estado === 'ACTIVO' && <>{!item.principal && <Button variant="ghost" onClick={() => makePrincipal.mutate({ id: item.id, version: item.version })}>Hacer principal</Button>}<Button variant="danger" loading={retire.isPending && retire.variables?.id === item.id} onClick={() => { setRetireTarget(item); setRetireMotivo('') }}>Retirar</Button></>}</>} />)}</div></div></>}
    </Card>

    <ConfirmDialog
      open={Boolean(retireTarget)}
      title="Retirar identificador"
      confirmLabel="Retirar identificador"
      loading={retire.isPending}
      disabled={retireMotivo.trim().length < 5}
      error={retire.error}
      onClose={() => { setRetireTarget(null); setRetireMotivo('') }}
      onConfirm={() => { if (retireTarget) retire.mutate({ id: retireTarget.id, version: retireTarget.version, motivo: retireMotivo }) }}
    >
      {retireTarget && (
        <>
          <p className="muted">
            Se retirará el identificador <strong>{retireTarget.valor}</strong> ({retireTarget.tipo}).
            {retireTarget.principal && ' Este animal quedará sin identificador principal.'}
          </p>
          <Field label="Motivo del retiro">
            <textarea value={retireMotivo} onChange={(event) => setRetireMotivo(event.target.value)} rows={3} required minLength={5} placeholder="Motivo del retiro (mínimo 5 caracteres)" />
          </Field>
        </>
      )}
    </ConfirmDialog>
  </div>
}
