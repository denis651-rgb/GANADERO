import { Activity } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function ReproduccionPage() {
  return (
    <ModuleLandingPage
      title="Reproducci\u00f3n"
      description="Celo, servicios, diagn\u00f3sticos, partos y destetes."
      phase={3}
      icon={Activity}
      capabilities={[
        "Registrar servicios",
        "Diagn\u00f3stico de gestaci\u00f3n",
        "Partos y cr\u00edas",
        "Alertas reproductivas"
      ]}
      endpoints={[
        "POST /api/v1/reproduccion/servicios",
        "POST /api/v1/reproduccion/diagnosticos",
        "POST /api/v1/reproduccion/partos"
      ]}
    />
  )
}
