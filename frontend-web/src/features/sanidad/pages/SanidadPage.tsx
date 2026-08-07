import { HeartPulse } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function SanidadPage() {
  return (
    <ModuleLandingPage
      title="Sanidad"
      description="Planes sanitarios, jornadas, tratamientos y cuarentenas."
      phase={3}
      icon={HeartPulse}
      status="EN_DESARROLLO"
      capabilities={[
        "Planes sanitarios",
        "Vacunaci\u00f3n por lote",
        "Casos cl\u00ednicos",
        "Periodos de retiro"
      ]}
      endpoints={[
        "GET /api/v1/planes-sanitarios",
        "POST /api/v1/jornadas-sanitarias",
        "POST /api/v1/tratamientos"
      ]}
    />
  )
}
