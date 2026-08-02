import { Sprout } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function AlimentacionPage() {
  return (
    <ModuleLandingPage
      title="Alimentaci\u00f3n"
      description="Raciones, planes y consumos relacionados con lotes."
      phase={4}
      icon={Sprout}
      capabilities={[
        "Definir raciones",
        "Planificar alimentaci\u00f3n",
        "Descontar inventario",
        "Calcular costos"
      ]}
      endpoints={[

      ]}
    />
  )
}
