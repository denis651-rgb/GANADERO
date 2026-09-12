import { Component, type ErrorInfo, type PropsWithChildren, type ReactNode } from 'react'
import { AlertTriangle, ArrowLeft } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'

interface Props extends PropsWithChildren {
  onBack?: () => void
}

interface State {
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary] Error no controlado al renderizar:', error, info.componentStack)
  }

  render(): ReactNode {
    const { error } = this.state
    if (!error) return this.props.children
    return <div className="page-stack">
      <Card style={{ textAlign: 'center' }}>
        <AlertTriangle size={32} aria-hidden="true" />
        <h2>Ocurrió un error inesperado</h2>
        <p className="muted">{error.message || 'La página no pudo mostrarse correctamente.'}</p>
        <div className="inline-actions" style={{ justifyContent: 'center' }}>
          {this.props.onBack && <Button variant="ghost" onClick={() => { this.setState({ error: null }); this.props.onBack?.() }}><ArrowLeft size={17} aria-hidden="true" />Volver atrás</Button>}
          <Button onClick={() => this.setState({ error: null })}>Volver a intentar</Button>
        </div>
      </Card>
    </div>
  }
}
