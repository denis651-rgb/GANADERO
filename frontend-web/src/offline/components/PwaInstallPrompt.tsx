import { useEffect, useState } from 'react'
import { Download, Share } from 'lucide-react'
import { Button } from '@/shared/components/Button'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

function isIosSafari() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') return false
  const standalone = (navigator as Navigator & { standalone?: boolean }).standalone
  if (standalone) return false
  if (window.matchMedia?.('(display-mode: standalone)').matches) return false
  return /iPhone|iPad|iPod/i.test(navigator.userAgent)
}

export function PwaInstallPrompt() {
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [installed, setInstalled] = useState(false)
  const [dismissed, setDismissed] = useState(false)
  const [iosDismissed, setIosDismissed] = useState(false)
  const [ios, setIos] = useState(() => isIosSafari())

  useEffect(() => {
    const capture = (event: Event) => {
      event.preventDefault()
      setInstallPrompt(event as BeforeInstallPromptEvent)
      setIos(false)
    }
    const onInstalled = () => {
      setInstalled(true)
      setInstallPrompt(null)
      setIos(false)
    }
    window.addEventListener('beforeinstallprompt', capture)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', capture)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [])

  if (ios && !installed && !iosDismissed) {
    return (
      <div className="install-toast" role="dialog" aria-label="Instalación de la aplicación en iPhone o iPad">
        <div>
          <strong>Instala GANADERO</strong>
          <span>En Safari pulsa <Share size={13} style={{ display: 'inline', verticalAlign: '-2px' }} /> Compartir y luego «Añadir a pantalla de inicio» para usarla sin conexión.</span>
        </div>
        <Button variant="ghost" onClick={() => setIosDismissed(true)}>Ahora no</Button>
      </div>
    )
  }

  if (!installPrompt || dismissed || installed) return null

  return (
    <div className="install-toast" role="dialog" aria-label="Instalación de la aplicación">
      <div>
        <strong>Instala GANADERO</strong>
        <span>Añádela a tu pantalla de inicio para usarla sin conexión.</span>
      </div>
      <Button onClick={() => void installPrompt.prompt()}><Download size={16} />Instalar</Button>
      <Button variant="ghost" onClick={() => setDismissed(true)}>Ahora no</Button>
    </div>
  )
}
