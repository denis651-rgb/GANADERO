import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, BellOff } from 'lucide-react'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { useToast } from '@/shared/toast/useToast'
import { useAuth } from '@/auth/auth-context'
import {
  getNotificationPreferences,
  getCurrentPushEndpoint,
  listPushDevices,
  pushSupported,
  saveNotificationPreferences,
  sendTestPush,
  subscribePush,
  unsubscribePush,
  type NotificationPreferences,
} from './push-api'

const labels: Record<keyof NotificationPreferences, string> = {
  reproduccion: 'Reproducción',
  sanidad: 'Sanidad',
  tratamientos: 'Tratamientos',
  pesajes: 'Pesajes',
  movimientos: 'Movimientos',
  inventario: 'Inventario',
  sistema: 'Sistema',
  casosCriticos: 'Casos críticos',
  criticas: 'Prioridad crítica',
  urgentes: 'Prioridad urgente',
  recordatorios: 'Recordatorios',
}

export function PushSettings() {
  const client = useQueryClient()
  const { showToast } = useToast()
  const { can } = useAuth()
  const devices = useQuery({ queryKey: ['push-devices'], queryFn: listPushDevices })
  const currentEndpoint = useQuery({ queryKey: ['push-current-endpoint'], queryFn: getCurrentPushEndpoint })
  const preferences = useQuery({
    queryKey: ['notification-preferences'],
    queryFn: getNotificationPreferences,
  })
  const enable = useMutation({
    mutationFn: () => subscribePush(navigator.platform || 'Este dispositivo'),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['push-devices'] })
      client.invalidateQueries({ queryKey: ['push-current-endpoint'] })
      showToast('Notificaciones activadas en este dispositivo.')
    },
  })
  const disable = useMutation({
    mutationFn: unsubscribePush,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['push-devices'] })
      client.invalidateQueries({ queryKey: ['push-current-endpoint'] })
    },
  })
  const save = useMutation({
    mutationFn: saveNotificationPreferences,
    onSuccess: (data) => {
      client.setQueryData(['notification-preferences'], data)
      showToast('Preferencias Push guardadas.')
    },
  })
  const testPush = useMutation({
    mutationFn: (subscriptionId: string) => sendTestPush(subscriptionId),
    onSuccess: () => showToast('Notificación de prueba enviada.'),
  })
  const current = devices.data?.find((device) => device.endpoint === currentEndpoint.data)
  const otherDevices = (devices.data?.length ?? 0) - (current ? 1 : 0)
  const permissionGranted = pushSupported() && Notification.permission === 'granted'

  return <Card>
    <h2>{current ? <Bell size={20} /> : <BellOff size={20} />} Notificaciones en este dispositivo</h2>
    {!pushSupported() && <Alert tone="info">Este navegador no admite Web Push.</Alert>}
    {enable.error && <Alert tone="danger">{enable.error.message}</Alert>}
    {testPush.error && <Alert tone="danger">{testPush.error.message}</Alert>}
    <p>Estado: <strong>{current ? 'Activadas' : 'Desactivadas'}</strong></p>
    {otherDevices > 0 && <p>{otherDevices} dispositivo(s) adicional(es) reciben notificaciones.</p>}
    {current
      ? <Button type="button" variant="secondary" loading={disable.isPending}
          onClick={() => disable.mutate(current.id)}>Desactivar</Button>
      : <Button type="button" disabled={!pushSupported()} loading={enable.isPending}
          onClick={() => enable.mutate()}>Activar notificaciones</Button>}
    {can('ALERTA_CONFIGURAR') && <Button type="button" variant="secondary"
      disabled={!current || !permissionGranted} loading={testPush.isPending}
      onClick={() => current && testPush.mutate(current.id)}>Enviar notificación de prueba</Button>}

    {preferences.data && <form className="form-grid" onSubmit={(event) => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      save.mutate(Object.fromEntries(
        (Object.keys(labels) as Array<keyof NotificationPreferences>).map((key) => [key, form.has(key)]),
      ) as unknown as NotificationPreferences)
    }}>
      <h3>Recibir notificaciones Push</h3>
      <p>Desactivar una opción no oculta las alertas del centro de alertas.</p>
      {(Object.entries(labels) as Array<[keyof NotificationPreferences, string]>).map(([key, label]) =>
        <label key={key}><input type="checkbox" name={key} defaultChecked={preferences.data[key]} /> {label}</label>)}
      <div className="form-actions"><Button type="submit" loading={save.isPending}>Guardar preferencias</Button></div>
    </form>}
  </Card>
}
