import { useCallback, useRef, useState, type ReactNode } from 'react'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { ToastContext, type ToastTone } from '@/shared/toast/useToast'

interface Toast {
  id: number
  message: string
  tone: ToastTone
}

const toastIcons: Record<ToastTone, ReactNode> = {
  success: <CheckCircle2 size={19} aria-hidden="true" />,
  danger: <AlertCircle size={19} aria-hidden="true" />,
  info: <Info size={19} aria-hidden="true" />,
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => {
    setToasts((items) => items.filter((item) => item.id !== id))
  }, [])

  const showToast = useCallback((message: string, tone: ToastTone = 'success') => {
    const id = nextId.current++
    setToasts((items) => [...items, { id, message, tone }])
    window.setTimeout(() => dismiss(id), 4500)
  }, [dismiss])

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="toast-region" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.tone}`} role="status">
            {toastIcons[toast.tone]}
            <span>{toast.message}</span>
            <button type="button" className="toast-close" onClick={() => dismiss(toast.id)} aria-label="Cerrar notificación">
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
