import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Activity, ClipboardList, HeartPulse, LayoutDashboard, Stethoscope, Syringe } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { listCasos, listJornadas, listPlanes, listTratamientos } from '@/features/sanidad/api'
import { useSanidadCatalogs } from '@/features/sanidad/catalogs'
import { CasosPanel } from '@/features/sanidad/components/CasosPanel'
import { EnfermedadesPanel } from '@/features/sanidad/components/EnfermedadesPanel'
import { JornadasPanel } from '@/features/sanidad/components/JornadasPanel'
import { PlanesPanel } from '@/features/sanidad/components/PlanesPanel'
import { ResumenPanel } from '@/features/sanidad/components/ResumenPanel'
import { TratamientosPanel } from '@/features/sanidad/components/TratamientosPanel'
import { Alert } from '@/shared/components/Alert'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

type Seccion = 'resumen' | 'planes' | 'jornadas' | 'casos' | 'tratamientos'
type SubSeccion = 'planes' | 'enfermedades'

const SECCIONES: Array<{ key: Seccion; label: string; icon: typeof LayoutDashboard; permisos: string[] }> = [
  { key: 'resumen', label: 'Resumen', icon: LayoutDashboard, permisos: [] },
  { key: 'planes', label: 'Planes sanitarios', icon: ClipboardList, permisos: ['SANIDAD_VER'] },
  { key: 'jornadas', label: 'Jornadas', icon: Syringe, permisos: ['SANIDAD_VER'] },
  { key: 'casos', label: 'Casos clínicos', icon: Stethoscope, permisos: ['SANIDAD_VER'] },
  { key: 'tratamientos', label: 'Tratamientos', icon: HeartPulse, permisos: ['SANIDAD_VER'] },
]

export function SanidadPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const [seccion, setSeccion] = useState<Seccion>('resumen')
  const [subSeccion, setSubSeccion] = useState<SubSeccion>('planes')

  const catalogs = useSanidadCatalogs()
  const planes = useQuery({ queryKey: ['sanidad-planes'], queryFn: listPlanes })
  const jornadas = useQuery({ queryKey: ['sanidad-jornadas'], queryFn: listJornadas })
  const casos = useQuery({ queryKey: ['sanidad-casos'], queryFn: () => listCasos() })
  const tratamientos = useQuery({ queryKey: ['sanidad-tratamientos'], queryFn: () => listTratamientos() })

  const refresh = () => {
    for (const key of ['sanidad-planes', 'sanidad-jornadas', 'sanidad-casos', 'sanidad-tratamientos', 'sanidad-items', 'sanidad-enfermedades', 'sanidad-aplicaciones', 'sanidad-catalogos']) {
      void client.invalidateQueries({ queryKey: [key] })
    }
  }

  const loading = planes.isPending || jornadas.isPending || casos.isPending || tratamientos.isPending
  const error = planes.error ?? jornadas.error ?? casos.error ?? tratamientos.error ?? catalogs.error
  const seccionesDisponibles = SECCIONES.filter((item) => item.permisos.every((permiso) => can(permiso)))

  if (catalogs.isPending) return <div className="page-stack"><PageHeader eyebrow="Campo" title="Sanidad" description="Planes sanitarios, jornadas, casos clínicos y tratamientos." /><LoadingState message="Cargando catálogos…" /></div>
  if (catalogs.error) return <div className="page-stack"><PageHeader eyebrow="Campo" title="Sanidad" description="Planes sanitarios, jornadas, casos clínicos y tratamientos." /><Alert tone="danger">{normalizeApiError(catalogs.error).message}</Alert></div>

  return <div className="page-stack">
    <PageHeader eyebrow="Campo" title="Sanidad" description="Planes sanitarios, jornadas, casos clínicos y tratamientos." />
    <nav className="tabs" aria-label="Secciones de sanidad">
      {seccionesDisponibles.map(({ key, label, icon: Icon }) => <button key={key} type="button" className={`tab-button ${seccion === key ? 'active' : ''}`} onClick={() => setSeccion(key)} aria-current={seccion === key ? 'page' : undefined}><Icon size={16} aria-hidden="true" />{label}</button>)}
    </nav>
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {loading && <LoadingState message="Cargando información de sanidad…" />}
    {!loading && seccion === 'resumen' && <ResumenPanel planes={planes.data!} jornadas={jornadas.data!} casos={casos.data!} tratamientos={tratamientos.data!} catalogs={catalogs.data} />}
    {!loading && seccion === 'planes' && <>
      <nav className="tabs" aria-label="Subsecciones de planes sanitarios">
        <button type="button" className={`tab-button ${subSeccion === 'planes' ? 'active' : ''}`} onClick={() => setSubSeccion('planes')} aria-current={subSeccion === 'planes' ? 'page' : undefined}><ClipboardList size={16} aria-hidden="true" />Planes</button>
        <button type="button" className={`tab-button ${subSeccion === 'enfermedades' ? 'active' : ''}`} onClick={() => setSubSeccion('enfermedades')} aria-current={subSeccion === 'enfermedades' ? 'page' : undefined}><Activity size={16} aria-hidden="true" />Enfermedades</button>
      </nav>
      {subSeccion === 'planes' && <PlanesPanel planes={planes.data!} isLoading={planes.isPending} error={planes.error} catalogs={catalogs.data} refresh={refresh} />}
      {subSeccion === 'enfermedades' && <EnfermedadesPanel />}
    </>}
    {!loading && seccion === 'jornadas' && <JornadasPanel jornadas={jornadas.data!} isLoading={jornadas.isPending} error={jornadas.error} catalogs={catalogs.data} refresh={refresh} />}
    {!loading && seccion === 'casos' && <CasosPanel casos={casos.data!} isLoading={casos.isPending} error={casos.error} catalogs={catalogs.data} refresh={refresh} />}
    {!loading && seccion === 'tratamientos' && <TratamientosPanel tratamientos={tratamientos.data!} isLoading={tratamientos.isPending} error={tratamientos.error} catalogs={catalogs.data} refresh={refresh} />}
  </div>
}
