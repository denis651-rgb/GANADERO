import { useCallback, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastTone } from '@/shared/toast/useToast'
import { FloatingNotice } from './FloatingNotice'

interface Toast {
  id: number
  message: string
  tone: ToastTone
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
  }, [])

  return <ToastContext.Provider value={{ showToast }}>
    {children}
    {toasts.map((toast) => <FloatingNotice key={toast.id} tone={toast.tone} onClose={() => dismiss(toast.id)}>
      {toast.message}
    </FloatingNotice>)}
  </ToastContext.Provider>
}
