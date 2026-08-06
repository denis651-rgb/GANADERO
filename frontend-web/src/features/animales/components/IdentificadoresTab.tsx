import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Hash } from 'lucide-react'
import { actualizarIdentificador, asignarIdentificador, listIdentificadores, retirarIdentificador } from '@/features/animales/api'
import type { TipoIdentificador } from '@/features/animales/types'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

const tipos: TipoIdentificador[] = ['ARETE', 'QR', 'RFID', 'TATUAJE', 'OTRO']

export function IdentificadoresTab({ animalId }: { animalId: string }) {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
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
    onSuccess: async () => { setShowForm(false); await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const retire = useMutation({
    mutationFn: ({ id, version, motivo }: { id: string; version: number; motivo: string }) => retirarIdentificador(animalId, id, motivo, version),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const makePrincipal = useMutation({
    mutationFn: (id: string) => actualizarIdentificador(animalId, id, { principal: true }),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const error = query.error ?? assign.error ?? retire.error ?? makePrincipal.error

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {assign.isSuccess && <Alert tone="success">Identificador asignado correctamente.</Alert>}
    {retire.isSuccess && <Alert tone="success">Identificador retirado correctamente.</Alert>}
    <Card>
      <div className="filter-heading"><span><Hash size={18} />Identificadores</span><Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Cancelar' : 'Asignar identificador'}</Button></div>
      {showForm && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); assign.mutate(event.currentTarget) }}>
        <Field label="Tipo"><select name="tipo" required defaultValue="ARETE">{tipos.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
        <Field label="Valor"><input name="valor" required maxLength={120} placeholder="Número o código del identificador" /></Field>
        <label className="checkbox-line"><input name="principal" type="checkbox" defaultChecked /> Identificador principal</label>
        <Field label="Observaciones"><input name="observaciones" maxLength={1000} /></Field>
        <div className="form-actions"><Button type="submit" loading={assign.isPending}>Guardar identificador</Button></div>
      </form>}
      {query.isPending && <LoadingState message="Cargando identificadores…" />}
      {query.data?.length === 0 && !showForm && <EmptyState title="Sin identificadores" description="Asigna el primer identificador del animal." />}
      {query.data && query.data.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Tipo</th><th>Valor</th><th>Estado</th><th>Asignación</th><th>Retiro</th><th></th></tr></thead><tbody>{query.data.map((item) => <tr key={item.id}>
        <td><strong>{item.tipo}</strong></td><td>{item.valor}{item.principal && ' '}<span className="status-badge">Principal</span></td>
        <td><span className="status-badge">{item.estado}</span></td>
        <td>{new Date(item.fechaAsignacion).toLocaleDateString('es-BO')}</td>
        <td>{item.fechaRetiro ? `${new Date(item.fechaRetiro).toLocaleDateString('es-BO')}${item.motivoRetiro ? ` · ${item.motivoRetiro}` : ''}` : '—'}</td>
        <td>{item.estado === 'ACTIVO' && <div className="row-actions">{!item.principal && <Button variant="ghost" onClick={() => makePrincipal.mutate(item.id)}>Hacer principal</Button>}<Button variant="danger" loading={retire.isPending} onClick={() => { const motivo = window.prompt('Motivo del retiro') ?? ''; if (motivo.trim()) retire.mutate({ id: item.id, version: item.version, motivo }) }}>Retirar</Button></div>}</td>
      </tr>)}</tbody></table></div>}
    </Card>
  </div>
}
