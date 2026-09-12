import type { ReactNode } from 'react'

/** Tabla del contenido Markdown envuelta en un contenedor con scroll horizontal para tablas anchas. */
export function ManualTable({ children }: { children?: ReactNode }) {
  return (
    <div className="manual-table-wrapper">
      <table>{children}</table>
    </div>
  )
}
