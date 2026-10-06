import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Clock, Cloud, HelpCircle, RefreshCw, Save, Send, Smartphone, XCircle } from 'lucide-react'
import {
  getConfiguracionTelegram,
  getEstadoIntegracionTelegram,
  guardarConfiguracionTelegram,
  probarNotificacionTelegram,
  sincronizarAlertasFirebase,
  type GuardarTelegramInput,
} from '@/features/configuracion/telegramApi'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'
import { useToast } from '@/shared/toast/useToast'
import { useAuth } from '@/auth/auth-context'

export function TelegramPanel() {
  const client = useQueryClient()
  const { showToast } = useToast()
  const { can } = useAuth()
  const canEdit = can('SANIDAD_CONFIGURAR')

  const [testResult, setTestResult] = useState<{ exito: boolean; mensaje: string } | null>(null)
  const [tokenVisible, setTokenVisible] = useState(false)

  // Consultar configuración actual y estado del Relay
  const configQuery = useQuery({
    queryKey: ['configuracion-telegram'],
    queryFn: getConfiguracionTelegram,
  })

  const estadoQuery = useQuery({
    queryKey: ['estado-telegram'],
    queryFn: getEstadoIntegracionTelegram,
    refetchInterval: 15_000,
  })

  // Mutación para guardar configuración
  const saveMutation = useMutation({
    mutationFn: (data: GuardarTelegramInput) => guardarConfiguracionTelegram(data),
    onSuccess: () => {
      showToast('Configuración de Telegram guardada y sincronizada.')
      void client.invalidateQueries({ queryKey: ['configuracion-telegram'] })
      void client.invalidateQueries({ queryKey: ['estado-telegram'] })
    },
  })

  // Mutación para enviar prueba inmediata al celular
  const testMutation = useMutation({
    mutationFn: (data: { botToken?: string; chatId?: string }) => probarNotificacionTelegram(data),
    onSuccess: (result) => {
      setTestResult(result)
      if (result.exito) {
        showToast('¡Mensaje de prueba enviado a Telegram! Revisa tu celular.')
      }
    },
    onError: (err) => {
      const errorMsg = normalizeApiError(err).message
      setTestResult({ exito: false, mensaje: errorMsg })
    },
  })

  // Mutación para sincronizar alertas pendientes con Firebase
  const syncMutation = useMutation({
    mutationFn: sincronizarAlertasFirebase,
    onSuccess: (cantidad) => {
      showToast(`Sincronización completa: ${cantidad} recordatorio(s) actualizados en Firebase.`)
      void client.invalidateQueries({ queryKey: ['estado-telegram'] })
    },
  })

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const botToken = String(form.get('botToken') ?? '').trim()
    const chatIdPrincipal = String(form.get('chatIdPrincipal') ?? '').trim()
    const horaNotificacion = String(form.get('horaNotificacion') ?? '07:00').trim()
    const activo = form.get('activo') === 'on'
    const notificarSanidad = form.get('notificarSanidad') === 'on'
    const notificarReproduccion = form.get('notificarReproduccion') === 'on'

    saveMutation.mutate({
      botToken,
      chatIdPrincipal,
      activo,
      notificarSanidad,
      notificarReproduccion,
      horaNotificacion,
    })
  }

  const handleTestNotification = (formElement: HTMLFormElement | null) => {
    if (!formElement) return
    const form = new FormData(formElement)
    const botToken = String(form.get('botToken') ?? '').trim()
    const chatId = String(form.get('chatIdPrincipal') ?? '').trim()
    testMutation.mutate({ botToken, chatId })
  }

  if (configQuery.isPending) {
    return (
      <Card>
        <LoadingState message="Cargando configuración de Telegram..." />
      </Card>
    )
  }

  const config = configQuery.data
  const estado = estadoQuery.data

  return (
    <div className="page-stack">
      {/* Tarjeta de Estado de Conexión */}
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Smartphone size={20} className="text-primary" />
              Notificaciones Móviles (Telegram + Firebase Relay 24/7)
            </h3>
            <p className="muted" style={{ margin: '0.25rem 0 0 0' }}>
              Recibe avisos de vacunas, tratamientos y partos en tu celular aunque la computadora esté apagada.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.35rem 0.75rem',
              borderRadius: '999px',
              backgroundColor: estado?.telegramConfigurado ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
              color: estado?.telegramConfigurado ? '#16a34a' : '#dc2626',
              fontSize: '0.875rem',
              fontWeight: 500,
            }}>
              {estado?.telegramConfigurado ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
              Telegram: {estado?.telegramConfigurado ? 'Configurado' : 'No configurado'}
            </div>

            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.35rem 0.75rem',
              borderRadius: '999px',
              backgroundColor: estado?.firebaseConectado ? 'rgba(34, 197, 94, 0.15)' : 'rgba(234, 179, 8, 0.15)',
              color: estado?.firebaseConectado ? '#16a34a' : '#ca8a04',
              fontSize: '0.875rem',
              fontWeight: 500,
            }}>
              <Cloud size={16} />
              Firebase Relay: {estado?.firebaseConectado ? 'Conectado (24/7)' : 'Modo local (Sin JSON)'}
            </div>
          </div>
        </div>

        {estado && estado.alertasPendientesSincronizacion > 0 && (
          <div style={{ marginTop: '1rem' }}>
            <Alert tone="warning">
              Tienes <strong>{estado.alertasPendientesSincronizacion}</strong> alerta(s) pendiente(s) de sincronizar con Firebase.
              <Button
                variant="secondary"
                style={{ marginLeft: '1rem' }}
                loading={syncMutation.isPending}
                onClick={() => syncMutation.mutate()}
              >
                <RefreshCw size={14} /> Sincronizar ahora
              </Button>
            </Alert>
          </div>
        )}
      </Card>

      {/* Formulario de Configuración */}
      <Card>
        <form
          id="telegram-form"
          className="form-grid"
          onSubmit={handleSubmit}
        >
          <Field
            label="Token del Bot de Telegram"
            hint="Obtenido al crear tu bot con @BotFather en Telegram (ej: 7123456789:AAFxxx...)"
            disabled={!canEdit}
          >
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                name="botToken"
                type={tokenVisible ? 'text' : 'password'}
                defaultValue={config?.botToken ?? ''}
                placeholder="Ingresa el token de BotFather"
                required
                disabled={!canEdit}
                style={{ flex: 1 }}
              />
              <Button
                type="button"
                variant="secondary"
                onClick={() => setTokenVisible(!tokenVisible)}
              >
                {tokenVisible ? 'Ocultar' : 'Mostrar'}
              </Button>
            </div>
          </Field>

          <Field
            label="Chat ID de Telegram"
            hint="Tu ID numérico personal o el ID del grupo ganadero (puedes consultarlo con @userinfobot en Telegram)"
            disabled={!canEdit}
          >
            <input
              name="chatIdPrincipal"
              type="text"
              defaultValue={config?.chatIdPrincipal ?? ''}
              placeholder="Ej: 123456789 o -100123456789 para grupos"
              required
              disabled={!canEdit}
            />
          </Field>

          <Field
            label="Hora de notificación diaria"
            hint="Hora en la que el Relay en la nube despachará los avisos del día a tu celular."
            disabled={!canEdit}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Clock size={16} className="text-muted" />
              <input
                name="horaNotificacion"
                type="time"
                defaultValue={config?.horaNotificacion ?? '07:00'}
                disabled={!canEdit}
              />
            </div>
          </Field>

          <div className="form-full" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.5rem' }}>
            <label className="checkbox-line">
              <input
                name="activo"
                type="checkbox"
                defaultChecked={config?.activo ?? true}
                disabled={!canEdit}
              />
              <strong>Habilitar notificaciones a Telegram</strong>
            </label>

            <label className="checkbox-line">
              <input
                name="notificarSanidad"
                type="checkbox"
                defaultChecked={config?.notificarSanidad ?? true}
                disabled={!canEdit}
              />
              Notificar eventos de Sanidad (Vacunaciones, Tratamientos, Desparasitaciones)
            </label>

            <label className="checkbox-line">
              <input
                name="notificarReproduccion"
                type="checkbox"
                defaultChecked={config?.notificarReproduccion ?? true}
                disabled={!canEdit}
              />
              Notificar eventos de Reproducción (Partos próximos, Celos detectados, Palpaciones)
            </label>
          </div>

          {/* Resultado de la prueba */}
          {testResult && (
            <div className="form-full">
              <Alert tone={testResult.exito ? 'success' : 'danger'}>
                {testResult.mensaje}
              </Alert>
            </div>
          )}

          {/* Acciones */}
          {canEdit && (
            <div className="form-actions form-full" style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <Button
                type="submit"
                loading={saveMutation.isPending}
              >
                <Save size={16} /> Guardar configuración
              </Button>

              <Button
                type="button"
                variant="secondary"
                loading={testMutation.isPending}
                onClick={() => {
                  const form = document.getElementById('telegram-form') as HTMLFormElement
                  handleTestNotification(form)
                }}
              >
                <Send size={16} /> Probar en mi celular
              </Button>

              {estado?.firebaseConectado && (
                <Button
                  type="button"
                  variant="secondary"
                  loading={syncMutation.isPending}
                  onClick={() => syncMutation.mutate()}
                >
                  <RefreshCw size={16} /> Forzar Sincronización Nube
                </Button>
              )}
            </div>
          )}
        </form>
      </Card>

      {/* Guía de Ayuda Rápida */}
      <Card>
        <h4 style={{ margin: '0 0 0.75rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <HelpCircle size={18} /> ¿Cómo configurar tu Bot de Telegram en 2 minutos?
        </h4>
        <ol style={{ paddingLeft: '1.25rem', lineHeight: '1.6', margin: 0, color: 'var(--color-text-muted, #64748b)' }}>
          <li>
            Abre Telegram en tu celular o PC y busca el contacto oficial <strong>@BotFather</strong>.
          </li>
          <li>
            Escribe <code>/newbot</code>, asígnale un nombre (ej. <em>GanaderoAlertasBot</em>) y copia el <strong>Token</strong> generado en el campo de arriba.
          </li>
          <li>
            Inicia una conversación con tu nuevo bot presionando <strong>INICIAR / START</strong>.
          </li>
          <li>
            Para conocer tu <strong>Chat ID</strong>, busca el bot <strong>@userinfobot</strong> en Telegram y te responderá tu número de ID.
          </li>
          <li>
            Pega tu Chat ID arriba y presiona <strong>&quot;Probar en mi celular&quot;</strong>. ¡Tu teléfono sonará al instante!
          </li>
        </ol>
      </Card>
    </div>
  )
}
