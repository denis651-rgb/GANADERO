import { useEffect, useState } from 'react'
import { Download } from 'lucide-react'
import { Button } from '@/shared/components/Button'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export function PwaInstallPrompt() {
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [installed, setInstalled] = useState(false)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    const capture = (event: Event) => {
      event.preventDefault()
      setInstallPrompt(event as BeforeInstallPromptEvent)
    }
    const onInstalled = () => {
      setInstalled(true)
      setInstallPrompt(null)
    }
    window.addEventListener('beforeinstallprompt', capture)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', capture)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [])

  if (!installPrompt || dismissed || installed) return null

  return (
    <div className="install-toast" role="status">
      <div>
        <strong>Instala GANADERO</strong>
        <span>Añádela a tu pantalla de inicio para usarla sin conexión.</span>
      </div>
      <Button onClick={() => void installPrompt.prompt()}><Download size={16} />Instalar</Button>
      <Button variant="ghost" onClick={() => setDismissed(true)}>Ahora no</Button>
    </div>
  )
}
