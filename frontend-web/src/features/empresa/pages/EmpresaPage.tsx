import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Save } from 'lucide-react'
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

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Administración" title="Empresa y configuración" description="Datos generales y reglas operativas aplicadas a toda la empresa." />
      {query.isPending && <LoadingState message="Cargando empresa…" />}
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {mutation.isSuccess && <Alert tone="success">Configuración guardada correctamente.</Alert>}
      {query.data && (
        <form key={`${query.data.empresa.version}-${query.data.configuracion.version}`} className="page-stack" onSubmit={(event) => { event.preventDefault(); mutation.mutate(event.currentTarget) }}>
          <Card>
            <h3><Building2 size={20} /> Datos generales</h3>
            <div className="form-grid">
              <Field label="Razón social"><input name="razonSocial" required defaultValue={query.data.empresa.razonSocial} /></Field>
              <Field label="Nombre comercial"><input name="nombreComercial" required defaultValue={query.data.empresa.nombreComercial} /></Field>
              <Field label="NIT"><input name="nit" defaultValue={query.data.empresa.nit} /></Field>
              <Field label="Teléfono"><input name="telefono" defaultValue={query.data.empresa.telefono} /></Field>
              <Field label="Correo"><input name="email" type="email" defaultValue={query.data.empresa.email} /></Field>
              <Field label="Dirección"><input name="direccion" defaultValue={query.data.empresa.direccion} /></Field>
            </div>
          </Card>
          <Card>
            <h3>Reglas operativas</h3>
            <div className="form-grid">
              <Field label="Moneda"><input name="moneda" required minLength={3} maxLength={3} defaultValue={query.data.configuracion.moneda} /></Field>
              <Field label="Alerta preparto (días)"><input name="diasAlertaPreparto" type="number" min="0" defaultValue={query.data.configuracion.diasAlertaPreparto} /></Field>
              <Field label="Alerta vacunación (días)"><input name="diasAlertaVacunacion" type="number" min="0" defaultValue={query.data.configuracion.diasAlertaVacunacion} /></Field>
              <Field label="Días sin pesaje"><input name="diasSinPesaje" type="number" min="0" defaultValue={query.data.configuracion.diasSinPesaje} /></Field>
              <Field label="Calidad de imagen"><input name="calidadImagen" type="number" min="1" max="100" defaultValue={query.data.configuracion.calidadImagen} /></Field>
              <div className="checkbox-stack">
                <label><input name="permitirStockNegativo" type="checkbox" defaultChecked={query.data.configuracion.permitirStockNegativo} /> Permitir stock negativo</label>
                <label><input name="requiereAprobacionVenta" type="checkbox" defaultChecked={query.data.configuracion.requiereAprobacionVenta} /> Aprobar ventas</label>
                <label><input name="comprimirImagenes" type="checkbox" defaultChecked={query.data.configuracion.comprimirImagenes} /> Comprimir imágenes</label>
              </div>
            </div>
          </Card>
          <div className="form-actions"><Button type="submit" loading={mutation.isPending}><Save size={18} />Guardar cambios</Button></div>
        </form>
      )}
    </div>
  )
}
