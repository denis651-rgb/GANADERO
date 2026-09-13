import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Baby, Coins, Download, HeartCrack, ShoppingCart } from 'lucide-react'
import { getReporteMuertes, getReporteNacimientos, getReporteVentas } from '@/features/reportes/api'
import { calcularRangoPeriodo, type TipoPeriodo } from '@/features/reportes/periodo'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { descargarCsv } from '@/shared/utils/csv'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { formatDate, todayInBolivia } from '@/shared/utils/date'
import { normalizeApiError } from '@/shared/api/errors'

type TipoPeriodoUi = TipoPeriodo | 'PERSONALIZADO'

const anioActual = Number(todayInBolivia().slice(0, 4))
const ANIOS = Array.from({ length: 6 }, (_, i) => anioActual - i)

const modalidadLabel: Record<string, string> = { EN_PIE: 'En pie', CARNEADO: 'Carneado' }

function moneda(valor: number, codigo: string) {
  return valor.toLocaleString('es-BO', { style: 'currency', currency: codigo || 'BOB' })
}

export function ReportesPage() {
  const [tipoPeriodo, setTipoPeriodo] = useState<TipoPeriodoUi>('TRIMESTRE')
  const [anio, setAnio] = useState(anioActual)
  const [numero, setNumero] = useState(1)
  const [desdeManual, setDesdeManual] = useState('')
  const [hastaManual, setHastaManual] = useState('')

  const { desde, hasta } = useMemo(() => {
    if (tipoPeriodo === 'PERSONALIZADO') return { desde: desdeManual, hasta: hastaManual }
    return calcularRangoPeriodo(tipoPeriodo, anio, numero)
  }, [tipoPeriodo, anio, numero, desdeManual, hastaManual])

  const rangoValido = Boolean(desde && hasta && desde <= hasta)

  const nacidos = useQuery({
    queryKey: ['reporte-nacimientos', desde, hasta],
    queryFn: () => getReporteNacimientos(desde, hasta),
    enabled: rangoValido,
  })
  const muertos = useQuery({
    queryKey: ['reporte-muertes', desde, hasta],
    queryFn: () => getReporteMuertes(desde, hasta),
    enabled: rangoValido,
  })
  const ventas = useQuery({
    queryKey: ['reporte-ventas', desde, hasta],
    queryFn: () => getReporteVentas(desde, hasta),
    enabled: rangoValido,
  })

  const ingresoTotal = (ventas.data ?? []).reduce((acc, v) => acc + v.precio, 0)
  const error = nacidos.error ?? muertos.error ?? ventas.error
  const cargando = nacidos.isPending || muertos.isPending || ventas.isPending

  return <div className="page-stack">
    <PageHeader eyebrow="Comercial" title="Reportes"
      description="Movimientos del hato (nacimientos, muertes y ventas) por período." />

    <Card>
      <div className="filter-heading">
        <span>Período</span>
        <select aria-label="Tipo de período" value={tipoPeriodo} onChange={(event) => setTipoPeriodo(event.target.value as TipoPeriodoUi)}>
          <option value="TRIMESTRE">Trimestral</option>
          <option value="SEMESTRE">Semestral</option>
          <option value="ANIO">Anual</option>
          <option value="PERSONALIZADO">Personalizado</option>
        </select>

        {tipoPeriodo !== 'PERSONALIZADO' && <>
          <select aria-label="Año" value={anio} onChange={(event) => setAnio(Number(event.target.value))}>
            {ANIOS.map((valor) => <option key={valor} value={valor}>{valor}</option>)}
          </select>
          {tipoPeriodo === 'TRIMESTRE' && <select aria-label="Trimestre" value={numero} onChange={(event) => setNumero(Number(event.target.value))}>
            <option value={1}>Trimestre 1 (ene-mar)</option>
            <option value={2}>Trimestre 2 (abr-jun)</option>
            <option value={3}>Trimestre 3 (jul-sep)</option>
            <option value={4}>Trimestre 4 (oct-dic)</option>
          </select>}
          {tipoPeriodo === 'SEMESTRE' && <select aria-label="Semestre" value={numero} onChange={(event) => setNumero(Number(event.target.value))}>
            <option value={1}>Semestre 1 (ene-jun)</option>
            <option value={2}>Semestre 2 (jul-dic)</option>
          </select>}
        </>}
        {tipoPeriodo === 'PERSONALIZADO' && <>
          <input aria-label="Desde" type="date" value={desdeManual} onChange={(event) => setDesdeManual(event.target.value)} />
          <input aria-label="Hasta" type="date" value={hastaManual} onChange={(event) => setHastaManual(event.target.value)} />
        </>}
      </div>
      {rangoValido && <p className="muted">Del {formatDate(desde)} al {formatDate(hasta)}.</p>}
      {!rangoValido && tipoPeriodo === 'PERSONALIZADO' && <p className="muted">Elige una fecha "desde" anterior o igual a "hasta".</p>}
    </Card>

    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

    {rangoValido && <Card>
      <h3>Resumen</h3>
      {cargando
        ? <LoadingState message="Calculando el resumen del período…" />
        : <div className="metric-grid">
          <Card className="metric-card"><span className="metric-icon" aria-hidden="true"><Baby size={22} /></span><div><span>Nacidos</span><strong>{nacidos.data?.length ?? 0}</strong></div></Card>
          <Card className="metric-card"><span className="metric-icon" aria-hidden="true"><HeartCrack size={22} /></span><div><span>Muertos</span><strong>{muertos.data?.length ?? 0}</strong></div></Card>
          <Card className="metric-card"><span className="metric-icon" aria-hidden="true"><ShoppingCart size={22} /></span><div><span>Vendidos</span><strong>{ventas.data?.length ?? 0}</strong></div></Card>
          <Card className="metric-card"><span className="metric-icon" aria-hidden="true"><Coins size={22} /></span><div><span>Ingreso por ventas</span><strong>{moneda(ingresoTotal, 'BOB')}</strong></div></Card>
        </div>}
    </Card>}

    {rangoValido && <Card>
      <div className="section-heading">
        <h3>Nacimientos</h3>
        <Button variant="ghost" disabled={!nacidos.data?.length}
          onClick={() => descargarCsv(`nacimientos_${desde}_${hasta}.csv`,
            ['Código', 'Nombre', 'Sexo', 'Fecha nacimiento', 'Raza', 'Categoría', 'Peso nacimiento (kg)', 'Madre', 'Padre', 'Propiedad', 'Potrero'],
            (nacidos.data ?? []).map((a) => [a.codigo, a.nombre ?? '', a.sexo, a.fechaNacimiento, a.raza, a.categoria, a.pesoNacimientoKg ?? '', a.madre ?? '', a.padre ?? '', a.propiedad ?? '', a.potrero ?? '']))}>
          <Download size={16} aria-hidden="true" />Exportar CSV
        </Button>
      </div>
      {nacidos.isPending && <LoadingState message="Cargando nacimientos…" />}
      {nacidos.data?.length === 0 && <EmptyState title="Sin nacimientos" description="No hay animales nacidos registrados en este período." />}
      {nacidos.data && nacidos.data.length > 0 && <div className="table-wrapper"><table><thead><tr>
        <th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Sexo</th><th scope="col">Fecha</th>
        <th scope="col">Raza</th><th scope="col">Categoría</th><th scope="col">Peso nac. (kg)</th>
        <th scope="col">Madre</th><th scope="col">Padre</th><th scope="col">Propiedad / Potrero</th>
      </tr></thead><tbody>{nacidos.data.map((a) => <tr key={a.animalId}>
        <td><strong>{a.codigo}</strong></td><td>{a.nombre ?? '—'}</td><td>{a.sexo}</td>
        <td>{formatDate(a.fechaNacimiento)}{a.fechaNacimientoEstimada ? ' (est.)' : ''}</td>
        <td>{a.raza}</td><td>{a.categoria}</td><td>{a.pesoNacimientoKg ?? '—'}</td>
        <td>{a.madre ?? '—'}</td><td>{a.padre ?? '—'}</td>
        <td>{[a.propiedad, a.potrero].filter(Boolean).join(' / ') || '—'}</td>
      </tr>)}</tbody></table></div>}
    </Card>}

    {rangoValido && <Card>
      <div className="section-heading">
        <h3>Muertes</h3>
        <Button variant="ghost" disabled={!muertos.data?.length}
          onClick={() => descargarCsv(`muertes_${desde}_${hasta}.csv`,
            ['Código', 'Nombre', 'Sexo', 'Raza', 'Categoría', 'Fecha', 'Motivo', 'Propiedad', 'Potrero'],
            (muertos.data ?? []).map((a) => [a.codigo, a.nombre ?? '', a.sexo, a.raza, a.categoria, a.fechaMuerte, a.motivo ?? '', a.propiedad ?? '', a.potrero ?? '']))}>
          <Download size={16} aria-hidden="true" />Exportar CSV
        </Button>
      </div>
      {muertos.isPending && <LoadingState message="Cargando muertes…" />}
      {muertos.data?.length === 0 && <EmptyState title="Sin muertes" description="No hay bajas por muerte registradas en este período." />}
      {muertos.data && muertos.data.length > 0 && <div className="table-wrapper"><table><thead><tr>
        <th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Raza</th><th scope="col">Categoría</th>
        <th scope="col">Fecha</th><th scope="col">Motivo</th><th scope="col">Propiedad / Potrero</th>
      </tr></thead><tbody>{muertos.data.map((a) => <tr key={a.animalId}>
        <td><strong>{a.codigo}</strong></td><td>{a.nombre ?? '—'}</td><td>{a.raza}</td><td>{a.categoria}</td>
        <td>{formatDate(a.fechaMuerte)}</td><td>{a.motivo ?? '—'}</td>
        <td>{[a.propiedad, a.potrero].filter(Boolean).join(' / ') || '—'}</td>
      </tr>)}</tbody></table></div>}
    </Card>}

    {rangoValido && <Card>
      <div className="section-heading">
        <h3>Ventas</h3>
        <Button variant="ghost" disabled={!ventas.data?.length}
          onClick={() => descargarCsv(`ventas_${desde}_${hasta}.csv`,
            ['Código', 'Nombre', 'Raza', 'Fecha', 'Comprador', 'Teléfono', 'Precio', 'Moneda', 'Modalidad', 'Peso venta (kg)'],
            (ventas.data ?? []).map((v) => [v.codigo, v.nombre ?? '', v.raza, v.fechaVenta, v.comprador, v.telefonoComprador ?? '', v.precio, v.moneda, modalidadLabel[v.modalidad] ?? v.modalidad, v.pesoVentaKg ?? '']))}>
          <Download size={16} aria-hidden="true" />Exportar CSV
        </Button>
      </div>
      {ventas.isPending && <LoadingState message="Cargando ventas…" />}
      {ventas.data?.length === 0 && <EmptyState title="Sin ventas" description="No hay ventas registradas en este período." />}
      {ventas.data && ventas.data.length > 0 && <div className="table-wrapper"><table><thead><tr>
        <th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Raza</th><th scope="col">Fecha</th>
        <th scope="col">Comprador</th><th scope="col">Teléfono</th><th scope="col">Precio</th>
        <th scope="col">Modalidad</th><th scope="col">Peso (kg)</th>
      </tr></thead><tbody>{ventas.data.map((v) => <tr key={v.ventaId}>
        <td><strong>{v.codigo}</strong></td><td>{v.nombre ?? '—'}</td><td>{v.raza}</td><td>{formatDate(v.fechaVenta)}</td>
        <td>{v.comprador}</td><td>{v.telefonoComprador ?? '—'}</td><td>{moneda(v.precio, v.moneda)}</td>
        <td>{modalidadLabel[v.modalidad] ?? v.modalidad}</td><td>{v.pesoVentaKg ?? '—'}</td>
      </tr>)}</tbody></table></div>}
    </Card>}
  </div>
}
