import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Hash, QrCode } from 'lucide-react'
import { asignarIdentificador, hacerPrincipalIdentificador, listIdentificadores, retirarIdentificador } from '@/features/animales/api'
import type { Identificador, TipoIdentificador } from '@/features/animales/types'
import { generarQr, reemplazarQr } from '@/features/animales/qr/qr-api'
import { AnimalQrCard } from '@/features/animales/qr/components/AnimalQrCard'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

const tiposManuales: TipoIdentificador[] = ['ARETE', 'RFID', 'TATUAJE', 'OTRO']

export function IdentificadoresTab({ animalId, animalCodigo }: { animalId: string; animalCodigo: string }) {
  const client = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [generateOpen, setGenerateOpen] = useState(false)
  const [replaceTarget, setReplaceTarget] = useState<Identificador | null>(null)
  const [qrView, setQrView] = useState<Identificador | null>(null)
  const [generatePrincipal, setGeneratePrincipal] = useState(true)
  const [replaceMotivo, setReplaceMotivo] = useState('')
  const [replacePrincipal, setReplacePrincipal] = useState(true)
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
    onSuccess: async () => { setShowForm(false); await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const retire = useMutation({
    mutationFn: ({ id, version, motivo }: { id: string; version: number; motivo: string }) => retirarIdentificador(animalId, id, motivo, version),
    onSuccess: async () => {
      setRetireTarget(null)
      setRetireMotivo('')
      await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] })
    },
  })
  const makePrincipal = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => hacerPrincipalIdentificador(animalId, id, version),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] }) },
  })
  const generate = useMutation({
    mutationFn: () => generarQr(animalId, generatePrincipal),
    onSuccess: async (saved) => {
      setGenerateOpen(false)
      await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] })
      setQrView(saved)
    },
  })
  const replace = useMutation({
    mutationFn: () => reemplazarQr(animalId, replaceTarget!.id, replaceMotivo, replacePrincipal, replaceTarget!.version),
    onSuccess: async (saved) => {
      setReplaceTarget(null)
      setReplaceMotivo('')
      await client.invalidateQueries({ queryKey: ['animal-identificadores', animalId] })
      setQrView(saved)
    },
  })
  const error = query.error ?? assign.error ?? retire.error ?? makePrincipal.error ?? generate.error ?? replace.error
  const activePrincipal = query.data?.find((item) => item.principal && item.estado === 'ACTIVO')
  const activeQr = query.data?.find((item) => item.tipo === 'QR' && item.estado === 'ACTIVO')

  const replaceError = replace.error ?? generate.error

  return <div className="page-stack">
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {assign.isSuccess && <Alert tone="success">Identificador asignado correctamente.</Alert>}
    {retire.isSuccess && <Alert tone="success">Identificador retirado correctamente.</Alert>}
    {generate.isSuccess && <Alert tone="success">Código QR generado correctamente.</Alert>}
    {replace.isSuccess && <Alert tone="success">Código QR reemplazado correctamente.</Alert>}
    {query.data && query.data.length > 0 && !activePrincipal && <Alert>Este animal no tiene un identificador principal activo.</Alert>}
    <Card>
      <div className="filter-heading">
        <span><Hash size={18} />Identificadores</span>
        <div className="row-actions">
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Cancelar' : 'Asignar identificador'}</Button>
          <Button onClick={() => setGenerateOpen(true)}><QrCode size={16} />Generar QR</Button>
        </div>
      </div>
      {showForm && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); assign.mutate(event.currentTarget) }}>
        <Field label="Tipo"><select name="tipo" required defaultValue="ARETE">{tiposManuales.map((tipo) => <option key={tipo}>{tipo}</option>)}</select></Field>
        <Field label="Valor"><input name="valor" required maxLength={120} placeholder="Número o código del identificador" /></Field>
        <label className="checkbox-line"><input name="principal" type="checkbox" defaultChecked /> Identificador principal</label>
        <Field label="Observaciones"><input name="observaciones" maxLength={1000} /></Field>
        <div className="form-actions"><Button type="submit" loading={assign.isPending}>Guardar identificador</Button></div>
        <p className="field-hint">El identificador QR se genera desde el servidor y no se asigna manualmente.</p>
      </form>}
      {query.isPending && <LoadingState message="Cargando identificadores…" />}
      {query.data?.length === 0 && !showForm && <EmptyState title="Sin identificadores" description="Asigna el primer identificador o genera el código QR del animal." />}
      {query.data && query.data.length > 0 && <div className="table-wrapper"><table><thead><tr><th>Tipo</th><th>Valor</th><th>Estado</th><th>Asignación</th><th>Retiro</th><th></th></tr></thead><tbody>{query.data.map((item) => <tr key={item.id}>
        <td><strong>{item.tipo}</strong></td><td>{item.valor}{item.principal && ' '}<span className="status-badge">Principal</span></td>
        <td><span className="status-badge">{item.estado}</span></td>
        <td>{new Date(item.fechaAsignacion).toLocaleDateString('es-BO')}</td>
        <td>{item.fechaRetiro ? `${new Date(item.fechaRetiro).toLocaleDateString('es-BO')}${item.motivoRetiro ? ` · ${item.motivoRetiro}` : ''}` : '—'}</td>
        <td><div className="row-actions">
          {item.tipo === 'QR' && item.estado === 'ACTIVO' && <Button variant="ghost" onClick={() => setQrView(item)}><QrCode size={15} />Ver QR</Button>}
          {item.estado === 'ACTIVO' && <>
            {item.tipo === 'QR' && <Button variant="ghost" onClick={() => setReplaceTarget(item)}>Reemplazar QR</Button>}
            {!item.principal && <Button variant="ghost" onClick={() => makePrincipal.mutate({ id: item.id, version: item.version })}>Hacer principal</Button>}
            <Button variant="danger" loading={retire.isPending && retire.variables?.id === item.id} onClick={() => { setRetireTarget(item); setRetireMotivo('') }}>Retirar</Button>
          </>}
        </div></td>
      </tr>)}</tbody></table></div>}
    </Card>

    <Modal open={generateOpen} title="Generar código QR" onClose={() => { if (!generate.isPending) setGenerateOpen(false) }}>
      <div className="page-stack">
        <p className="muted">El código QR se genera y firma en el servidor. No contiene datos sensibles y puede reemplazarse cuando el animal cambie de identificación.</p>
        {activeQr && <Alert tone="info">Este animal ya tiene un QR activo. Generar uno nuevo lo reemplazará.</Alert>}
        <label className="checkbox-line"><input type="checkbox" checked={generatePrincipal} onChange={(event) => setGeneratePrincipal(event.target.checked)} /> Marcar como identificador principal</label>
        {replaceError && <Alert tone="danger">{normalizeApiError(replaceError).message}</Alert>}
        <div className="form-actions"><Button onClick={() => generate.mutate()} loading={generate.isPending}>Generar QR</Button></div>
      </div>
    </Modal>

    <Modal open={Boolean(replaceTarget)} title="Reemplazar código QR" onClose={() => { if (!replace.isPending) setReplaceTarget(null) }}>
      {replaceTarget && <div className="page-stack">
        <p className="muted">Se generará un nuevo QR con nueva firma. El QR anterior quedará marcado como retirado y dejará de ser válido.</p>
        <Field label="Motivo del reemplazo"><textarea value={replaceMotivo} onChange={(event) => setReplaceMotivo(event.target.value)} rows={3} required minLength={5} placeholder="Motivo del reemplazo (mínimo 5 caracteres)" /></Field>
        <label className="checkbox-line"><input type="checkbox" checked={replacePrincipal} onChange={(event) => setReplacePrincipal(event.target.checked)} /> Marcar como identificador principal</label>
        {replaceError && <Alert tone="danger">{normalizeApiError(replaceError).message}</Alert>}
        <div className="form-actions"><Button onClick={() => replace.mutate()} loading={replace.isPending} disabled={replaceMotivo.trim().length < 5}>Reemplazar QR</Button></div>
      </div>}
    </Modal>

    <Modal open={Boolean(qrView)} title="Código QR" onClose={() => setQrView(null)} wide>
      {qrView && <AnimalQrCard
        animalId={animalId}
        animalCodigo={animalCodigo}
        identificador={qrView}
        showPayload
        onReplace={() => { setQrView(null); setReplaceTarget(qrView) }}
        onRetire={() => { setQrView(null); setRetireTarget(qrView); setRetireMotivo('') }}
      />}
    </Modal>

    <Modal
      open={Boolean(retireTarget)}
      title="Retirar identificador"
      onClose={() => { if (!retire.isPending) { setRetireTarget(null); setRetireMotivo('') } }}
    >
      {retireTarget && <div className="page-stack">
        <p className="muted">
          Se retirará el identificador <strong>{retireTarget.valor}</strong> ({retireTarget.tipo}).
          {retireTarget.tipo === 'QR' && ' El código QR dejará de ser válido.'}
          {retireTarget.principal && ' Este animal quedará sin identificador principal.'}
        </p>
        <Field label="Motivo del retiro">
          <textarea value={retireMotivo} onChange={(event) => setRetireMotivo(event.target.value)} rows={3} required minLength={5} placeholder="Motivo del retiro (mínimo 5 caracteres)" />
        </Field>
        {retire.error && <Alert tone="danger">{normalizeApiError(retire.error).message}</Alert>}
        <div className="form-actions">
          <Button variant="ghost" onClick={() => { setRetireTarget(null); setRetireMotivo('') }} disabled={retire.isPending}>Cancelar</Button>
          <Button variant="danger" loading={retire.isPending} disabled={retireMotivo.trim().length < 5} onClick={() => retire.mutate({ id: retireTarget.id, version: retireTarget.version, motivo: retireMotivo })}>Retirar identificador</Button>
        </div>
      </div>}
    </Modal>
  </div>
}
