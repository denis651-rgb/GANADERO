import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, Clock, HeartPulse, Save, Syringe } from 'lucide-react'
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
        diasAlertaPreparto2: Number(data.get('diasAlertaPreparto2')),
        diasAlertaPreparto3: Number(data.get('diasAlertaPreparto3')),
        diasAlertaVacunacion: Number(data.get('diasAlertaVacunacion')),
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

  const error = config.error ?? saveConfig.error
  const canEditConfig = can('CONFIGURACION_EDITAR')

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Mi finca" title="Configuración general" description="Ajustes operativos, de alertas y de reproducción de la finca." />
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}

      {config.isPending && <Card><LoadingState message="Cargando configuración…" /></Card>}

      {config.data && <form onSubmit={(event) => { event.preventDefault(); saveConfig.mutate(event.currentTarget) }}>
        <div className="page-stack">
          {/* Horario y Alertas Matutinas */}
          <Card>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', margin: '0 0 1rem 0' }}>
              <Clock size={18} className="text-primary" /> Horario y Tiempos de Notificaciones
            </h3>
            <div className="form-grid">
              <Field
                label="Hora matutina de los avisos"
                hint="Hora en la que se despachan las alertas del día a Telegram y al panel (ej. 07:00 a. m. antes de salir al campo sin señal)."
                disabled={!canEditConfig}
              >
                <input name="horaAvisos" type="time" required defaultValue={config.data.horaAvisos ?? '07:00'} disabled={!canEditConfig} />
              </Field>
            </div>
          </Card>

          {/* Alertas de Reproducción y Partos */}
          <Card>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', margin: '0 0 1rem 0' }}>
              <HeartPulse size={18} className="text-primary" /> Alertas de Reproducción y Partos Escalonados
            </h3>
            <div className="form-grid">
              <Field label="1er aviso preparto (días antes)" hint="Primer aviso para traslado al potrero o lote de maternidad." disabled={!canEditConfig}>
                <input name="diasAlertaPreparto" type="number" min="0" defaultValue={config.data.diasAlertaPreparto ?? 15} disabled={!canEditConfig} />
              </Field>
              <Field label="2do aviso preparto (días antes)" hint="Seguimiento intermedio de la vaca en maternidad." disabled={!canEditConfig}>
                <input name="diasAlertaPreparto2" type="number" min="0" defaultValue={config.data.diasAlertaPreparto2 ?? 7} disabled={!canEditConfig} />
              </Field>
              <Field label="3er aviso preparto (días antes)" hint="Aviso de parición inminente para alistar kit neonatal." disabled={!canEditConfig}>
                <input name="diasAlertaPreparto3" type="number" min="0" defaultValue={config.data.diasAlertaPreparto3 ?? 1} disabled={!canEditConfig} />
              </Field>
              <Field label="Días post-servicio para diagnóstico" hint="Días recomendados para palpación o ecografía post-monta/inseminación." disabled={!canEditConfig}>
                <input name="diasDiagnosticoPostServicio" type="number" min="0" defaultValue={config.data.diasDiagnosticoPostServicio ?? 30} disabled={!canEditConfig} />
              </Field>
              <Field label="Días de gestación estimada" hint="Duración promedio de preñez para cálculo del parto probable." disabled={!canEditConfig}>
                <input name="diasGestacionEstimada" type="number" min="1" defaultValue={config.data.diasGestacionEstimada ?? 285} disabled={!canEditConfig} />
              </Field>
              <Field label="Días de alerta de destete" hint="Días previos para planificar la separación de la cría." disabled={!canEditConfig}>
                <input name="diasAlertaDestete" type="number" min="0" defaultValue={config.data.diasAlertaDestete ?? 7} disabled={!canEditConfig} />
              </Field>
            </div>
          </Card>

          {/* Alertas de Sanidad y Vacunaciones */}
          <Card>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', margin: '0 0 1rem 0' }}>
              <Syringe size={18} className="text-primary" /> Alertas de Sanidad y Manejo
            </h3>
            <div className="form-grid">
              <Field label="Anticipación de avisos de vacunas (días antes)" hint="Aviso previo para preparar biológicos, cadena de frío y veterinario." disabled={!canEditConfig}>
                <input name="diasAlertaVacunacion" type="number" min="0" defaultValue={config.data.diasAlertaVacunacion ?? 1} disabled={!canEditConfig} />
              </Field>
              <Field label="Días sin pesaje para alertar" hint="Tiempo máximo de un animal sin control de peso." disabled={!canEditConfig}>
                <input name="diasSinPesaje" type="number" min="0" defaultValue={config.data.diasSinPesaje ?? 60} disabled={!canEditConfig} />
              </Field>
            </div>
          </Card>

          {/* Ajustes Generales y Preferencias */}
          <Card>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', margin: '0 0 1rem 0' }}>
              <Bell size={18} className="text-primary" /> Parámetros Generales de la Finca
            </h3>
            <div className="form-grid">
              <Field label="Zona horaria" disabled><input value={config.data.zonaHoraria} disabled /></Field>
              <Field label="Moneda" disabled><input value={config.data.moneda} disabled /></Field>
              <Field label="Unidad de peso" disabled><select value="KG" disabled><option value="KG">Kilogramos (KG)</option></select></Field>
              <Field label="Unidad de superficie" disabled={!canEditConfig}><select name="unidadSuperficie" defaultValue={config.data.unidadSuperficie} disabled={!canEditConfig}><option value="HA">Hectáreas (HA)</option><option value="M2">Metros cuadrados (M2)</option><option value="ACRE">Acres</option></select></Field>
              <Field label="Calidad de imagen (1-100)" hint="Calidad usada al comprimir fotos nuevas." disabled={!canEditConfig}><input name="calidadImagen" type="number" min="1" max="100" defaultValue={config.data.calidadImagen} disabled={!canEditConfig} /></Field>
              <Field label="Nombre de usuario" hint="Se muestra en la aplicación." disabled={!canEditConfig}><input name="nombreUsuario" maxLength={120} defaultValue={config.data.nombreUsuario ?? ''} disabled={!canEditConfig} /></Field>
              <div className="form-full">
                <label className="checkbox-line"><input name="comprimirImagenes" type="checkbox" defaultChecked={config.data.comprimirImagenes} disabled={!canEditConfig} /> Comprimir fotos al subirlas</label>
              </div>
            </div>

            {canEditConfig && (
              <div className="form-actions form-full" style={{ marginTop: '1.5rem' }}>
                <Button type="submit" loading={saveConfig.isPending}>
                  <Save size={17} aria-hidden="true" /> Guardar configuración
                </Button>
              </div>
            )}
          </Card>
        </div>
      </form>}
    </div>
  )
}
