import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, MailPlus, RefreshCw, X, XCircle } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  cancelarInvitacion,
  consultarInvitacion,
  crearInvitacion,
  listarInvitaciones,
  reenviarInvitacion,
  type EstadoInvitacion,
  type InvitacionResponse,
} from '@/features/invitaciones/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'

const PAGE_SIZE = 20

const ESTADOS: Array<{ value: EstadoInvitacion | ''; label: string }> = [
  { value: '', label: 'Todos los estados' },
  { value: 'PENDIENTE', label: 'Pendiente' },
  { value: 'ACEPTADA', label: 'Aceptada' },
  { value: 'VENCIDA', label: 'Vencida' },
  { value: 'CANCELADA', label: 'Cancelada' },
  { value: 'ERROR_ENVIO', label: 'Error de envío' },
]

const ESTADO_LABEL: Record<EstadoInvitacion, string> = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  VENCIDA: 'Vencida',
  CANCELADA: 'Cancelada',
  ERROR_ENVIO: 'Error de envío',
}

function puedeReenviar(estado: EstadoInvitacion) {
  return estado === 'PENDIENTE' || estado === 'ERROR_ENVIO' || estado === 'VENCIDA'
}

function puedeCancelar(estado: EstadoInvitacion) {
  return estado === 'PENDIENTE' || estado === 'ERROR_ENVIO' || estado === 'VENCIDA'
}

function fecha(iso?: string) {
  return iso ? new Date(iso).toLocaleString('es-BO') : '—'
}

export function InvitacionesPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const { showToast } = useToast()
  const [estado, setEstado] = useState<EstadoInvitacion | ''>('')
  const [email, setEmail] = useState('')
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [detalle, setDetalle] = useState<InvitacionResponse | null>(null)
  const [cancelando, setCancelando] = useState<InvitacionResponse | null>(null)

  const query = useQuery({
    queryKey: ['invitaciones', estado, email, page],
    queryFn: () => listarInvitaciones({
      estado: estado || undefined,
      email: email.trim() || undefined,
      page,
      size: PAGE_SIZE,
    }),
  })

  const create = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const d = new FormData(form)
      const cargo = String(d.get('cargo') ?? '').trim()
      return crearInvitacion({ email: String(d.get('email')), cargo: cargo || undefined })
    },
    onSuccess: () => {
      setShowForm(false)
      setPage(0)
      showToast('Invitación enviada correctamente.')
      client.invalidateQueries({ queryKey: ['invitaciones'] })
    },
  })

  const resend = useMutation({
    mutationFn: (inv: InvitacionResponse) => reenviarInvitacion(inv.id, inv.version),
    onSuccess: () => {
      showToast('Invitación reenviada.')
      client.invalidateQueries({ queryKey: ['invitaciones'] })
    },
  })

  const cancel = useMutation({
    mutationFn: ({ inv, motivo }: { inv: InvitacionResponse; motivo: string }) =>
      cancelarInvitacion(inv.id, motivo, inv.version),
    onSuccess: () => {
      setCancelando(null)
      showToast('Invitación cancelada.')
      client.invalidateQueries({ queryKey: ['invitaciones'] })
    },
  })

  const detail = useMutation({
    mutationFn: (inv: InvitacionResponse) => consultarInvitacion(inv.id),
    onSuccess: (data) => setDetalle(data),
  })

  const error = query.error ?? create.error ?? resend.error ?? cancel.error ?? detail.error

  function onSubmitCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    create.mutate(e.currentTarget)
  }

  function onSubmitCancel(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const d = new FormData(e.currentTarget)
    if (!cancelando) return
    cancel.mutate({ inv: cancelando, motivo: String(d.get('motivo') ?? '') })
  }

  const totalPages = query.data ? Math.max(1, Math.ceil(query.data.total / PAGE_SIZE)) : 1
  const puedeGestionar = can('USUARIO_CREAR')

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Seguridad"
        title="Invitaciones"
        description="Envía, consulta, reenvía y cancela invitaciones de acceso a la empresa."
        actions={puedeGestionar && (
          <Button onClick={() => setShowForm((v) => !v)}>
            <MailPlus size={18} />Nueva invitación
          </Button>
        )}
      />

      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

      {showForm && (
        <Card>
          <form className="form-grid compact-form" onSubmit={onSubmitCreate}>
            <Field label="Correo">
              <input name="email" type="email" required />
            </Field>
            <Field label="Cargo">
              <input name="cargo" placeholder="COLABORADOR" />
            </Field>
            <div className="form-actions">
              <Button type="submit" loading={create.isPending}>Enviar invitación</Button>
            </div>
          </form>
        </Card>
      )}

      {cancelando && (
        <Card>
          <h3>Cancelar invitación de {cancelando.email}</h3>
          <p className="muted">La invitación quedará cancelada y la membresía del invitado será bloqueada. Esta acción no se puede deshacer.</p>
          <form className="form-grid compact-form" onSubmit={onSubmitCancel}>
            <Field label="Motivo">
              <textarea name="motivo" rows={3} maxLength={300} required />
            </Field>
            <div className="form-actions">
              <Button variant="secondary" onClick={() => setCancelando(null)}>Volver</Button>
              <Button variant="danger" type="submit" loading={cancel.isPending}>
                <XCircle size={16} />Cancelar invitación
              </Button>
            </div>
          </form>
        </Card>
      )}

      {detalle && (
        <Card>
          <div className="detail-heading">
            <h3>Invitación de {detalle.email}</h3>
            <Button variant="ghost" onClick={() => setDetalle(null)}><X size={16} />Cerrar</Button>
          </div>
          <dl className="detail-list">
            <div><dt>Correo</dt><dd>{detalle.email}</dd></div>
            <div><dt>Estado</dt><dd><span className="status-badge">{ESTADO_LABEL[detalle.estado]}</span></dd></div>
            <div><dt>Fecha de envío</dt><dd>{fecha(detalle.fechaEnvio)}</dd></div>
            <div><dt>Fecha de vencimiento</dt><dd>{fecha(detalle.fechaVencimiento)}</dd></div>
            <div><dt>Fecha de aceptación</dt><dd>{fecha(detalle.fechaAceptacion)}</dd></div>
            <div><dt>Fecha de cancelación</dt><dd>{fecha(detalle.fechaCancelacion)}</dd></div>
            <div><dt>Intentos de envío</dt><dd>{detalle.intentosEnvio}</dd></div>
            <div><dt>Motivo de cancelación</dt><dd>{detalle.motivoCancelacion || '—'}</dd></div>
            <div><dt>Último error</dt><dd>{detalle.ultimoErrorCodigo ? `${detalle.ultimoErrorCodigo}: ${detalle.ultimoErrorMensaje ?? ''}` : '—'}</dd></div>
          </dl>
        </Card>
      )}

      <Card>
        <div className="filter-heading">
          <select value={estado} onChange={(e) => { setEstado(e.target.value as EstadoInvitacion | ''); setPage(0) }} aria-label="Filtrar por estado">
            {ESTADOS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
          <input
            type="search"
            placeholder="Buscar por correo…"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); setPage(0) } }}
          />
          <Button variant="secondary" onClick={() => { setEmail(''); setEstado(''); setPage(0) }}>Limpiar</Button>
        </div>

        {query.isPending && <LoadingState message="Consultando invitaciones…" />}
        {query.isError && <Alert tone="danger">{normalizeApiError(query.error).message}</Alert>}
        {query.data && query.data.items.length === 0 && (
          <EmptyState
            title="No hay invitaciones"
            description="Crea la primera invitación para dar acceso a un colaborador."
          />
        )}
        {query.data && query.data.items.length > 0 && (
          <>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Correo</th>
                    <th>Estado</th>
                    <th>Enviada</th>
                    <th>Vence</th>
                    <th>Intentos</th>
                    <th>Error</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {query.data.items.map((inv) => (
                    <tr key={inv.id}>
                      <td><strong>{inv.email}</strong><span className="table-secondary">v{inv.version}</span></td>
                      <td><span className="status-badge">{ESTADO_LABEL[inv.estado]}</span></td>
                      <td>{fecha(inv.fechaEnvio)}</td>
                      <td>{fecha(inv.fechaVencimiento)}</td>
                      <td>{inv.intentosEnvio}</td>
                      <td>{inv.ultimoErrorCodigo || '—'}</td>
                      <td className="row-actions">
                        <Button variant="ghost" onClick={() => detail.mutate(inv)}><Eye size={16} />Ver</Button>
                        {puedeGestionar && puedeReenviar(inv.estado) && (
                          <Button variant="ghost" loading={resend.isPending} onClick={() => resend.mutate(inv)}>
                            <RefreshCw size={16} />Reenviar
                          </Button>
                        )}
                        {puedeGestionar && puedeCancelar(inv.estado) && (
                          <Button variant="ghost" onClick={() => setCancelando(inv)}><XCircle size={16} />Cancelar</Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pagination">
              <div>
                <Button variant="secondary" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>Anterior</Button>
                <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
              </div>
              <span>Página {page + 1} de {totalPages} · {query.data.total} invitaciones</span>
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
