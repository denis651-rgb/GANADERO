import { WalletCards } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function FinanzasPage() {
  return (
    <ModuleLandingPage
      title="Finanzas"
      description="Gastos, ingresos, pagos y centros de costo."
      phase={5}
      icon={WalletCards}
      capabilities={[
        "Gastos por categor\u00eda",
        "Ingresos",
        "Centros de costo",
        "M\u00e1rgenes por animal y lote"
      ]}
      endpoints={[

      ]}
    />
  )
}
