import { Fragment } from 'react'
import { Link, useLocation } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { appModules } from '@/app/modules'
import { getLote } from '@/features/lotes/api'

const EXTRA_LABELS: Record<string, string> = {
  '/animales/nuevo': 'Nuevo animal',
  '/animales/ingreso-lote': 'Ingreso de lote',
  '/animales/qr/imprimir': 'Imprimir QR',
  '/qr/escanear': 'Escanear QR',
}

const STATIC_LABELS: Record<string, string> = { ...EXTRA_LABELS }
for (const module of appModules) {
  STATIC_LABELS[module.path] = module.label
  module.children?.forEach((child) => {
    STATIC_LABELS[child.path] = child.label
  })
}


function segmentLabel(seg: string): string {
  return seg.charAt(0).toUpperCase() + seg.slice(1).replace(/-/g, ' ')
}

interface Crumb {
  to: string
  label: string
}

/** Dinámico: sustituye el UUID del último tramo por el nombre del lote cargado en caché. */
const LOTE_DETAIL_PATTERN = /^\/lotes\/([0-9a-fA-F-]{36})$/

export function Breadcrumbs() {
  const { pathname } = useLocation()

  const crumbs: Crumb[] = []
  let acc = ''
  for (const seg of pathname.split('/').filter(Boolean)) {
    acc += `/${seg}`
    crumbs.push({ to: acc, label: STATIC_LABELS[acc] ?? segmentLabel(seg) })
  }

  const last = crumbs.length - 1

  // Resuelve el nombre del lote desde la caché de React Query (la misma clave que usa LoteDetail).
  const matched = pathname.match(LOTE_DETAIL_PATTERN)
  const loteId = matched?.[1]
  const loteQuery = useQuery({
    queryKey: ['lote', loteId],
    queryFn: () => getLote(loteId!),
    enabled: Boolean(loteId),
  })

  const resolvedCrumbs = crumbs.map((crumb, i) => {
    if (i === last && loteQuery.data) {
      return { ...crumb, label: loteQuery.data.nombre || loteQuery.data.codigo || crumb.label }
    }
    return crumb
  })

  return (
    <nav className="topbar-breadcrumbs" aria-label="Ruta de navegación">
      <Link to="/" className="topbar-brand" aria-label="Panel principal">
        <img src="/icons/logo.png" alt="" width={18} height={18} />
        GANADERO
      </Link>
      {resolvedCrumbs.map((crumb, i) => {
        const isLast = i === last
        return (
          <Fragment key={crumb.to}>
            <span className={isLast ? 'crumb crumb-current' : 'crumb'}>
              <span className="crumb-sep" aria-hidden="true">/</span>
              {isLast ? (
                <span aria-current="page">{crumb.label}</span>
              ) : (
                <Link to={crumb.to} className="crumb-link">
                  {crumb.label}
                </Link>
              )}
            </span>
          </Fragment>
        )
      })}
    </nav>
  )
}
