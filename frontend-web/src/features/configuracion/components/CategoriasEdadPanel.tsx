import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Power, RefreshCw, Pencil } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import { listCategoriasEdad, cambiarEstadoCategoriaEdad, reclasificarCategoriasEdad, type ResultadoReclasificacion } from '@/features/configuracion/categoriasEdadApi'
import { RangoCategoriaModal } from '@/features/configuracion/components/RangoCategoriaModal'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'
import type { CategoriaAnimal } from '@/features/animales/types'

const GRUPOS: Array<{ sexo: 'MACHO' | 'HEMBRA' | 'AMBOS'; titulo: string }> = [
  { sexo: 'MACHO', titulo: 'Machos' },
  { sexo: 'HEMBRA', titulo: 'Hembras' },
  { sexo: 'AMBOS', titulo: 'Ambos sexos' },
]

function rangoTexto(categoria: CategoriaAnimal) {
  if (categoria.edadMinMeses == null && categoria.edadMaxMeses == null) return 'Cualquier edad'
  if (categoria.edadMaxMeses == null) return `${categoria.edadMinMeses ?? 0}+ meses`
  return `${categoria.edadMinMeses ?? 0}–${categoria.edadMaxMeses} meses`
}

export function CategoriasEdadPanel() {
  const { can } = useAuth()
  const client = useQueryClient()
  const editable = can('CONFIGURACION_EDITAR')
  const [modalCategoria, setModalCategoria] = useState<CategoriaAnimal | 'nueva' | null>(null)
  const [confirmarReclasificar, setConfirmarReclasificar] = useState(false)
  const [resultado, setResultado] = useState<ResultadoReclasificacion | null>(null)

  const query = useQuery({ queryKey: ['categorias-edad'], queryFn: listCategoriasEdad, enabled: editable })
  const toggle = useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) => cambiarEstadoCategoriaEdad(id, activo),
    onSuccess: () => client.invalidateQueries({ queryKey: ['categorias-edad'] }),
  })
  const reclasificar = useMutation({
    mutationFn: reclasificarCategoriasEdad,
    onSuccess: (data) => { setResultado(data); setConfirmarReclasificar(false); client.invalidateQueries({ queryKey: ['categorias-edad'] }) },
  })

  if (!editable) return null

  return <Card>
    <div className="section-heading">
      <div><span className="eyebrow">Mi finca</span><h3>Categorías por edad</h3></div>
      <div className="inline-actions">
        <Button variant="secondary" onClick={() => setConfirmarReclasificar(true)}><RefreshCw size={16} aria-hidden="true" />Aplicar reclasificación ahora</Button>
        <Button onClick={() => setModalCategoria('nueva')}><Plus size={16} aria-hidden="true" />Nueva categoría</Button>
      </div>
    </div>
    <p className="muted">Estos rangos determinan la categoría automática de cada animal según su sexo y edad. Buey y otras excepciones manuales no participan del cálculo automático.</p>
    {query.isPending && <LoadingState message="Cargando categorías…" />}
    {query.error && <Alert tone="danger">{normalizeApiError(query.error).message}</Alert>}
    {resultado && <Alert tone="success">
      Reclasificación aplicada: {resultado.procesados} procesados, {resultado.actualizados} actualizados, {resultado.omitidos} omitidos, {resultado.errores} con error.
    </Alert>}
    {query.data && GRUPOS.map(({ sexo, titulo }) => {
      const categorias = query.data.filter((item) => item.sexoAplicable === sexo).sort((a, b) => a.ordenEvaluacion - b.ordenEvaluacion || (a.edadMinMeses ?? 0) - (b.edadMinMeses ?? 0))
      if (categorias.length === 0) return null
      return <div key={sexo} className="page-stack" style={{ marginTop: 14 }}>
        <h4>{titulo}</h4>
        <div className="table-wrapper"><table>
          <caption className="visually-hidden">Categorías por edad: {titulo}</caption>
          <thead><tr><th scope="col">Categoría</th><th scope="col">Rango</th><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>{categorias.map((categoria) => <tr key={categoria.id}>
            <td><strong>{categoria.nombre}</strong><span className="table-secondary">{categoria.codigo}</span></td>
            <td>{rangoTexto(categoria)}</td>
            <td>{categoria.clasificacionAutomatica ? 'Automática' : 'Manual (excepción)'}</td>
            <td><span className={`status-badge ${categoria.activo ? 'status-activo' : 'status-inactivo'}`}>{categoria.activo ? 'ACTIVA' : 'INACTIVA'}</span></td>
            <td><div className="inline-actions">
              <Button variant="ghost" onClick={() => setModalCategoria(categoria)}><Pencil size={16} aria-hidden="true" />Editar</Button>
              <Button variant="ghost" loading={toggle.isPending} onClick={() => toggle.mutate({ id: categoria.id, activo: !categoria.activo })}><Power size={16} aria-hidden="true" />{categoria.activo ? 'Desactivar' : 'Activar'}</Button>
            </div></td>
          </tr>)}</tbody>
        </table></div>
      </div>
    })}

    {modalCategoria && <RangoCategoriaModal
      open
      categoria={modalCategoria === 'nueva' ? null : modalCategoria}
      onClose={() => setModalCategoria(null)}
      onSaved={() => client.invalidateQueries({ queryKey: ['categorias-edad'] })}
    />}

    <ConfirmDialog
      open={confirmarReclasificar}
      title="Aplicar reclasificación por edad"
      confirmLabel="Aplicar ahora"
      variant="warning"
      loading={reclasificar.isPending}
      error={reclasificar.error}
      onClose={() => setConfirmarReclasificar(false)}
      onConfirm={() => reclasificar.mutate()}
    >
      Se revisarán todos los animales activos con fecha de nacimiento conocida o estimada y se actualizará su categoría si corresponde. Las excepciones manuales (como Buey) no se modifican. Queda registro en el historial de cada animal.
    </ConfirmDialog>
  </Card>
}
