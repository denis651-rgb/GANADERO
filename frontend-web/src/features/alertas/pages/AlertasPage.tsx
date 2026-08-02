import { Bell } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function AlertasPage() {
  return (
    <ModuleLandingPage
      title="Alertas"
      description="Reglas y eventos que requieren atenci\u00f3n del usuario."
      phase={3}
      icon={Bell}
      capabilities={[
        "Pr\u00f3ximos partos",
        "Vacunas pendientes",
        "Stock m\u00ednimo",
        "Animales sin pesaje"
      ]}
      endpoints={[

      ]}
    />
  )
}
