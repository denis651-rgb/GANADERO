import { Link } from 'react-router'
import { Button } from '@/shared/components/Button'

export function NotFoundPage() {
  return (
    <main className="not-found">
      <img src="/logo.svg" alt="" width="72" height="72" />
      <p className="eyebrow">404</p>
      <h1>Página no encontrada</h1>
      <p>La dirección solicitada no existe dentro de GANADERO.</p>
      <Link to="/"><Button>Volver al panel</Button></Link>
    </main>
  )
}
