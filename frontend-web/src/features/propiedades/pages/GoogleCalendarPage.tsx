import { GoogleCalendarPanel } from '@/features/configuracion/components/GoogleCalendarPanel'
import { PageHeader } from '@/shared/components/PageHeader'

export function GoogleCalendarPage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Google Calendar" description="Sincroniza las actividades sanitarias con un calendario de Google." />
      <GoogleCalendarPanel />
    </div>
  )
}
