import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Baby, Bug, Stethoscope } from 'lucide-react'
import { getAnimal } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { AnimalSearchSelect } from '@/features/reproduccion/components/AnimalSearchSelect'
import {
  ESTADO_CALOSTRADO_LABELS,
  listControlesEctoparasitarios,
  listControlesNeonatales,
  listExamenesReproductivos,
  MOMENTO_CONTROL_NEONATAL_LABELS,
  NIVEL_CARGA_PARASITARIA_LABELS,
  RESULTADO_EXAMEN_REPRODUCTIVO_LABELS,
  TIPO_ECTOPARASITO_LABELS,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { ControlEctoparasitarioModal } from '@/features/sanidad/components/ControlEctoparasitarioModal'
import { ControlNeonatalModal } from '@/features/sanidad/components/ControlNeonatalModal'
import { ExamenReproductivoModal } from '@/features/sanidad/components/ExamenReproductivoModal'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { normalizeApiError } from '@/shared/api/errors'

type ModalActivo = 'neonatal' | 'ecto' | 'reproductivo' | null

export function ControlesPanel({ catalogs, initialAnimalId }: { catalogs: SanidadCatalogs; initialAnimalId?: string }) {
  const [animalId, setAnimalId] = useState(initialAnimalId ?? '')
  const [selectedAnimal, setSelectedAnimal] = useState<AnimalSummary>()
  const [modal, setModal] = useState<ModalActivo>(null)
  const enabled = Boolean(animalId)
  const loadedAnimal = useQuery({ queryKey: ['animal', animalId], queryFn: () => getAnimal(animalId), enabled: enabled && !catalogs.animals.some((item) => item.id === animalId) })
  const animal = selectedAnimal?.id === animalId ? selectedAnimal : catalogs.animals.find((item) => item.id === animalId) ?? loadedAnimal.data
  const neonatales = useQuery({ queryKey: ['sanidad-control-neonatal', animalId], queryFn: () => listControlesNeonatales(animalId), enabled })
  const ectoparasitarios = useQuery({ queryKey: ['sanidad-control-ecto', animalId], queryFn: () => listControlesEctoparasitarios({ animalId }), enabled })
  const reproductivos = useQuery({ queryKey: ['sanidad-examen-reproductivo', animalId], queryFn: () => listExamenesReproductivos(animalId), enabled })
  const error = loadedAnimal.error ?? neonatales.error ?? ectoparasitarios.error ?? reproductivos.error
  const loading = enabled && (neonatales.isPending || ectoparasitarios.isPending || reproductivos.isPending)
  const puedeNeonatal = animal ? esNeonatoElegible(animal) : false
  const puedeReproductivo = Boolean(animal?.fechaNacimiento && animal.estado === 'ACTIVO')
  const cerrar = () => setModal(null)

  return <div className="page-stack">
    <Card>
      <div className="section-heading"><div><span className="eyebrow">Animal</span><h2>Controles individuales</h2></div></div>
      <p className="muted">Busca un animal para consultar su historial y registrar únicamente controles que le correspondan.</p>
      <div style={{ maxWidth: 520 }}><AnimalSearchSelect label="Animal" name="animalId" value={animalId} onChange={(id, selected) => { setAnimalId(id); setSelectedAnimal(selected) }} /></div>
    </Card>
    {!animal && <EmptyState title="Selecciona un animal" description="Aquí se mostrarán sus controles neonatales, ectoparasitarios y exámenes reproductivos." />}
    {animal && <>
      {error && <Alert tone="danger">{normalizeApiError(error).message}</Alert>}
      <Card>
        <div className="section-heading"><div><h3><Baby size={19} aria-hidden="true" /> Control neonatal</h3><p className="muted">Solo durante el nacimiento o la primera semana de vida.</p></div><Button variant="secondary" disabled={!puedeNeonatal} onClick={() => setModal('neonatal')}>Registrar control neonatal</Button></div>
        {!puedeNeonatal && <Alert tone="info">No disponible: requiere animal activo, nacimiento confirmado y una edad máxima de 7 días.</Alert>}
        {loading ? <LoadingState message="Cargando controles…" /> : neonatales.data?.length
          ? <ul className="attention-list">{neonatales.data.map((control) => <li key={control.id}><div><strong>{new Date(control.fechaControl).toLocaleDateString('es-BO')} · {MOMENTO_CONTROL_NEONATAL_LABELS[control.momento]}</strong><span>Calostrado: {ESTADO_CALOSTRADO_LABELS[control.calostrado]}{control.diarrea ? ' · Con diarrea' : ''}</span></div></li>)}</ul>
          : <p className="muted">Sin controles neonatales registrados.</p>}
      </Card>
      <Card>
        <div className="section-heading"><div><h3><Bug size={19} aria-hidden="true" /> Control ectoparasitario</h3><p className="muted">Evaluación de carga parasitaria y tratamiento aplicado.</p></div><Button variant="secondary" disabled={animal.estado !== 'ACTIVO'} onClick={() => setModal('ecto')}>Registrar control ectoparasitario</Button></div>
        {ectoparasitarios.data?.length ? <ul className="attention-list">{ectoparasitarios.data.map((control) => <li key={control.id}><div><strong>{new Date(control.fecha).toLocaleDateString('es-BO')} · {TIPO_ECTOPARASITO_LABELS[control.tipo]}</strong><span>Carga {NIVEL_CARGA_PARASITARIA_LABELS[control.nivelCarga]} · {control.tratado ? 'Tratado' : 'Sin tratamiento'}</span></div></li>)}</ul> : !loading && <p className="muted">Sin controles ectoparasitarios registrados.</p>}
      </Card>
      <Card>
        <div className="section-heading"><div><h3><Stethoscope size={19} aria-hidden="true" /> Examen reproductivo</h3><p className="muted">La edad mínima configurada se valida al guardar.</p></div><Button variant="secondary" disabled={!puedeReproductivo} onClick={() => setModal('reproductivo')}>Registrar examen reproductivo</Button></div>
        {!animal.fechaNacimiento && <Alert tone="info">No disponible: primero registra o estima la fecha de nacimiento para comprobar la edad.</Alert>}
        {reproductivos.data?.length ? <ul className="attention-list">{reproductivos.data.map((examen) => <li key={examen.id}><div><strong>{new Date(examen.fecha).toLocaleDateString('es-BO')} · {RESULTADO_EXAMEN_REPRODUCTIVO_LABELS[examen.resultado]}</strong><span>{examen.observaciones || 'Sin observaciones.'}</span></div></li>)}</ul> : !loading && <p className="muted">Sin exámenes reproductivos registrados.</p>}
      </Card>
      {modal === 'neonatal' && <ControlNeonatalModal animalId={animal.id} animalCodigo={catalogs.animalLabel(animal.id)} onClose={cerrar} onSaved={cerrar} />}
      {modal === 'ecto' && <ControlEctoparasitarioModal animalId={animal.id} destinoLabel={catalogs.animalLabel(animal.id)} onClose={cerrar} onSaved={cerrar} />}
      {modal === 'reproductivo' && <ExamenReproductivoModal animal={animal} onClose={cerrar} onSaved={cerrar} />}
    </>}
  </div>
}

function esNeonatoElegible(animal: AnimalSummary) {
  if (animal.estado !== 'ACTIVO' || !animal.fechaNacimiento || animal.fechaNacimientoEstimada) return false
  const nacimiento = new Date(`${animal.fechaNacimiento}T00:00:00`)
  const edadDias = Math.floor((Date.now() - nacimiento.getTime()) / 86_400_000)
  return edadDias >= 0 && edadDias <= 7
}
