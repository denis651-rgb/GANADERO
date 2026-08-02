import { Route } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function MovimientosPage() {
  return (
    <ModuleLandingPage
      title="Movimientos"
      description="Traslados confirmados entre propiedades, potreros y lotes."
      phase={1}
      icon={Route}
      capabilities={[
        "Crear borradores",
        "Validar origen y destino",
        "Confirmar transaccionalmente",
        "Registrar l\u00ednea de tiempo y auditor\u00eda"
      ]}
      endpoints={[
        "POST /api/v1/movimientos",
        "GET /api/v1/movimientos",
        "POST /api/v1/movimientos/{id}/confirmar",
        "POST /api/v1/movimientos/{id}/anular"
      ]}
    />
  )
}
