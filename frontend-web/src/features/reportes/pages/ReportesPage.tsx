import { FileBarChart } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function ReportesPage() {
  return (
    <ModuleLandingPage
      title="Reportes"
      description="Indicadores productivos, sanitarios y econ\u00f3micos."
      phase={6}
      icon={FileBarChart}
      capabilities={[
        "Inventario ganadero",
        "Productividad",
        "Reproducci\u00f3n y sanidad",
        "Costos y rentabilidad"
      ]}
      endpoints={[

      ]}
    />
  )
}
