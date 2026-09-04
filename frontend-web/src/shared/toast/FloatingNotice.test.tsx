import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Alert } from '@/shared/components/Alert'
import { ToastProvider } from './ToastProvider'
import { useToast } from './useToast'

function Trigger() {
  const { showToast } = useToast()
  return <button onClick={() => showToast('Guardado')}>Avisar</button>
}

afterEach(() => { cleanup(); vi.useRealTimers() })

describe('Avisos emergentes', () => {
  it('saca el error del formulario y lo cierra exactamente a los cuatro segundos', () => {
    vi.useFakeTimers()
    const { container } = render(<Alert tone="danger">Categoría incorrecta</Alert>)
    expect(container).toBeEmptyDOMElement()
    expect(screen.getByRole('alert').closest('#app-notifications')).not.toBeNull()
    act(() => vi.advanceTimersByTime(3999))
    expect(screen.getByRole('alert')).toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('apila avisos y toasts y permite cerrarlos manualmente', () => {
    render(<ToastProvider><Trigger /><Alert tone="warning">Advertencia</Alert></ToastProvider>)
    fireEvent.click(screen.getByText('Avisar'))
    expect(document.querySelectorAll('#app-notifications .toast')).toHaveLength(2)
    fireEvent.click(screen.getAllByLabelText('Cerrar notificación')[0])
    expect(screen.queryByText('Advertencia')).not.toBeInTheDocument()
    expect(screen.getByText('Guardado')).toBeInTheDocument()
  })

  it('no repite el mismo aviso al renderizar pero muestra un mensaje nuevo', () => {
    vi.useFakeTimers()
    const { rerender } = render(<Alert>Primero</Alert>)
    act(() => vi.advanceTimersByTime(4000))
    rerender(<Alert>Primero</Alert>)
    expect(screen.queryByText('Primero')).not.toBeInTheDocument()
    rerender(<Alert>Segundo</Alert>)
    expect(screen.getByText('Segundo')).toBeInTheDocument()
  })
})
