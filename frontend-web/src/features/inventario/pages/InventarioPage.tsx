import { PackageSearch } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function InventarioPage() {
  return (
    <ModuleLandingPage
      title="Inventario"
      description="Productos, lotes, almacenes, existencias y movimientos."
      phase={4}
      icon={PackageSearch}
      capabilities={[
        "Control por lote",
        "Vencimientos",
        "Existencias por almac\u00e9n",
        "Ajustes y conteos"
      ]}
      endpoints={[
        "GET /api/v1/productos",
        "GET /api/v1/inventario/existencias",
        "POST /api/v1/inventario/ajustes"
      ]}
    />
  )
}
