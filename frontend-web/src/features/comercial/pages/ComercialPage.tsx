import { ShoppingCart } from 'lucide-react'
import { ModuleLandingPage } from '@/shared/components/ModuleLandingPage'

export function ComercialPage() {
  return (
    <ModuleLandingPage
      title="Compras y ventas"
      description="Terceros, compras de insumos y operaciones con animales."
      phase={5}
      icon={ShoppingCart}
      capabilities={[
        "Proveedores y clientes",
        "Compras de productos",
        "Compra y venta de animales",
        "Validaciones sanitarias"
      ]}
      endpoints={[

      ]}
    />
  )
}
