import { Fragment } from 'react'
import { Link, useLocation } from 'react-router'
import { appModules } from '@/app/modules'

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

export function Breadcrumbs() {
  const { pathname } = useLocation()

  const crumbs: Crumb[] = []
  let acc = ''
  for (const seg of pathname.split('/').filter(Boolean)) {
    acc += `/${seg}`
    crumbs.push({ to: acc, label: STATIC_LABELS[acc] ?? segmentLabel(seg) })
  }

  const last = crumbs.length - 1

  return (
    <nav className="topbar-breadcrumbs" aria-label="Ruta de navegación">
      <Link to="/" className="topbar-brand" aria-label="Panel principal">
        <img src="/icons/logo.png" alt="" width={18} height={18} />
        GANADERO
      </Link>
      {crumbs.map((crumb, i) => {
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