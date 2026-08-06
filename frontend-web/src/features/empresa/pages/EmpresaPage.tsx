import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlarmClock, Building2, Coins, FileText, Hash, Image, Info, Mail, MapPin, Phone,
  RotateCcw, Save, Scale, ShieldCheck, SlidersHorizontal, Store, Syringe,
} from 'lucide-react'
import { getConfiguracionEmpresa, getEmpresa, updateConfiguracion, updateEmpresa } from '@/features/empresa/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { PageHeader } from '@/shared/components/PageHeader'
import { normalizeApiError } from '@/shared/api/errors'

export function EmpresaPage() {
  const client = useQueryClient()
  const query = useQuery({
    queryKey: ['empresa-completa'],
    queryFn: async () => {
      const [empresa, configuracion] = await Promise.all([getEmpresa(), getConfiguracionEmpresa()])
      return { empresa, configuracion }
    },
  })
  const mutation = useMutation({
    mutationFn: async (form: HTMLFormElement) => {
      if (!query.data) return
      const data = new FormData(form)
      await Promise.all([
        updateEmpresa({
          razonSocial: String(data.get('razonSocial') ?? ''), nombreComercial: String(data.get('nombreComercial') ?? ''),
          nit: String(data.get('nit') ?? ''), telefono: String(data.get('telefono') ?? ''),
          email: String(data.get('email') ?? ''), direccion: String(data.get('direccion') ?? ''),
          version: query.data.empresa.version,
        }),
        updateConfiguracion({
          moneda: String(data.get('moneda') ?? 'BOB').toUpperCase(),
          diasAlertaPreparto: Number(data.get('diasAlertaPreparto')),
          diasAlertaVacunacion: Number(data.get('diasAlertaVacunacion')),
          diasSinPesaje: Number(data.get('diasSinPesaje')),
          permitirStockNegativo: data.get('permitirStockNegativo') === 'on',
          requiereAprobacionVenta: data.get('requiereAprobacionVenta') === 'on',
          comprimirImagenes: data.get('comprimirImagenes') === 'on',
          calidadImagen: Number(data.get('calidadImagen')),
          version: query.data.configuracion.version,
        }),
      ])
    },
    onSuccess: () => client.invalidateQueries({ queryKey: ['empresa-completa'] }),
  })
  const error = query.error ?? mutation.error
  const empresa = query.data?.empresa
  const configuracion = query.data?.configuracion
  const initials = (empresa?.nombreComercial || empresa?.razonSocial || 'E')
    .split(/\s+/).filter(Boolean).map((word) => word[0]).join('').slice(0, 2).toUpperCase()

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Administración"
        title="Empresa"
        description="Datos generales y reglas operativas aplicadas a toda la organización."
        actions={<Button type="submit" form="empresa-form" loading={mutation.isPending}><Save size={18} />Guardar cambios</Button>}
      />
      {query.isPending && <LoadingState message="Cargando empresa…" />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {mutation.isSuccess && !query.isPending && <Alert tone="success">Cambios guardados correctamente.</Alert>}

      {query.data && empresa && configuracion && (
        <form
          id="empresa-form"
          key={`${empresa.version}-${configuracion.version}`}
          className="empresa-layout"
          onSubmit={(event) => { event.preventDefault(); mutation.mutate(event.currentTarget) }}
        >
          <div className="empresa-main">
            <Card className="empresa-hero">
              <span className="empresa-avatar">{initials}</span>
              <div>
                <h3>{empresa.nombreComercial || empresa.razonSocial}</h3>
                <p>{empresa.razonSocial}{empresa.nit ? ` · NIT ${empresa.nit}` : ''}</p>
                <div className="hero-meta">
                  <span className="status-badge status-synced">Activa</span>
                  {empresa.codigo && <span className="hero-code"><Hash size={12} />{empresa.codigo}</span>}
                </div>
              </div>
            </Card>

            <Card>
              <h3 className="settings-title"><Building2 size={17} />Datos generales</h3>
              <div className="form-grid">
                <Field label="Razón social" icon={<Building2 size={18} />}>
                  <input name="razonSocial" required defaultValue={empresa.razonSocial} />
                </Field>
                <Field label="Nombre comercial" icon={<Store size={18} />}>
                  <input name="nombreComercial" required defaultValue={empresa.nombreComercial} />
                </Field>
                <Field label="NIT" icon={<Hash size={18} />}>
                  <input name="nit" defaultValue={empresa.nit ?? ''} />
                </Field>
                <Field label="Teléfono" icon={<Phone size={18} />}>
                  <input name="telefono" type="tel" defaultValue={empresa.telefono ?? ''} />
                </Field>
                <Field label="Correo" icon={<Mail size={18} />}>
                  <input name="email" type="email" defaultValue={empresa.email ?? ''} />
                </Field>
                <Field label="Dirección" icon={<MapPin size={18} />}>
                  <input name="direccion" defaultValue={empresa.direccion ?? ''} />
                </Field>
              </div>
            </Card>
          </div>

          <div className="empresa-side">
            <Card>
              <h3 className="settings-title"><SlidersHorizontal size={17} />Reglas operativas</h3>
              <div className="settings-grid">
                <Field label="Moneda" icon={<Coins size={18} />} hint="Código ISO 4217 (BOB, USD…)">
                  <input name="moneda" required minLength={3} maxLength={3} list="monedas-sugeridas" defaultValue={configuracion.moneda} />
                  <datalist id="monedas-sugeridas">
                    <option value="BOB" /><option value="USD" /><option value="EUR" />
                  </datalist>
                </Field>
                <Field label="Alerta preparto (días)" icon={<AlarmClock size={18} />}>
                  <input name="diasAlertaPreparto" type="number" min="0" defaultValue={configuracion.diasAlertaPreparto} />
                </Field>
                <Field label="Alerta vacunación (días)" icon={<Syringe size={18} />}>
                  <input name="diasAlertaVacunacion" type="number" min="0" defaultValue={configuracion.diasAlertaVacunacion} />
                </Field>
                <Field label="Días sin pesaje" icon={<Scale size={18} />}>
                  <input name="diasSinPesaje" type="number" min="0" defaultValue={configuracion.diasSinPesaje} />
                </Field>
                <Field label="Calidad de imagen (%)" icon={<Image size={18} />}>
                  <input name="calidadImagen" type="number" min="1" max="100" defaultValue={configuracion.calidadImagen} />
                </Field>
              </div>

              <div className="settings-section">
                <h3 className="settings-title"><ShieldCheck size={17} />Preferencias</h3>
                <label className="toggle-row">
                  <span>
                    <strong>Permitir stock negativo</strong>
                    <small>Permite registrar existencias por debajo de cero.</small>
                  </span>
                  <span className="toggle">
                    <input name="permitirStockNegativo" type="checkbox" defaultChecked={configuracion.permitirStockNegativo} />
                    <span className="toggle-track" />
                  </span>
                </label>
                <label className="toggle-row">
                  <span>
                    <strong>Requerir aprobación de ventas</strong>
                    <small>Las ventas deben ser aprobadas antes de confirmarse.</small>
                  </span>
                  <span className="toggle">
                    <input name="requiereAprobacionVenta" type="checkbox" defaultChecked={configuracion.requiereAprobacionVenta} />
                    <span className="toggle-track" />
                  </span>
                </label>
                <label className="toggle-row">
                  <span>
                    <strong>Comprimir imágenes</strong>
                    <small>Reduce el peso de las imágenes al subirlas.</small>
                  </span>
                  <span className="toggle">
                    <input name="comprimirImagenes" type="checkbox" defaultChecked={configuracion.comprimirImagenes} />
                    <span className="toggle-track" />
                  </span>
                </label>
              </div>
            </Card>

            <Card className="empresa-summary">
              <h3 className="settings-title"><FileText size={17} />Configuración aplicada</h3>
              <dl>
                <div className="summary-row">
                  <dt><Coins size={15} />Moneda</dt>
                  <dd>{configuracion.moneda}</dd>
                </div>
                <div className="summary-row">
                  <dt><Scale size={15} />Unidad de peso</dt>
                  <dd>{configuracion.unidadPeso}</dd>
                </div>
                <div className="summary-row">
                  <dt><MapPin size={15} />Unidad de superficie</dt>
                  <dd>{configuracion.unidadSuperficie}</dd>
                </div>
              </dl>
            </Card>
          </div>

          <div className="save-bar">
            <span className="save-hint"><Info size={16} />Los cambios se aplican a toda la empresa.</span>
            <span className="save-actions">
              <Button type="reset" variant="ghost"><RotateCcw size={18} />Descartar</Button>
              <Button type="submit" loading={mutation.isPending}><Save size={18} />Guardar cambios</Button>
            </span>
          </div>
        </form>
      )}
    </div>
  )
}
