import { useState, type FormEvent, type MouseEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Gauge, Save } from 'lucide-react'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { Modal } from '@/shared/components/Modal'
import { crearCategoriaEdad, actualizarCategoriaEdad, simularImpactoCategoriaEdad, type RangoCategoriaInput } from '@/features/configuracion/categoriasEdadApi'
import type { CategoriaAnimal } from '@/features/animales/types'

interface RangoCategoriaModalProps {
  open: boolean
  categoria: CategoriaAnimal | null
  onClose: () => void
  onSaved: () => void
}

export function RangoCategoriaModal({ open, categoria, onClose, onSaved }: RangoCategoriaModalProps) {
  const editando = Boolean(categoria)
  const [confirmarHueco, setConfirmarHueco] = useState(false)
  const [huecoPendiente, setHuecoPendiente] = useState<RangoCategoriaInput | null>(null)
  const [simulacion, setSimulacion] = useState<number | null>(null)

  const simular = useMutation({ mutationFn: simularImpactoCategoriaEdad, onSuccess: (data) => setSimulacion(data.animalesAfectados) })
  const guardar = useMutation({
    mutationFn: (input: RangoCategoriaInput) => categoria ? actualizarCategoriaEdad(categoria.id, input) : crearCategoriaEdad(input),
    onSuccess: () => { onSaved(); cerrar() },
    onError: (error, input) => {
      if (normalizeApiError(error).code === 'ANIMAL_CATEGORY_RANGE_GAP') setHuecoPendiente(input)
    },
  })

  function cerrar() {
    setConfirmarHueco(false)
    setHuecoPendiente(null)
    setSimulacion(null)
    onClose()
  }

  function leerFormulario(form: HTMLFormElement): RangoCategoriaInput {
    const data = new FormData(form)
    return {
      codigo: String(data.get('codigo') ?? categoria?.codigo ?? '').toUpperCase(),
      nombre: String(data.get('nombre') ?? ''),
      sexoAplicable: data.get('sexoAplicable') as RangoCategoriaInput['sexoAplicable'],
      edadMinMeses: data.get('edadMinMeses') ? Number(data.get('edadMinMeses')) : undefined,
      edadMaxMeses: data.get('edadMaxMeses') ? Number(data.get('edadMaxMeses')) : undefined,
      descripcion: String(data.get('descripcion') ?? '') || undefined,
      clasificacionAutomatica: data.get('clasificacionAutomatica') === 'on',
      ordenEvaluacion: Number(data.get('ordenEvaluacion') ?? 0),
      confirmarHueco,
      id: categoria?.id,
    }
  }

  function simularAhora(event: MouseEvent<HTMLButtonElement>) {
    setSimulacion(null)
    const form = event.currentTarget.form
    if (form) simular.mutate(leerFormulario(form))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (guardar.isPending) return
    const input = leerFormulario(event.currentTarget)
    guardar.mutate(input)
  }

  const error = guardar.error && !huecoPendiente ? guardar.error : null

  return <Modal open={open} title={editando ? `Editar ${categoria?.nombre}` : 'Nueva categoría por edad'} description="Define el rango de edad y sexo que activa esta categoría automáticamente." onClose={cerrar}>
    <form className="page-stack" onSubmit={submit}>
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      {huecoPendiente && <Alert tone="warning">
        Este rango deja un hueco sin cubrir entre categorías automáticas. ¿Confirmas que es intencional?
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={() => setHuecoPendiente(null)}>Revisar rango</Button>
          <Button type="button" loading={guardar.isPending} onClick={() => { setConfirmarHueco(true); guardar.mutate({ ...huecoPendiente, confirmarHueco: true }) }}>Confirmar hueco intencional</Button>
        </div>
      </Alert>}
      <div className="form-grid">
        <Field label="Código" required hint="Identificador corto, en mayúsculas."><input name="codigo" required maxLength={30} defaultValue={categoria?.codigo} disabled={editando} style={{ textTransform: 'uppercase' }} /></Field>
        <Field label="Nombre" required><input name="nombre" required maxLength={80} defaultValue={categoria?.nombre} /></Field>
        <Field label="Sexo aplicable" required><select name="sexoAplicable" required defaultValue={categoria?.sexoAplicable ?? 'AMBOS'}><option value="MACHO">Macho</option><option value="HEMBRA">Hembra</option><option value="AMBOS">Ambos</option></select></Field>
        <Field label="Edad mínima (meses)"><input name="edadMinMeses" type="number" min="0" step="1" defaultValue={categoria?.edadMinMeses ?? 0} /></Field>
        <Field label="Edad máxima (meses)" hint="Vacío = sin límite (categoría abierta)."><input name="edadMaxMeses" type="number" min="0" step="1" defaultValue={categoria?.edadMaxMeses} /></Field>
        <Field label="Orden de evaluación" hint="Menor primero cuando haya empates."><input name="ordenEvaluacion" type="number" min="0" step="1" defaultValue={categoria?.ordenEvaluacion ?? 0} /></Field>
        <div className="form-full"><Field label="Descripción"><textarea name="descripcion" rows={2} defaultValue={categoria?.descripcion} /></Field></div>
        <label className="checkbox-line form-full"><input name="clasificacionAutomatica" type="checkbox" defaultChecked={categoria?.clasificacionAutomatica ?? true} /> Calcular automáticamente por edad (desactiva para excepciones manuales, como Buey)</label>
      </div>
      {simulacion != null && <Alert tone="info">{simulacion} animal(es) cambiarían de categoría si aplicas este rango.</Alert>}
      <div className="form-actions">
        <Button type="button" variant="ghost" onClick={cerrar}>Cancelar</Button>
        <Button type="button" variant="secondary" loading={simular.isPending} onClick={simularAhora}><Gauge size={16} aria-hidden="true" />Simular impacto</Button>
        <Button type="submit" loading={guardar.isPending}><Save size={17} aria-hidden="true" />{editando ? 'Guardar cambios' : 'Crear categoría'}</Button>
      </div>
    </form>
  </Modal>
}
