import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Save } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { getConfiguracion, updateConfiguracion } from '@/features/configuracion/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'
import { useToast } from '@/shared/toast/useToast'

export function ConfiguracionGeneralPage() {
  const client = useQueryClient()
  const { can } = useAuth()
  const { showToast } = useToast()

  const config = useQuery({ queryKey: ['configuracion'], queryFn: getConfiguracion })

  const saveConfig = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return updateConfiguracion({
        unidadPeso: 'KG',
        unidadSuperficie: String(data.get('unidadSuperficie') ?? '') || undefined,
        diasAlertaPreparto: Number(data.get('diasAlertaPreparto')),
        diasSinPesaje: Number(data.get('diasSinPesaje')),
        diasAlertaDestete: Number(data.get('diasAlertaDestete')),
        diasDiagnosticoPostServicio: Number(data.get('diasDiagnosticoPostServicio')),
        diasGestacionEstimada: Number(data.get('diasGestacionEstimada')),
        horaAvisos: String(data.get('horaAvisos') ?? '') || undefined,
        comprimirImagenes: data.get('comprimirImagenes') === 'on',
        calidadImagen: Number(data.get('calidadImagen')),
        nombreUsuario: String(data.get('nombreUsuario') ?? '') || undefined,
        version: config.data!.version,
      })
    },
    onSuccess: () => { showToast('Configuración guardada correctamente.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })
  const savePin = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const pin = String(data.get('nuevoPin') ?? '')
      const confirmacion = String(data.get('confirmacionPin') ?? '')
      if (pin !== confirmacion) throw new Error('El PIN y su confirmación no coinciden.')
      return updateConfiguracion({ nuevoPin: pin, version: config.data!.version })
    },
    onSuccess: (_data, form) => { form.reset(); showToast('PIN actualizado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })
  const removePin = useMutation({
    mutationFn: () => updateConfiguracion({ quitarPin: true, version: config.data!.version }),
    onSuccess: () => { showToast('PIN eliminado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })

  const error = config.error ?? saveConfig.error
  const canEditConfig = can('CONFIGURACION_EDITAR')

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Configuración general" description="Ajustes generales de la operación." />
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

      {config.isPending && <Card><LoadingState message="Cargando configuración…" /></Card>}

      {config.data && <Card>
        <h3>Configuración general</h3>
        <form className="form-grid" onSubmit={(event) => { event.preventDefault(); saveConfig.mutate(event.currentTarget) }}>
          <Field label="Zona horaria" disabled><input value={config.data.zonaHoraria} disabled /></Field>
          <Field label="Moneda" disabled><input value={config.data.moneda} disabled /></Field>
          <Field label="Unidad de peso" disabled><select value="KG" disabled><option value="KG">Kilogramos (KG)</option></select></Field>
          <Field label="Unidad de superficie" disabled={!canEditConfig}><select name="unidadSuperficie" defaultValue={config.data.unidadSuperficie} disabled={!canEditConfig}><option value="HA">Hectáreas (HA)</option><option value="M2">Metros cuadrados (M2)</option><option value="ACRE">Acres</option></select></Field>
          <Field label="Días de alerta antes del parto" disabled={!canEditConfig}><input name="diasAlertaPreparto" type="number" min="0" defaultValue={config.data.diasAlertaPreparto} disabled={!canEditConfig} /></Field>
          <Field label="Días sin pesaje para alertar" disabled={!canEditConfig}><input name="diasSinPesaje" type="number" min="0" defaultValue={config.data.diasSinPesaje} disabled={!canEditConfig} /></Field>
          <Field label="Días de alerta de destete" disabled={!canEditConfig}><input name="diasAlertaDestete" type="number" min="0" defaultValue={config.data.diasAlertaDestete} disabled={!canEditConfig} /></Field>
          <Field label="Días para diagnóstico post-servicio" disabled={!canEditConfig}><input name="diasDiagnosticoPostServicio" type="number" min="0" defaultValue={config.data.diasDiagnosticoPostServicio} disabled={!canEditConfig} /></Field>
          <Field label="Días de gestación estimada" disabled={!canEditConfig}><input name="diasGestacionEstimada" type="number" min="1" defaultValue={config.data.diasGestacionEstimada} disabled={!canEditConfig} /></Field>
          <Field label="Hora de los avisos" hint="Hora a la que salen los avisos que nacen de una fecha, como el parto probable, el destete o el fin de un retiro. Los días de anticipación siguen siendo los de arriba." disabled={!canEditConfig}><input name="horaAvisos" type="time" required defaultValue={config.data.horaAvisos} disabled={!canEditConfig} /></Field>
          <Field label="Calidad de imagen (1-100)" hint="Calidad usada al comprimir fotos nuevas." disabled={!canEditConfig}><input name="calidadImagen" type="number" min="1" max="100" defaultValue={config.data.calidadImagen} disabled={!canEditConfig} /></Field>
          <Field label="Nombre de usuario" hint="Se muestra en la aplicación." disabled={!canEditConfig}><input name="nombreUsuario" maxLength={120} defaultValue={config.data.nombreUsuario ?? ''} disabled={!canEditConfig} /></Field>
          <label className="checkbox-line"><input name="comprimirImagenes" type="checkbox" defaultChecked={config.data.comprimirImagenes} disabled={!canEditConfig} /> Comprimir fotos al subirlas</label>
          {canEditConfig && <div className="form-actions form-full"><Button type="submit" loading={saveConfig.isPending}><Save size={17} aria-hidden="true" />Guardar configuración</Button></div>}
        </form>
      </Card>}

      {canEditConfig && config.data && <Card>
        <h3><KeyRound size={17} aria-hidden="true" /> Bloqueo con PIN</h3>
        <div className="alert alert-warning" role="note">
          <div>
            <strong>El bloqueo con PIN todavía no está en funcionamiento.</strong>
            <span>Puedes guardar un PIN, pero la aplicación no lo pide al abrirse: hoy no protege el acceso y cualquiera que abra Ganadero en este equipo ve los datos. Protege el equipo con la clave de inicio de sesión de Windows.</span>
          </div>
        </div>
        {config.data.pinConfigurado && <p className="muted">Hay un PIN guardado para cuando el bloqueo esté disponible.</p>}
        {savePin.error && <Alert tone="danger">{normalizeApiError(savePin.error).message}</Alert>}
        <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); savePin.mutate(event.currentTarget) }}>
          <Field label="Nuevo PIN"><input name="nuevoPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <Field label="Confirmar PIN"><input name="confirmacionPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <div className="form-actions">
            <Button type="submit" loading={savePin.isPending}>{config.data.pinConfigurado ? 'Cambiar PIN' : 'Configurar PIN'}</Button>
            {config.data.pinConfigurado && <Button type="button" variant="secondary" loading={removePin.isPending} onClick={() => removePin.mutate()}>Quitar PIN</Button>}
          </div>
        </form>
      </Card>}
    </div>
  )
}
