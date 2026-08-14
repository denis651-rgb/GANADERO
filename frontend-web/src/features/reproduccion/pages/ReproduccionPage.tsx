import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Activity, Baby, HeartPulse, LayoutDashboard, Stethoscope, XCircle } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { listAbortos, listCelos, listDestetes, listDiagnosticos, listPartos, listServicios } from '@/features/reproduccion/api'
import { useReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { AbortosPanel } from '@/features/reproduccion/components/AbortosPanel'
import { CelosPanel } from '@/features/reproduccion/components/CelosPanel'
import { DestetesPanel } from '@/features/reproduccion/components/DestetesPanel'
import { DiagnosticosPanel } from '@/features/reproduccion/components/DiagnosticosPanel'
import { PartosPanel } from '@/features/reproduccion/components/PartosPanel'
import { ResumenPanel } from '@/features/reproduccion/components/ResumenPanel'
import { ServiciosPanel } from '@/features/reproduccion/components/ServiciosPanel'
import { Alert } from '@/shared/components/Alert'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

type Seccion = 'resumen' | 'celos' | 'servicios' | 'diagnosticos' | 'partos'
type SubSeccion = 'partos' | 'abortos' | 'destetes'

const SECCIONES: Array<{ key: Seccion; label: string; icon: typeof LayoutDashboard }> = [
  { key: 'resumen', label: 'Resumen', icon: LayoutDashboard },
  { key: 'celos', label: 'Celos', icon: Activity },
  { key: 'servicios', label: 'Servicios', icon: HeartPulse },
  { key: 'diagnosticos', label: 'Diagnósticos', icon: Stethoscope },
  { key: 'partos', label: 'Partos y crías', icon: Baby },
]

export function ReproduccionPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const [seccion, setSeccion] = useState<Seccion>('resumen')
  const [subSeccion, setSubSeccion] = useState<SubSeccion>('partos')

  const catalogs = useReproduccionCatalogs()
  const celos = useQuery({ queryKey: ['reproduccion-celos'], queryFn: () => listCelos({ page: 0, size: 50 }) })
  const servicios = useQuery({ queryKey: ['reproduccion-servicios'], queryFn: () => listServicios({ page: 0, size: 50 }) })
  const diagnosticos = useQuery({ queryKey: ['reproduccion-diagnosticos'], queryFn: () => listDiagnosticos({ page: 0, size: 50 }) })
  const partos = useQuery({ queryKey: ['reproduccion-partos'], queryFn: () => listPartos({ page: 0, size: 50 }) })
  const abortos = useQuery({ queryKey: ['reproduccion-abortos'], queryFn: () => listAbortos({ page: 0, size: 50 }) })
  const destetes = useQuery({ queryKey: ['reproduccion-destetes'], queryFn: () => listDestetes({ page: 0, size: 50 }) })

  const refresh = () => {
    for (const key of ['reproduccion-celos', 'reproduccion-servicios', 'reproduccion-diagnosticos', 'reproduccion-partos', 'reproduccion-abortos', 'reproduccion-destetes', 'reproduccion-catalogos']) {
      void client.invalidateQueries({ queryKey: [key] })
    }
  }

  const loading = celos.isPending || servicios.isPending || diagnosticos.isPending || partos.isPending || abortos.isPending || destetes.isPending
  const error = celos.error ?? servicios.error ?? diagnosticos.error ?? partos.error ?? abortos.error ?? destetes.error ?? catalogs.error
  const seccionesDisponibles = SECCIONES.filter((item) => item.key === 'resumen' || can('REPRODUCCION_VER'))

  if (catalogs.isPending) return <div className="page-stack"><PageHeader eyebrow="Campo" title="Reproducción" description="Celo, servicios, diagnósticos, partos y destetes." /><LoadingState message="Cargando catálogos…" /></div>
  if (catalogs.error) return <div className="page-stack"><PageHeader eyebrow="Campo" title="Reproducción" description="Celo, servicios, diagnósticos, partos y destetes." /><Alert tone="danger">{normalizeApiError(catalogs.error).message}</Alert></div>

  return <div className="page-stack">
    <PageHeader eyebrow="Campo" title="Reproducción" description="Celo, servicios, diagnósticos, partos y destetes." />
    <nav className="tabs" aria-label="Secciones de reproducción">
      {seccionesDisponibles.map(({ key, label, icon: Icon }) => <button key={key} type="button" className={`tab-button ${seccion === key ? 'active' : ''}`} onClick={() => setSeccion(key)} aria-current={seccion === key ? 'page' : undefined}><Icon size={16} aria-hidden="true" />{label}</button>)}
    </nav>
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {loading && <LoadingState message="Cargando información reproductiva…" />}
    {!loading && seccion === 'resumen' && <ResumenPanel celos={celos.data!} servicios={servicios.data!} diagnosticos={diagnosticos.data!} partos={partos.data!} abortos={abortos.data!} destetes={destetes.data!} />}
    {!loading && seccion === 'celos' && <CelosPanel celos={celos.data!} isLoading={celos.isPending} error={celos.error} catalogs={catalogs.data} refresh={refresh} />}
    {!loading && seccion === 'servicios' && <ServiciosPanel servicios={servicios.data!} celos={celos.data!.content} isLoading={servicios.isPending} error={servicios.error} catalogs={catalogs.data} refresh={refresh} />}
    {!loading && seccion === 'diagnosticos' && <DiagnosticosPanel diagnosticos={diagnosticos.data!} servicios={servicios.data!.content} isLoading={diagnosticos.isPending} error={diagnosticos.error} catalogs={catalogs.data} refresh={refresh} />}
    {!loading && seccion === 'partos' && <>
      <nav className="tabs" aria-label="Subsecciones de partos y crías">
        <button type="button" className={`tab-button ${subSeccion === 'partos' ? 'active' : ''}`} onClick={() => setSubSeccion('partos')} aria-current={subSeccion === 'partos' ? 'page' : undefined}><Baby size={16} aria-hidden="true" />Partos</button>
        <button type="button" className={`tab-button ${subSeccion === 'abortos' ? 'active' : ''}`} onClick={() => setSubSeccion('abortos')} aria-current={subSeccion === 'abortos' ? 'page' : undefined}><XCircle size={16} aria-hidden="true" />Abortos</button>
        <button type="button" className={`tab-button ${subSeccion === 'destetes' ? 'active' : ''}`} onClick={() => setSubSeccion('destetes')} aria-current={subSeccion === 'destetes' ? 'page' : undefined}><Activity size={16} aria-hidden="true" />Destetes</button>
      </nav>
      {subSeccion === 'partos' && <PartosPanel partos={partos.data!} isLoading={partos.isPending} error={partos.error} catalogs={catalogs.data} refresh={refresh} />}
      {subSeccion === 'abortos' && <AbortosPanel abortos={abortos.data!} isLoading={abortos.isPending} error={abortos.error} catalogs={catalogs.data} refresh={refresh} />}
      {subSeccion === 'destetes' && <DestetesPanel destetes={destetes.data!} isLoading={destetes.isPending} error={destetes.error} catalogs={catalogs.data} refresh={refresh} />}
    </>}
  </div>
}
