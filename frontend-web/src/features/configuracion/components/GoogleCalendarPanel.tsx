import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, RefreshCw, RotateCw, Save, Unlink, Upload } from 'lucide-react'
import { getConfiguracionCalendarioExterno, getEstadoSincronizacionCalendario, guardarConfiguracionCalendarioExterno, reintentarSincronizacionCalendario } from '@/features/configuracion/calendarioExternoApi'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

async function withStatusTimeout<T>(request: () => Promise<T>): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined
  try {
    return await Promise.race([
      request(),
      new Promise<never>((_, reject) => {
        timer = setTimeout(() => reject(new Error('La verificación con Google tardó demasiado. Intenta de nuevo.')), 10_000)
      }),
    ])
  } finally {
    clearTimeout(timer)
  }
}

const QUERY_KEY = ['configuracion-calendario-externo'] as const

export function GoogleCalendarPanel() {
  const client = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const desktop = window.ganadero?.googleCalendar
  const [syncMessage, setSyncMessage] = useState<string>()
  const backend = useQuery({ queryKey: QUERY_KEY, queryFn: getConfiguracionCalendarioExterno, retry: false })
  const syncState = useQuery({ queryKey: ['estado-sincronizacion-calendario'], queryFn: getEstadoSincronizacionCalendario, retry: false, refetchInterval: 30_000 })
  const oauth = useQuery({
    queryKey: ['google-calendar-oauth-desktop'],
    queryFn: () => withStatusTimeout(() => desktop!.status()),
    retry: false,
    enabled: Boolean(desktop),
  })
  const action = useMutation({
    mutationFn: async (action: { kind: 'import'; file: File } | { kind: 'connect' | 'change' | 'revoke' | 'sync' | 'retry' }) => {
      if (!desktop) throw new Error('La conexión con Google Calendar sólo está disponible en Ganadero Desktop.')
      if (action.kind === 'import') {
        if (action.file.size > 64 * 1024) throw new Error('El archivo OAuth supera el tamaño permitido de 64 KB.')
        return { status: await desktop.importClientConfig(await action.file.text(), action.file.name) }
      }
      if (action.kind === 'connect') return { status: await desktop.connect() }
      if (action.kind === 'change') return { status: await desktop.changeAccount() }
      if (action.kind === 'retry') {
        await reintentarSincronizacionCalendario()
        const sync = await desktop.syncNow()
        return { status: await desktop.status(), sync }
      }
      if (action.kind === 'sync') {
        const sync = await desktop.syncNow()
        return { status: await desktop.status(), sync }
      }
      return { status: await desktop.revoke() }
    },
    onSuccess: ({ status, sync }) => {
      client.setQueryData(['google-calendar-oauth-desktop'], status)
      if (sync) {
        setSyncMessage(sync.claimed === 0
          ? 'No había eventos pendientes. Recuerda que las actividades Manuales no generan eventos; usa Por edad, Periódica o Fecha programada.'
          : `Sincronización terminada: ${sync.completed} evento(s) enviado(s)${sync.failed ? ` y ${sync.failed} con error` : ''}.`)
      }
      void client.invalidateQueries({ queryKey: QUERY_KEY })
      void client.invalidateQueries({ queryKey: ['estado-sincronizacion-calendario'] })
    },
  })
  const save = useMutation({
    mutationFn: guardarConfiguracionCalendarioExterno,
    onSuccess: (data) => {
      client.setQueryData(QUERY_KEY, data)
      void client.invalidateQueries({ queryKey: ['estado-sincronizacion-calendario'] })
    },
  })

  const error = backend.error ?? oauth.error ?? syncState.error ?? action.error ?? save.error
  const connected = Boolean(oauth.data?.connected)
  const counts = new Map(syncState.data?.cola.map(item => [item.estado, item.cantidad]) ?? [])
  const pending = (counts.get('PENDIENTE') ?? 0) + (counts.get('REINTENTO') ?? 0) + (counts.get('PROCESANDO') ?? 0)
  const failed = counts.get('ERROR_DEFINITIVO') ?? 0

  return <Card>
    <div className="section-heading">
      <p className="muted">Conecta una cuenta para enviar las actividades sanitarias al calendario creado por Ganadero.</p>
      {backend.data && <span className={`status-badge ${connected ? 'status-activo' : 'status-inactivo'}`}>{connected ? 'AUTORIZADO' : backend.data.estado}</span>}
    </div>
    {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
    {syncMessage && <Alert tone="success">{syncMessage}</Alert>}
    {(backend.isLoading || (desktop && oauth.isLoading)) && <LoadingState message="Verificando conexión con Google…" />}
    {!desktop && <Alert tone="warning">Abre esta pantalla desde la aplicación Ganadero Desktop para conectar Google Calendar.</Alert>}
    {desktop && oauth.data && !oauth.data.available && <Alert tone="warning">{oauth.data.message}</Alert>}
    {oauth.data?.message && oauth.data.available && <Alert tone="warning">{oauth.data.message}</Alert>}
    {connected && <dl className="detail-list"><div><dt>Cuenta autorizada</dt><dd>{oauth.data?.email ?? backend.data?.cuentaEmail}</dd></div><div><dt>Alcance</dt><dd>Calendarios creados por Ganadero</dd></div><div><dt>Zona horaria</dt><dd>{backend.data?.zonaHoraria ?? 'America/La_Paz'}</dd></div></dl>}
    {backend.data && <form key={backend.data.version} className="form-grid" onSubmit={(event) => {
      event.preventDefault()
      const values = new FormData(event.currentTarget)
      save.mutate({ cuentaEmail: backend.data?.cuentaEmail, calendarioNombre: String(values.get('calendarName') ?? ''),
        zonaHoraria: String(values.get('timeZone') ?? ''), sincronizacionAutomatica: values.get('automatic') === 'on' })
    }}>
      <label className="field"><span className="field-label">Nombre del calendario</span><span className="field-control"><input name="calendarName" required maxLength={120} defaultValue={backend.data.calendarioNombre} /></span></label>
      <label className="field"><span className="field-label">Zona horaria</span><span className="field-control"><select name="timeZone" defaultValue={backend.data.zonaHoraria}><option value="America/La_Paz">Bolivia · America/La_Paz</option></select></span></label>
      <label className="checkbox-row"><input name="automatic" type="checkbox" defaultChecked={backend.data.sincronizacionAutomatica} /> Sincronizar automáticamente las actividades sanitarias</label>
      <div className="form-actions"><Button type="submit" variant="secondary" loading={save.isPending}><Save size={17} aria-hidden="true" />Guardar configuración</Button></div>
    </form>}
    {syncState.data && <dl className="detail-list">
      <div><dt>Calendario externo</dt><dd>{backend.data?.calendarioExternoId ? backend.data.calendarioNombre : 'Se creará al sincronizar'}</dd></div>
      <div><dt>Pendientes</dt><dd>{pending}</dd></div><div><dt>Errores definitivos</dt><dd>{failed}</dd></div>
      <div><dt>Última sincronización</dt><dd>{backend.data?.ultimaSincronizacion ? new Date(backend.data.ultimaSincronizacion).toLocaleString('es-BO') : 'Todavía no sincronizado'}</dd></div>
    </dl>}
    <div className="form-actions">
      {(backend.isError || oauth.isError) && <Button type="button" variant="secondary" disabled={backend.isFetching || oauth.isFetching} onClick={() => {
        void backend.refetch()
        if (desktop) void oauth.refetch()
      }}>Reintentar verificación</Button>}
      {!connected && <>
        <input ref={fileInputRef} type="file" accept="application/json,.json" hidden onChange={(event) => {
          const file = event.currentTarget.files?.[0]
          event.currentTarget.value = ''
          if (file) action.mutate({ kind: 'import', file })
        }} />
        <Button type="button" variant="secondary" loading={action.isPending} disabled={!desktop} onClick={() => fileInputRef.current?.click()}><Upload size={17} aria-hidden="true" />Importar JSON OAuth</Button>
      </>}
      {!connected && <Button type="button" loading={action.isPending} disabled={!desktop || oauth.data?.available===false} onClick={() => action.mutate({ kind: 'connect' })}><Link size={17} aria-hidden="true" />Conectar Google</Button>}
      {connected && <>
        <Button type="button" loading={action.isPending} onClick={() => { setSyncMessage(undefined); action.mutate({ kind: 'sync' }) }}><RotateCw size={17} aria-hidden="true" />Sincronizar ahora</Button>
        {failed > 0 && <Button type="button" variant="secondary" loading={action.isPending} onClick={() => action.mutate({ kind: 'retry' })}><RotateCw size={17} aria-hidden="true" />Reintentar errores</Button>}
        <Button type="button" variant="secondary" loading={action.isPending} onClick={() => action.mutate({ kind: 'change' })}><RefreshCw size={17} aria-hidden="true" />Cambiar cuenta</Button>
        <Button type="button" variant="danger" loading={action.isPending} onClick={() => action.mutate({ kind: 'revoke' })}><Unlink size={17} aria-hidden="true" />Revocar acceso</Button>
      </>}
    </div>
    <p className="muted">Revocar elimina los tokens cifrados de este equipo. Registrar o atender una alarma no se considera una actividad sanitaria realizada.</p>
  </Card>
}
