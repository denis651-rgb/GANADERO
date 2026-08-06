import { LoaderCircle } from 'lucide-react'

export function LoadingState({ message = 'Cargando…', fullscreen = false }: { message?: string; fullscreen?: boolean }) {
  return (
    <div className={fullscreen ? 'loading-state fullscreen' : 'loading-state'}>
      <LoaderCircle className="spin" size={28} />
      <span>{message}</span>
    </div>
  )
}
