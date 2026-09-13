import { useRef, useState, type KeyboardEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle, AlertTriangle, ArrowLeft, Baby, Bug, CalendarClock, Edit3, ExternalLink, MapPin, RefreshCw, Scale, ShoppingCart, Stethoscope, Syringe } from 'lucide-react'
import { changeAnimalState, getAnimal, getAnimalTimeline, getHistorialCategorias, listCategorias, listRazas } from '@/features/animales/api'
import { listAlerts, type AlertType } from '@/features/alertas/api'
import { GenealogiaTab } from '@/features/animales/components/GenealogiaTab'
import { IdentificadoresTab } from '@/features/animales/components/IdentificadoresTab'
import { FotosTab } from '@/features/animales/components/FotosTab'
import type { AnimalState } from '@/features/animales/types'
import { listPropiedades } from '@/features/propiedades/api'
import { listAllPotreros } from '@/features/potreros/api'
import { ESTADO_CALOSTRADO_LABELS, listControlesEctoparasitarios, listControlesNeonatales, listExamenesReproductivos, listTratamientos, MOMENTO_CONTROL_NEONATAL_LABELS, NIVEL_CARGA_PARASITARIA_LABELS, RESULTADO_EXAMEN_REPRODUCTIVO_LABELS, TIPO_ECTOPARASITO_LABELS, type ControlEctoparasitario, type ControlNeonatal, type ExamenReproductivo, type Tratamiento } from '@/features/sanidad/api'
import { getResumenCompraAnimal } from '@/features/compras/api'
import { getPesajeHistory } from '@/features/pesajes/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { useToast } from '@/shared/toast/useToast'
import { normalizeApiError } from '@/shared/api/errors'
import { formatDate } from '@/shared/utils/date'

const states: AnimalState[] = ['ACTIVO', 'VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO']
const criticalStates = new Set<AnimalState>(['VENDIDO', 'MUERTO', 'PERDIDO', 'TRANSFERIDO', 'DESCARTADO'])
const UNIDAD_EDAD_LABELS: Record<string, string> = { DIAS: 'días', MESES: 'meses', ANIOS: 'años' }
const FUENTE_EDAD_LABELS: Record<string, string> = { PROVEEDOR: 'proveedor', ESTIMACION_CAMPO: 'estimación de campo' }
const TIPO_CAMBIO_CATEGORIA_LABELS: Record<string, string> = { AUTOMATICO: 'Automático', MANUAL: 'Manual', CORRECCION: 'Corrección' }
const TIPOS_CALENDARIO_SANITARIO = new Set<AlertType>(['VACUNA_PROXIMA', 'VACUNA_VENCIDA', 'REVISION_SANITARIA_INGRESO'])
type Tab = 'timeline' | 'identificadores' | 'genealogia' | 'fotos'
const animalTabs: Tab[] = ['timeline', 'identificadores', 'fotos', 'genealogia']

export function AnimalDetailPage() {
  const { id = '' } = useParams()
  const [tab, setTab] = useState<Tab>('timeline')
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([])
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroModulo, setFiltroModulo] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [page, setPage] = useState(0)
  const [pendingStateChange, setPendingStateChange] = useState<{ estado: AnimalState; motivo: string } | null>(null)
  const timelineSize = 10
  const client = useQueryClient()
  const { showToast } = useToast()
  const animal = useQuery({ queryKey: ['animal', id], queryFn: () => getAnimal(id), enabled: Boolean(id) })
  const history = useQuery({
    queryKey: ['animal-timeline', id, { tipo: filtroTipo, modulo: filtroModulo, desde, hasta, page, size: timelineSize }],
    queryFn: () => getAnimalTimeline(id, {
      tipo: filtroTipo || undefined,
      modulo: filtroModulo || undefined,
      desde: desde || undefined,
      hasta: hasta || undefined,
      page,
      size: timelineSize,
    }),
    enabled: Boolean(id),
  })
  const calendarioSanitario = useQuery({
    queryKey: ['animal-alertas-sanitarias', id],
    queryFn: () => listAlerts({ animalId: id }),
    enabled: Boolean(id),
  })
  const alertasSanitarias = (calendarioSanitario.data ?? [])
    .filter((alerta) => TIPOS_CALENDARIO_SANITARIO.has(alerta.tipo) && !['RESUELTA', 'CANCELADA'].includes(alerta.estado))
  const controlesNeonatales = useQuery({
    queryKey: ['sanidad-control-neonatal', id],
    queryFn: () => listControlesNeonatales(id),
    enabled: Boolean(id),
  })
  const controlesEctoparasitarios = useQuery({
    queryKey: ['sanidad-control-ecto', id],
    queryFn: () => listControlesEctoparasitarios({ animalId: id }),
    enabled: Boolean(id),
  })
  const examenesReproductivos = useQuery({
    queryKey: ['sanidad-examen-reproductivo', id],
    queryFn: () => listExamenesReproductivos(id),
    enabled: Boolean(id),
  })
  const vacunaciones = useQuery({
    queryKey: ['animal-timeline', id, 'ultima-vacunacion'],
    queryFn: () => getAnimalTimeline(id, { tipo: 'VACUNACION_APLICADA', page: 0, size: 1 }),
    enabled: Boolean(id),
  })
  const tratamientos = useQuery({ queryKey: ['sanidad-tratamientos', id], queryFn: () => listTratamientos(id), enabled: Boolean(id) })
  const historialCategorias = useQuery({ queryKey: ['animal-historial-categorias', id], queryFn: () => getHistorialCategorias(id), enabled: Boolean(id) })
  const compraResumen = useQuery({ queryKey: ['animal-compra-resumen', id], queryFn: () => getResumenCompraAnimal(id), enabled: Boolean(id) })
  const historialPesos = useQuery({ queryKey: ['pesaje-history', id], queryFn: () => getPesajeHistory(id), enabled: Boolean(id) })
  const catalogs = useQuery({ queryKey: ['animal-detail-catalogs'], queryFn: async () => {
    const [breeds, categories, properties, paddocks] = await Promise.all([listRazas(), listCategorias(), listPropiedades(), listAllPotreros()])
    return { breeds: breeds ?? [], categories: categories ?? [], properties: properties ?? [], paddocks: paddocks ?? [] }
  } })
  const stateMutation = useMutation({
    mutationFn: ({ estado, motivo }: { estado: AnimalState; motivo: string }) =>
      changeAnimalState(id, estado, motivo, animal.data!.version),
    onSuccess: async () => {
      setPendingStateChange(null)
      showToast('Estado actualizado correctamente.')
      await Promise.all([
        client.invalidateQueries({ queryKey: ['animal', id] }),
        client.invalidateQueries({ queryKey: ['animal-timeline', id] }),
        client.invalidateQueries({ queryKey: ['animals'] }),
      ])
    },
  })
  const error = animal.error ?? history.error ?? catalogs.error ?? stateMutation.error ?? calendarioSanitario.error ?? controlesNeonatales.error ?? controlesEctoparasitarios.error ?? examenesReproductivos.error ?? vacunaciones.error ?? tratamientos.error ?? historialCategorias.error ?? compraResumen.error ?? historialPesos.error

  if (animal.isPending) return <LoadingState message="Cargando animal…" />
  if (!animal.data) return <Alert tone="danger">No se encontró el animal solicitado.</Alert>
  const value = animal.data
  const location = [catalogs.data?.properties.find((item) => item.id === value.propiedadActualId)?.nombre, catalogs.data?.paddocks.find((item) => item.id === value.potreroActualId)?.nombre].filter(Boolean).join(' / ')
  const pesosActivos = (historialPesos.data ?? []).filter((p) => p.estado === 'ACTIVO')
  const ultimoMedido = [...pesosActivos].filter((p) => p.tipoPeso === 'MEDIDO').sort((a, b) => b.fecha.localeCompare(a.fecha))[0]
  const ultimoEstimado = [...pesosActivos].filter((p) => p.tipoPeso === 'ESTIMADO').sort((a, b) => b.fecha.localeCompare(a.fecha))[0]
  const historialPesosOrdenado = [...pesosActivos].sort((a, b) => b.fecha.localeCompare(a.fecha))

  function requestStateChange(form: HTMLFormElement) {
    const data = new FormData(form)
    const next = { estado: String(data.get('estado')) as AnimalState, motivo: String(data.get('motivo')) }
    if (criticalStates.has(next.estado)) {
      setPendingStateChange(next)
      return
    }
    stateMutation.mutate(next)
  }

  function handleTabKeyDown(event: KeyboardEvent<HTMLButtonElement>, currentTab: Tab) {
    const currentIndex = animalTabs.indexOf(currentTab)
    let nextIndex: number | null = null
    if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % animalTabs.length
    if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + animalTabs.length) % animalTabs.length
    if (event.key === 'Home') nextIndex = 0
    if (event.key === 'End') nextIndex = animalTabs.length - 1
    if (nextIndex == null) return
    event.preventDefault()
    setTab(animalTabs[nextIndex])
    tabRefs.current[nextIndex]?.focus()
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Ficha animal" title={`${value.codigo}${value.nombre ? ` · ${value.nombre}` : ''}`} description="Información, estado e historial cronológico." actions={<><Link className="button button-ghost" to="/animales"><ArrowLeft size={18} aria-hidden="true" />Volver</Link><Link className="button button-primary" to={`/animales/${id}/editar`}><Edit3 size={18} aria-hidden="true" />Editar</Link></>} />
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    <div className="two-column-grid">
      <Card><div className="detail-heading"><h3>Datos principales</h3><span className={`status-badge status-${value.estado.toLowerCase()}`}>{value.estado}</span></div><dl className="detail-list">
        <div><dt>Sexo</dt><dd>{value.sexo}</dd></div><div><dt>Raza</dt><dd>{catalogs.data?.breeds.find((item) => item.id === value.razaPrincipalId)?.nombre ?? '—'}</dd></div>
        <div><dt>Categoría</dt><dd>{catalogs.data?.categories.find((item) => item.id === value.categoriaActualId)?.nombre ?? '—'}</dd></div><div><dt>Propósito</dt><dd>{value.proposito}</dd></div>
        <div><dt>Nacimiento</dt><dd>{value.fechaNacimiento
          ? <>{value.fechaNacimiento} <span className={`status-badge ${value.fechaNacimientoEstimada ? 'status-en_desarrollo' : 'status-activo'}`}>{value.fechaNacimientoEstimada ? 'ESTIMADA' : 'CONFIRMADA'}</span></>
          : <>Edad desconocida <span className="status-badge status-inactivo">DESCONOCIDA</span></>}</dd></div>
        {value.fechaNacimientoEstimada && value.edadDeclaradaValor != null && <div><dt>Edad declarada</dt><dd>{value.edadDeclaradaValor} {UNIDAD_EDAD_LABELS[value.edadDeclaradaUnidad ?? 'MESES']} · referencia {value.fechaReferenciaEdad} · fuente {FUENTE_EDAD_LABELS[value.fuenteEdadDeclarada ?? 'ESTIMACION_CAMPO']}{value.observacionEstimacion ? ` · ${value.observacionEstimacion}` : ''}</dd></div>}
        <div><dt>Origen</dt><dd>{value.origen}</dd></div>
        <div><dt>{value.origen === 'NACIDO' ? 'Ingreso al hato' : 'Fecha de recepción'}</dt><dd>{value.fechaIngreso || 'Sin registro'}</dd></div>
        <div><dt>Precio de compra</dt><dd>{value.precioAdquisicion != null ? `${value.precioAdquisicion.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Bs` : 'Sin registro'}</dd></div>
        <div><dt>Peso al ingreso</dt><dd>{value.pesoIngresoKg != null ? `${value.pesoIngresoKg} kg (${value.pesoIngresoEstimado ? 'estimado' : 'medido'})` : 'Sin registro'}</dd></div>
        <div><dt>Peso al nacer</dt><dd>{value.pesoNacimientoKg != null ? `${value.pesoNacimientoKg} kg` : 'Desconocido'}</dd></div><div><dt>Condición corporal</dt><dd>{value.condicionCorporalActual ?? '—'}</dd></div>
      </dl>
        {value.origen === 'COMPRADO' && value.pesoNacimientoKg != null && value.pesoIngresoKg == null && <p>Revisa el peso al nacer: si corresponde a la compra, puedes corregirlo desde Editar.</p>}
      </Card>
      <Card><h3><MapPin size={19} /> Ubicación y observaciones</h3><p><strong>{location || 'Ubicación no disponible'}</strong></p><p className="muted">{value.observaciones || 'Sin observaciones registradas.'}</p></Card>
    </div>
    {compraResumen.data && <Card><h3><ShoppingCart size={19} aria-hidden="true" /> Compra</h3><dl className="detail-list">
      <div><dt>Código de compra</dt><dd><Link to="/compras">{compraResumen.data.codigo}</Link></dd></div>
      <div><dt>Proveedor</dt><dd>{compraResumen.data.proveedorNombre || '—'}{compraResumen.data.proveedorTelefono ? ` · ${compraResumen.data.proveedorTelefono}` : ''}</dd></div>
      <div><dt>Documento del proveedor</dt><dd>{compraResumen.data.proveedorDocumento || '—'}</dd></div>
      <div><dt>Fecha de recepción</dt><dd>{formatDate(compraResumen.data.fechaRecepcion)}</dd></div>
      <div><dt>Modalidad</dt><dd>{compraResumen.data.modalidad === 'POR_TROPA' ? 'Por tropa o punta' : 'Por unidad'}</dd></div>
      <div><dt>Precio asignado</dt><dd>{compraResumen.data.precioAsignado != null ? compraResumen.data.precioAsignado.toLocaleString('es-BO', { style: 'currency', currency: compraResumen.data.moneda || 'BOB' }) : 'Sin registro'}</dd></div>
    </dl></Card>}
    <Card><div className="section-heading"><h3><Scale size={19} aria-hidden="true" /> Peso</h3><Link className="button button-secondary" to={`/pesajes?animalId=${id}`}><ExternalLink size={17} aria-hidden="true" />Ver pesajes</Link></div>
      {historialPesos.isPending && <LoadingState message="Cargando historial de pesos…" />}
      {!historialPesos.isPending && pesosActivos.length === 0 && <p className="muted">Este animal aún no tiene pesos registrados.</p>}
      {pesosActivos.length > 0 && <>
        <dl className="detail-list">
          <div><dt>Último peso medido</dt><dd>{ultimoMedido ? <><strong>{ultimoMedido.pesoKg} kg</strong> · {formatDate(ultimoMedido.fecha)}</> : 'Sin registro'}</dd></div>
          <div><dt>Último peso estimado</dt><dd>{ultimoEstimado ? <><strong>{ultimoEstimado.pesoKg} kg</strong> · {formatDate(ultimoEstimado.fecha)}</> : 'Sin registro'}</dd></div>
        </dl>
        {!ultimoMedido && ultimoEstimado && <Alert tone="info">El último peso disponible es estimado; no hay un peso medido registrado todavía.</Alert>}
        <div className="table-wrapper"><table><caption className="visually-hidden">Historial de pesos del animal</caption><thead><tr><th scope="col">Fecha</th><th scope="col">Peso</th><th scope="col">Tipo</th><th scope="col">Motivo</th><th scope="col">Responsable</th></tr></thead><tbody>{historialPesosOrdenado.map((pesaje) => <tr key={pesaje.id}>
          <td>{formatDate(pesaje.fecha)}</td>
          <td><strong>{pesaje.pesoKg} kg</strong></td>
          <td><span className={`status-badge ${pesaje.tipoPeso === 'ESTIMADO' ? 'status-en_desarrollo' : 'status-activo'}`}>{pesaje.tipoPeso === 'ESTIMADO' ? 'ESTIMADO' : pesaje.tipoPeso === 'MEDIDO' ? 'MEDIDO' : 'SIN DATO'}</span></td>
          <td>{pesaje.tipo}</td>
          <td>{pesaje.responsableNombre || '—'}</td>
        </tr>)}</tbody></table></div>
      </>}
    </Card>
    <Card><h3><Syringe size={19} aria-hidden="true" /> Calendario sanitario</h3>
      {calendarioSanitario.isPending && <LoadingState message="Cargando calendario sanitario…" />}
      {!calendarioSanitario.isPending && alertasSanitarias.length === 0 && <p className="muted">Sin alertas sanitarias activas para este animal.</p>}
      <p className="muted">La ausencia de alertas no confirma vacunas ni un historial sanitario completo. Los antecedentes no documentados se consideran desconocidos.</p>
      {alertasSanitarias.length > 0 && <ul className="attention-list">{alertasSanitarias.map((alerta) => <li key={alerta.id} className={alerta.tipo === 'VACUNA_VENCIDA' ? 'attention-danger' : 'attention-warning'}>
        <span className="attention-icon">{alerta.tipo === 'VACUNA_VENCIDA' ? <AlertCircle size={18} aria-hidden="true" /> : <AlertTriangle size={18} aria-hidden="true" />}</span>
        <div><strong>{alerta.titulo}</strong><span>{alerta.mensaje}</span></div>
      </li>)}</ul>}
    </Card>
    <Card>
      <div className="section-heading"><div><span className="eyebrow">Consulta</span><h3>Resumen sanitario</h3></div><Link className="button button-secondary" to={`/sanidad?seccion=controles&animalId=${id}`}><ExternalLink size={17} aria-hidden="true" />Ver historial sanitario</Link></div>
      <dl className="detail-list">
        <div><dt><Syringe size={17} aria-hidden="true" /> Última vacunación</dt><dd>{vacunaciones.data?.content[0] ? `${formatHealthDate(vacunaciones.data.content[0].fechaEvento)} · ${vacunaciones.data.content[0].titulo ?? 'Vacunación aplicada'}` : 'Sin antecedentes registrados'}</dd></div>
        <div><dt><Stethoscope size={17} aria-hidden="true" /> Último tratamiento</dt><dd>{ultimoTratamiento(tratamientos.data)}</dd></div>
        <div><dt><Baby size={17} aria-hidden="true" /> Control neonatal</dt><dd>{ultimoControlNeonatal(controlesNeonatales.data)}</dd></div>
        <div><dt><Bug size={17} aria-hidden="true" /> Control ectoparasitario</dt><dd>{ultimoControlEctoparasitario(controlesEctoparasitarios.data)}</dd></div>
        <div><dt><Stethoscope size={17} aria-hidden="true" /> Examen reproductivo</dt><dd>{ultimoExamenReproductivo(examenesReproductivos.data)}</dd></div>
      </dl>
      <p className="muted">Los registros y nuevas acciones sanitarias se administran desde el módulo Sanidad.</p>
    </Card>
    <Card><h3>Historial de categorías</h3>
      {historialCategorias.isPending && <LoadingState message="Cargando historial de categorías…" />}
      {!historialCategorias.isPending && (historialCategorias.data?.length ?? 0) === 0 && <p className="muted">Sin cambios de categoría registrados.</p>}
      {historialCategorias.data && historialCategorias.data.length > 0 && <ul className="attention-list">{historialCategorias.data.map((cambio) => {
        const anterior = catalogs.data?.categories.find((item) => item.id === cambio.categoriaAnteriorId)?.nombre ?? 'Sin categoría previa'
        const nueva = catalogs.data?.categories.find((item) => item.id === cambio.categoriaNuevaId)?.nombre ?? cambio.categoriaNuevaId
        return <li key={cambio.id}>
          <div>
            <strong>{anterior} → {nueva}</strong>
            <span className="table-secondary">{new Date(cambio.fechaCambio).toLocaleString('es-BO')} · {TIPO_CAMBIO_CATEGORIA_LABELS[cambio.tipoCambio]}{cambio.edadDias != null ? ` · ${Math.floor(cambio.edadDias / 30)} meses (${cambio.edadConfirmada ? 'edad confirmada' : 'edad estimada'})` : ''}</span>
            {cambio.motivo && <p>{cambio.motivo}</p>}
          </div>
        </li>
      })}</ul>}
    </Card>
    <Card><h3><RefreshCw size={19} /> Cambiar estado</h3><form className="state-form" onSubmit={(event) => { event.preventDefault(); requestStateChange(event.currentTarget) }}><select name="estado" defaultValue="" required><option value="" disabled>Selecciona el nuevo estado…</option>{states.filter((state) => state !== value.estado).map((state) => <option key={state}>{state}</option>)}</select><input name="motivo" required maxLength={1000} placeholder="Motivo del cambio…" /><Button type="submit" loading={stateMutation.isPending}>Actualizar estado</Button></form></Card>
    <ConfirmDialog
      open={Boolean(pendingStateChange)}
      title="Confirmar cambio de estado"
      confirmLabel="Confirmar cambio"
      variant={pendingStateChange?.estado === 'MUERTO' ? 'danger' : 'warning'}
      loading={stateMutation.isPending}
      error={stateMutation.error}
      onClose={() => setPendingStateChange(null)}
      onConfirm={() => { if (pendingStateChange && !stateMutation.isPending) stateMutation.mutate(pendingStateChange) }}
    >
      {pendingStateChange && <div className="page-stack">
        <dl className="detail-list">
          <div><dt>Animal</dt><dd>{value.codigo}{value.nombre ? ` · ${value.nombre}` : ''}</dd></div>
          <div><dt>Estado actual</dt><dd>{value.estado}</dd></div>
          <div><dt>Nuevo estado</dt><dd>{pendingStateChange.estado}</dd></div>
          <div><dt>Motivo</dt><dd>{pendingStateChange.motivo}</dd></div>
        </dl>
        <Alert tone={pendingStateChange.estado === 'MUERTO' ? 'danger' : 'info'}>Esta operación afectará el estado operativo del animal y quedará registrada en su historial.</Alert>
      </div>}
    </ConfirmDialog>
    <div className="tabs animal-detail-tabs" role="tablist" aria-label="Secciones del animal">
      <button ref={(node) => { tabRefs.current[0] = node }} type="button" role="tab" id="tab-timeline" aria-selected={tab === 'timeline'} aria-controls="panel-timeline" tabIndex={tab === 'timeline' ? 0 : -1} className={`tab-button ${tab === 'timeline' ? 'active' : ''}`} onKeyDown={(event) => handleTabKeyDown(event, 'timeline')} onClick={() => setTab('timeline')}><CalendarClock size={17} aria-hidden="true" /> Línea de tiempo</button>
      <button ref={(node) => { tabRefs.current[1] = node }} type="button" role="tab" id="tab-identificadores" aria-selected={tab === 'identificadores'} aria-controls="panel-identificadores" tabIndex={tab === 'identificadores' ? 0 : -1} className={`tab-button ${tab === 'identificadores' ? 'active' : ''}`} onKeyDown={(event) => handleTabKeyDown(event, 'identificadores')} onClick={() => setTab('identificadores')}>Identificadores</button>
      <button ref={(node) => { tabRefs.current[2] = node }} type="button" role="tab" id="tab-fotos" aria-selected={tab === 'fotos'} aria-controls="panel-fotos" tabIndex={tab === 'fotos' ? 0 : -1} className={`tab-button ${tab === 'fotos' ? 'active' : ''}`} onKeyDown={(event) => handleTabKeyDown(event, 'fotos')} onClick={() => setTab('fotos')}>Fotografías</button>
      <button ref={(node) => { tabRefs.current[3] = node }} type="button" role="tab" id="tab-genealogia" aria-selected={tab === 'genealogia'} aria-controls="panel-genealogia" tabIndex={tab === 'genealogia' ? 0 : -1} className={`tab-button ${tab === 'genealogia' ? 'active' : ''}`} onKeyDown={(event) => handleTabKeyDown(event, 'genealogia')} onClick={() => setTab('genealogia')}>Genealogía</button>
    </div>
    {tab === 'identificadores' && <div role="tabpanel" id="panel-identificadores" aria-labelledby="tab-identificadores"><IdentificadoresTab animalId={id} animalCodigo={value.codigo} /></div>}
    {tab === 'fotos' && <div role="tabpanel" id="panel-fotos" aria-labelledby="tab-fotos"><FotosTab animalId={id} /></div>}
    {tab === 'genealogia' && <div role="tabpanel" id="panel-genealogia" aria-labelledby="tab-genealogia"><GenealogiaTab animalId={id} /></div>}
    {tab === 'timeline' && <Card role="tabpanel" id="panel-timeline" aria-labelledby="tab-timeline"><h3>Línea de tiempo</h3>
      <div className="filter-heading" style={{ justifyContent: 'flex-start' }}>
        <select aria-label="Filtrar por tipo" value={filtroTipo} onChange={(event) => { setFiltroTipo(event.target.value); setPage(0) }}><option value="">Todos los tipos</option>{[...new Set(history.data?.content.map((event) => event.tipo) ?? [])].map((tipo) => <option key={tipo}>{tipo}</option>)}</select>
        <select aria-label="Filtrar por módulo" value={filtroModulo} onChange={(event) => { setFiltroModulo(event.target.value); setPage(0) }}><option value="">Todos los módulos</option>{[...new Set(history.data?.content.map((event) => event.moduloOrigen) ?? [])].map((modulo) => <option key={modulo}>{modulo}</option>)}</select>
        <input type="date" aria-label="Desde" value={desde} onChange={(event) => { setDesde(event.target.value); setPage(0) }} />
        <input type="date" aria-label="Hasta" value={hasta} onChange={(event) => { setHasta(event.target.value); setPage(0) }} />
      </div>
      {history.isPending && <LoadingState message="Cargando línea de tiempo…" />}
      {history.data?.content.length === 0 && <EmptyState title="Sin eventos" description="Todavía no existen eventos para este animal." />}
      {history.data && history.data.content.length > 0 && <ol className="timeline">{history.data.content.map((event) => {
        const metadata = Object.entries(event.metadata ?? {})
          .filter(([key, val]) => key !== 'origenSync' && (typeof val === 'string' || typeof val === 'number' || typeof val === 'boolean'))
        return <li key={event.id}><span className="timeline-dot" /><div>
          <strong>{event.titulo ?? event.tipo.replaceAll('_', ' ')}</strong>
          <time>{new Date(event.fechaTecnica ?? event.fechaEvento).toLocaleString('es-BO')}</time>
          <span className="table-secondary">Módulo: {event.moduloOrigen}</span>
          {event.origenSync && <span className="status-badge">Sincronizado</span>}
          <p>{event.descripcion ?? 'Sin detalle adicional.'}</p>
          {event.usuarioNombre && <span className="table-secondary">Registrado por: {event.usuarioNombre}</span>}
          {metadata.map(([key, val]) => <span key={key} className="table-secondary">{timelineMetadataLabel(key)}: {key === 'pesoIngresoEstimado' ? (val ? 'Estimado' : 'Medido') : typeof val === 'boolean' ? (val ? 'Sí' : 'No') : String(val)}</span>)}
        </div></li>
      })}</ol>}
      {history.data && history.data.content.length > 0 && <div className="pagination"><span>Página {history.data.page + 1} de {Math.max(history.data.totalPages, 1)}</span><div><Button variant="ghost" disabled={page === 0 || history.isFetching} onClick={() => setPage((value) => value - 1)}>Anterior</Button><Button variant="ghost" disabled={page + 1 >= history.data.totalPages || history.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente</Button></div></div>}
    </Card>}
  </div>
}

function timelineMetadataLabel(key: string) {
  const labels: Record<string, string> = {
    pesoIngresoKg: 'Peso al ingreso (kg)',
    pesoIngresoEstimado: 'Tipo de peso al ingreso',
    fechaIngreso: 'Fecha de ingreso',
    correccionPesoCompraConfirmada: 'Corrección de peso de compra confirmada',
    pesoNacimientoAnteriorKg: 'Peso registrado antes como nacimiento (kg)',
    nacimientoDesconocido: 'Nacimiento desconocido',
  }
  return labels[key] ?? key.replaceAll('_', ' ')
}

function ultimoControlNeonatal(controles?: ControlNeonatal[]) {
  const control = controles ? [...controles].sort((a, b) => b.fechaControl.localeCompare(a.fechaControl))[0] : undefined
  return control
    ? `${formatHealthDate(control.fechaControl)} · ${MOMENTO_CONTROL_NEONATAL_LABELS[control.momento]} · Calostrado ${ESTADO_CALOSTRADO_LABELS[control.calostrado]}`
    : 'Sin antecedentes registrados'
}

function ultimoControlEctoparasitario(controles?: ControlEctoparasitario[]) {
  const control = controles ? [...controles].sort((a, b) => b.fecha.localeCompare(a.fecha))[0] : undefined
  return control
    ? `${formatHealthDate(control.fecha)} · ${TIPO_ECTOPARASITO_LABELS[control.tipo]} · Carga ${NIVEL_CARGA_PARASITARIA_LABELS[control.nivelCarga]}`
    : 'Sin antecedentes registrados'
}

function ultimoExamenReproductivo(examenes?: ExamenReproductivo[]) {
  const examen = examenes ? [...examenes].sort((a, b) => b.fecha.localeCompare(a.fecha))[0] : undefined
  return examen
    ? `${formatHealthDate(examen.fecha)} · ${RESULTADO_EXAMEN_REPRODUCTIVO_LABELS[examen.resultado]}`
    : 'Sin antecedentes registrados'
}

function ultimoTratamiento(tratamientos?: Tratamiento[]) {
  const tratamiento = tratamientos ? [...tratamientos].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))[0] : undefined
  return tratamiento
    ? `${formatHealthDate(tratamiento.fechaInicio)} · ${tratamiento.diagnostico || 'Sin diagnóstico'} · ${tratamiento.estado.replaceAll('_', ' ')}`
    : 'Sin antecedentes registrados'
}

function formatHealthDate(value: string) {
  return new Date(`${value.slice(0, 10)}T00:00:00`).toLocaleDateString('es-BO')
}
