import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'
import type { ToastTone } from './useToast'

export function FloatingNotice({ tone, children, onClose }: { tone: ToastTone; children: ReactNode; onClose?: () => void }) {
  const [container] = useState(() => document.createElement('div'))
  const [visible, setVisible] = useState(true)
  const closeRef = useRef(onClose)
  useEffect(() => { closeRef.current = onClose }, [onClose])

  useEffect(() => {
    let region = document.getElementById('app-notifications')
    if (!region) {
      region = document.createElement('div')
      region.id = 'app-notifications'
      region.className = 'toast-region'
      document.body.appendChild(region)
    }
    region.appendChild(container)
    const timer = window.setTimeout(() => {
      setVisible(false)
      closeRef.current?.()
    }, 4000)
    return () => {
      window.clearTimeout(timer)
      container.remove()
      if (!region.childElementCount) region.remove()
    }
  }, [container])

  const Icon = tone === 'danger' ? AlertCircle : tone === 'warning' ? AlertTriangle : tone === 'success' ? CheckCircle2 : Info
  return visible ? createPortal(
    <div className={`toast toast-${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <Icon size={20} aria-hidden="true" />
      <div className="toast-content">{children}</div>
      <button type="button" className="toast-close" aria-label="Cerrar notificación" onClick={() => { setVisible(false); closeRef.current?.() }}><X size={16} aria-hidden="true" /></button>
    </div>, container,
  ) : null
}
