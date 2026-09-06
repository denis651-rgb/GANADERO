import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/auth/auth-context'
import { getConfiguracionSanitaria, guardarConfiguracionSanitaria, type ConfiguracionSanitaria } from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

export function ConfiguracionSanitariaPanel() {
  const { can } = useAuth()
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['sanidad-configuracion'], queryFn: getConfiguracionSanitaria })
  const guardar = useMutation({
    mutationFn: guardarConfiguracionSanitaria,
    onSuccess: (data) => { client.setQueryData(['sanidad-configuracion'], data) },
  })
  const editable = can('SANIDAD_PLAN_ADMINISTRAR')
  return <Card>
    <h3>Edades mínimas para examen reproductivo</h3>
    <p className="muted">Define las edades en meses con el responsable veterinario. No hay valores predefinidos. Cumplir la edad no acredita aptitud reproductiva ni modifica exámenes anteriores.</p>
    {query.isPending && <LoadingState message="Cargando configuración…" />}
    {query.error && <Alert tone="danger">{normalizeApiError(query.error).message}<button type="button" onClick={() => void query.refetch()}>Reintentar</button></Alert>}
    {query.data && <form key={query.data.version} className="form-grid" onSubmit={(e) => {
      e.preventDefault()
      if (!editable || guardar.isPending) return
      const values = new FormData(e.currentTarget)
      const input: ConfiguracionSanitaria = {
        edadMinMachoMeses: Number(values.get('macho')),
        edadMinHembraMeses: Number(values.get('hembra')),
        horizonteProyeccionMeses: Number(values.get('horizonteProyeccionMeses')),
        version: query.data.version,
      }
      guardar.mutate(input)
    }}>
      <Field label="Machos: edad mínima (meses)" required><input name="macho" type="number" min="1" max="120" step="1" required disabled={!editable} defaultValue={query.data.edadMinMachoMeses ?? ''} /></Field>
      <Field label="Hembras: edad mínima (meses)" required><input name="hembra" type="number" min="1" max="120" step="1" required disabled={!editable} defaultValue={query.data.edadMinHembraMeses ?? ''} /></Field>
      <Field label="Proyección del calendario (meses)" hint="Genera con anticipación las actividades que luego podrán sincronizarse con un calendario externo." required>
        <input name="horizonteProyeccionMeses" type="number" min="1" max="24" step="1" required disabled={!editable} defaultValue={query.data.horizonteProyeccionMeses ?? 12} />
      </Field>
      {editable && <div className="form-actions"><Button type="submit" loading={guardar.isPending}>Guardar edades mínimas</Button></div>}
    </form>}
    {guardar.error && <Alert tone="danger">{normalizeApiError(guardar.error).message}</Alert>}
    {guardar.isSuccess && <p role="status">Edades mínimas guardadas.</p>}
  </Card>
}
