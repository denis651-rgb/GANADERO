import { useState } from 'react'
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
  const [pinMessage, setPinMessage] = useState<string | null>(null)

  const config = useQuery({ queryKey: ['configuracion'], queryFn: getConfiguracion })

  const saveConfig = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return updateConfiguracion({
        unidadPeso: 'KG',
        unidadSuperficie: String(data.get('unidadSuperficie') ?? '') || undefined,
        diasAlertaPreparto: Number(data.get('diasAlertaPreparto')),
        diasAlertaVacunacion: Number(data.get('diasAlertaVacunacion')),
        diasSinPesaje: Number(data.get('diasSinPesaje')),
        diasAlertaDestete: Number(data.get('diasAlertaDestete')),
        diasDiagnosticoPostServicio: Number(data.get('diasDiagnosticoPostServicio')),
        diasGestacionEstimada: Number(data.get('diasGestacionEstimada')),
        comprimirImagenes: data.get('comprimirImagenes') === 'on',
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
    onSuccess: (_data, form) => { form.reset(); setPinMessage('PIN actualizado.'); showToast('PIN actualizado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })
  const removePin = useMutation({
    mutationFn: () => updateConfiguracion({ quitarPin: true, version: config.data!.version }),
    onSuccess: () => { setPinMessage('PIN eliminado.'); showToast('PIN eliminado.'); void client.invalidateQueries({ queryKey: ['configuracion'] }) },
  })

  const error = config.error ?? saveConfig.error
  const canEditConfig = can('CONFIGURACION_EDITAR')

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Configuración general" description="Ajustes generales de la operación y bloqueo de la aplicación con PIN." />
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
          <Field label="Días de alerta de vacunación" disabled={!canEditConfig}><input name="diasAlertaVacunacion" type="number" min="0" defaultValue={config.data.diasAlertaVacunacion} disabled={!canEditConfig} /></Field>
          <Field label="Días sin pesaje para alertar" disabled={!canEditConfig}><input name="diasSinPesaje" type="number" min="0" defaultValue={config.data.diasSinPesaje} disabled={!canEditConfig} /></Field>
          <Field label="Días de alerta de destete" disabled={!canEditConfig}><input name="diasAlertaDestete" type="number" min="0" defaultValue={config.data.diasAlertaDestete} disabled={!canEditConfig} /></Field>
          <Field label="Días para diagnóstico post-servicio" disabled={!canEditConfig}><input name="diasDiagnosticoPostServicio" type="number" min="0" defaultValue={config.data.diasDiagnosticoPostServicio} disabled={!canEditConfig} /></Field>
          <Field label="Días de gestación estimada" disabled={!canEditConfig}><input name="diasGestacionEstimada" type="number" min="1" defaultValue={config.data.diasGestacionEstimada} disabled={!canEditConfig} /></Field>
          <Field label="Calidad de imagen (1-100)" disabled><input type="number" value={config.data.calidadImagen} disabled /></Field>
          <Field label="Nombre de usuario" hint="Se muestra en la aplicación." disabled={!canEditConfig}><input name="nombreUsuario" maxLength={120} defaultValue={config.data.nombreUsuario ?? ''} disabled={!canEditConfig} /></Field>
          <label className="checkbox-line"><input name="comprimirImagenes" type="checkbox" defaultChecked={config.data.comprimirImagenes} disabled={!canEditConfig} /> Comprimir fotos al subirlas</label>
          {canEditConfig && <div className="form-actions form-full"><Button type="submit" loading={saveConfig.isPending}><Save size={17} aria-hidden="true" />Guardar configuración</Button></div>}
        </form>
      </Card>}

      {canEditConfig && config.data && <Card>
        <h3><KeyRound size={17} aria-hidden="true" /> Bloqueo con PIN</h3>
        {config.data.pinConfigurado ? <p className="muted">La aplicación tiene un PIN configurado.</p> : <Alert tone="warning">Ningún PIN configurado: cualquiera puede abrir la aplicación.</Alert>}
        {pinMessage && <Alert tone="success">{pinMessage}</Alert>}
        {savePin.error && <Alert tone="danger">{normalizeApiError(savePin.error).message}</Alert>}
        <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); setPinMessage(null); savePin.mutate(event.currentTarget) }}>
          <Field label="Nuevo PIN"><input name="nuevoPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <Field label="Confirmar PIN"><input name="confirmacionPin" type="password" inputMode="numeric" minLength={4} maxLength={20} autoComplete="off" /></Field>
          <div className="form-actions">
            <Button type="submit" loading={savePin.isPending}>{config.data.pinConfigurado ? 'Cambiar PIN' : 'Configurar PIN'}</Button>
            {config.data.pinConfigurado && <Button type="button" variant="secondary" loading={removePin.isPending} onClick={() => { setPinMessage(null); removePin.mutate() }}>Quitar PIN</Button>}
          </div>
        </form>
      </Card>}
    </div>
  )
}
