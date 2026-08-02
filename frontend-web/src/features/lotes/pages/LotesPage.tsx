import { Boxes } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function LotesPage() {
  return (
    <ModuleLandingPage
      title="Lotes ganaderos"
      description="Agrupaci\u00f3n operativa de animales y membres\u00edas hist\u00f3ricas."
      phase={1}
      icon={Boxes}
      capabilities={[
        "Crear lotes",
        "Asignar animales en grupo",
        "Cerrar membres\u00eda anterior",
        "Actualizar lote actual"
      ]}
      endpoints={[
        "GET /api/v1/lotes",
        "POST /api/v1/lotes",
        "GET /api/v1/lotes/{id}/animales",
        "POST /api/v1/lotes/{id}/animales"
      ]}
    />
  )
}
