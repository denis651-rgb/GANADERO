import { Scale } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function PesajesPage() {
  return (
    <ModuleLandingPage
      title="Pesajes y productividad"
      description="Controles individuales, grupales y ganancia diaria."
      phase={2}
      icon={Scale}
      capabilities={[
        "Pesaje individual",
        "Pesaje por lote",
        "Curva de crecimiento",
        "Operaci\u00f3n offline"
      ]}
      endpoints={[
        "POST /api/v1/pesajes",
        "POST /api/v1/pesajes/lote",
        "GET /api/v1/pesajes"
      ]}
    />
  )
}
