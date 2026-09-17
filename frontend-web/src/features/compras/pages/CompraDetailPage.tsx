import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, ArrowLeft, Ban, CheckCircle2 } from 'lucide-react'
import { anularCompra, confirmarCompra, getCompra, getCompraDependencias, getCompraDetalles } from '@/features/compras/api'
import type { EstadoCompra } from '@/features/compras/types'
import { getProveedor } from '@/features/proveedores/api'
import { formatDate } from '@/shared/utils/date'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'
import { useToast } from '@/shared/toast/useToast'

const modalidadLabel: Record<string, string> = { POR_UNIDAD: 'Por unidad', POR_TROPA: 'Por tropa o punta' }
const estadoTone: Record<EstadoCompra, string> = { BORRADOR: 'status-badge-pending', CONFIRMADA: 'status-badge-confirmed', ANULADA: 'status-badge-annulled' }

export function CompraDetailPage() {
  const { id = '' } = useParams()
  const client = useQueryClient()
  const { showToast } = useToast()
  const [confirmando, setConfirmando] = useState(false)
  const [anulando, setAnulando] = useState(false)
  const [motivoAnulacion, setMotivoAnulacion] = useState('')

  const compra = useQuery({ queryKey: ['compra', id], queryFn: () => getCompra(id), enabled: Boolean(id) })
  const detalles = useQuery({ queryKey: ['compra-detalles', id], queryFn: () => getCompraDetalles(id), enabled: Boolean(id) })
  const proveedor = useQuery({ queryKey: ['proveedor', compra.data?.proveedorId], queryFn: () => getProveedor(compra.data!.proveedorId), enabled: Boolean(compra.data?.proveedorId) })
  const dependencias = useQuery({
    queryKey: ['compra-dependencias', id],
    queryFn: () => getCompraDependencias(id),
    enabled: Boolean(id) && compra.data?.estado === 'CONFIRMADA' && anulando,
  })

  const confirmar = useMutation({
    mutationFn: () => confirmarCompra(id, compra.data!.version),
    onSuccess: async () => {
      setConfirmando(false)
      showToast('Compra confirmada: se crearon los animales, el ingreso y los pesos declarados.')
      await Promise.all([
        client.invalidateQueries({ queryKey: ['compra', id] }),
        client.invalidateQueries({ queryKey: ['compra-detalles', id] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
        client.invalidateQueries({ queryKey: ['compras'] }),
      ])
    },
  })

  const anular = useMutation({
    mutationFn: () => anularCompra(id, motivoAnulacion, compra.data!.version),
    onSuccess: async () => {
      setAnulando(false)
      setMotivoAnulacion('')
      showToast('Compra anulada. Los animales asociados pasaron a estado Descartado.')
      await Promise.all([
        client.invalidateQueries({ queryKey: ['compra', id] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
        client.invalidateQueries({ queryKey: ['compras'] }),
      ])
    },
  })

  const error = compra.error ?? detalles.error ?? proveedor.error
  const value = compra.data

  return <div className="page-stack">
    <PageHeader
      eyebrow="Compras"
      title={value ? `Compra ${value.codigo}` : 'Compra'}
      description="Encabezado, animales incluidos y trazabilidad de la compra."
      actions={<Link className="button button-ghost" to="/compras"><ArrowLeft size={18} aria-hidden="true" />Volver</Link>}
    />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {compra.isPending && <LoadingState message="Cargando compra…" />}
    {value && <>
      <Card>
        <div className="section-heading">
          <h3>Datos de la compra</h3>
          <div style={{ display: 'flex', gap: '.5rem', alignItems: 'center' }}>
            <span className={`status-badge ${estadoTone[value.estado]}`}>{value.estado}</span>
            {value.estado === 'BORRADOR' && <Button onClick={() => setConfirmando(true)}><CheckCircle2 size={17} aria-hidden="true" />Confirmar compra</Button>}
            {value.estado === 'CONFIRMADA' && <Button variant="danger" className="jornada-icon-action" title="Anular compra" aria-label="Anular compra" onClick={() => setAnulando(true)}><Ban size={17} aria-hidden="true" /></Button>}
          </div>
        </div>
        <dl className="definition-list grid">
          <div><dt>Proveedor</dt><dd>{proveedor.data?.nombre ?? '—'}{proveedor.data?.telefono ? ` · ${proveedor.data.telefono}` : ''}</dd></div>
          <div><dt>Documento</dt><dd>{proveedor.data?.documento || '—'}</dd></div>
          <div><dt>Fecha de recepción</dt><dd>{formatDate(value.fechaRecepcion)}</dd></div>
          <div><dt>Modalidad</dt><dd>{modalidadLabel[value.modalidad] ?? value.modalidad}</dd></div>
          <div><dt>Cantidad</dt><dd>{value.cantidadAnimales} animal(es)</dd></div>
          <div><dt>Precio total</dt><dd><strong>{value.precioTotal.toLocaleString('es-BO', { style: 'currency', currency: value.moneda || 'BOB' })}</strong></dd></div>
          <div><dt>Precio unitario referencial</dt><dd>{value.precioUnitarioReferencial.toLocaleString('es-BO', { style: 'currency', currency: value.moneda || 'BOB' })}</dd></div>
          {value.origenMigracion && <div><dt>Origen</dt><dd><span className="status-badge">Dato migrado</span></dd></div>}
          {value.estado === 'ANULADA' && <div><dt>Motivo de anulación</dt><dd>{value.motivoAnulacion || '—'}</dd></div>}
        </dl>
        {value.observaciones && <p className="muted">{value.observaciones}</p>}
      </Card>

      <Card>
        <h3>Animales incluidos</h3>
        {detalles.isPending && <LoadingState message="Cargando animales…" />}
        {detalles.data && <div className="table-wrapper"><table><caption className="visually-hidden">Animales de la compra</caption><thead><tr>
          <th scope="col">#</th><th scope="col">Nombre</th><th scope="col">Precio asignado</th><th scope="col">Peso al ingreso</th><th scope="col">Animal</th>
        </tr></thead><tbody>{detalles.data.map((detalle) => <tr key={detalle.id}>
          <td>{detalle.numeroLinea}</td>
          <td>{detalle.nombre || '—'}</td>
          <td>{detalle.precioAsignado.toLocaleString('es-BO', { style: 'currency', currency: value.moneda || 'BOB' })}</td>
          <td>{detalle.pesoIngresoKg != null ? `${detalle.pesoIngresoKg} kg (${detalle.tipoPeso === 'ESTIMADO' ? 'estimado' : 'medido'})` : 'Sin registro'}</td>
          <td>{detalle.animalId ? <Link to={`/animales/${detalle.animalId}`}>Ver ficha</Link> : <span className="muted">Se crea al confirmar</span>}</td>
        </tr>)}</tbody></table></div>}
      </Card>

      <ConfirmDialog
        open={confirmando}
        title="Confirmar compra"
        confirmLabel="Confirmar compra"
        loading={confirmar.isPending}
        error={confirmar.error}
        onClose={() => setConfirmando(false)}
        onConfirm={() => confirmar.mutate()}
      >
        <p>Se crearán {value.cantidadAnimales} animal(es), el ingreso al hato y los pesos declarados en una sola operación. Después de confirmar, la compra no admite edición libre.</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={anulando}
        title="Anular compra"
        confirmLabel="Anular compra"
        variant="danger"
        loading={anular.isPending}
        disabled={motivoAnulacion.trim().length < 3 || (dependencias.data?.length ?? 0) > 0}
        error={anular.error}
        onClose={() => setAnulando(false)}
        onConfirm={() => anular.mutate()}
      >
        {dependencias.isPending && <LoadingState message="Verificando eventos posteriores…" />}
        {dependencias.data && dependencias.data.length > 0 && <Alert tone="danger">
          <div style={{ display: 'flex', gap: '.5rem', alignItems: 'flex-start' }}>
            <AlertTriangle size={18} aria-hidden="true" />
            <div>Esta compra tiene eventos posteriores y no puede anularse directamente. Corrígela mediante un ajuste administrativo:
              <ul>{dependencias.data.map((dep, i) => <li key={i}>{dep.tipo}: {dep.detalle}</li>)}</ul>
            </div>
          </div>
        </Alert>}
        {dependencias.data && dependencias.data.length === 0 && <>
          <p>Los animales de esta compra pasarán a estado Descartado. El ingreso original queda como hecho histórico.</p>
          <label className="field-label" htmlFor="motivo-anulacion">Motivo *</label>
          <textarea id="motivo-anulacion" rows={3} value={motivoAnulacion} onChange={(event) => setMotivoAnulacion(event.target.value)} maxLength={300} />
        </>}
      </ConfirmDialog>
    </>}
  </div>
}
