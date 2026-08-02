import { ClipboardList } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function AuditoriaPage() {
  return (
    <ModuleLandingPage
      title="Auditor\u00eda"
      description="Trazabilidad de usuario, entidad, cambios y correlaci\u00f3n."
      phase={1}
      icon={ClipboardList}
      capabilities={[
        "Consultar acciones cr\u00edticas",
        "Filtrar por usuario y m\u00f3dulo",
        "Mostrar valores anteriores y nuevos",
        "Proteger registros contra edici\u00f3n"
      ]}
      endpoints={[
        "GET /api/v1/auditoria"
      ]}
    />
  )
}
