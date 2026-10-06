import { TelegramPanel } from '@/features/configuracion/components/TelegramPanel'
import { PageHeader } from '@/shared/components/PageHeader'

export function TelegramPage() {
  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Mi finca"
        title="Notificaciones Móviles (Telegram)"
        description="Configura el Bot de Telegram y el Relay Cloud 24/7 para recibir avisos de sanidad y reproducción en tu celular."
      />
      <TelegramPanel />
    </div>
  )
}
