import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { abrirGestacion, listDiagnosticos, listGestaciones } from '../api'
import { Field } from '@/shared/components/Field'
import { Button } from '@/shared/components/Button'
import { Alert } from '@/shared/components/Alert'
import { normalizeApiError } from '@/shared/api/errors'
import { formatDate, todayInBolivia } from '@/shared/utils/date'

const estados = { ABIERTA: 'Abierta', FINALIZADA_PARTO: 'Finalizada por parto', FINALIZADA_ABORTO: 'Finalizada por aborto' }

// The parent keys this component by animal so neither selection nor draft crosses animals.
export function GestacionSelect({ animalId }: { animalId: string }) {
  const [modo, setModo] = useState('')
  const [diagnosticoId, setDiagnosticoId] = useState('')
  const [fecha, setFecha] = useState(todayInBolivia)
  const [inicio, setInicio] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const gestaciones = useQuery({ queryKey: ['gestaciones', animalId], queryFn: () => listGestaciones(animalId), enabled: !!animalId, staleTime: 0 })
  const diagnosticos = useQuery({ queryKey: ['diagnosticos-para-gestacion', animalId], queryFn: () => listDiagnosticos({ animalId, size: 500 }), enabled: !!animalId && modo === 'diagnostico' })
  const crear = useMutation({
    mutationFn: () => abrirGestacion(modo === 'diagnostico' ? { animalId, diagnosticoId } : { animalId, fechaConfirmacion: fecha, fechaInicioEstimada: inicio || undefined, observaciones }),
    onSuccess: async () => { await gestaciones.refetch(); setModo('') },
  })
  const abierta = gestaciones.data?.find((g) => g.estado === 'ABIERTA')
  const disponibles = diagnosticos.data?.content.filter((d) => d.resultado === 'POSITIVO' && d.estado === 'ACTIVO' && !gestaciones.data?.some((g) => g.diagnosticoId === d.id)) ?? []
  return <div className="form-full page-stack">
    <Field label="Gestación que finaliza" required hint="Solo puede finalizar una vez: por parto o por aborto.">
      <select name="cicloGestacionId" required key={abierta?.id ?? 'vacia'} defaultValue="">
        <option value="">{gestaciones.isFetching ? 'Consultando gestaciones…' : !animalId ? 'Selecciona primero la hembra' : 'Selecciona la gestación…'}</option>
        {abierta && !gestaciones.isError && <option value={abierta.id}>Confirmada el {formatDate(abierta.fechaConfirmacion)} · {abierta.antecedentesDesconocidos ? 'Antecedentes desconocidos' : 'Servicio registrado'}</option>}
      </select>
    </Field>
    {gestaciones.isError && <p role="alert">No se pudieron cargar las gestaciones. <Button type="button" variant="ghost" onClick={() => void gestaciones.refetch()}>Reintentar</Button></p>}
    {!!animalId && !gestaciones.isPending && !gestaciones.isError && !abierta && <>
      <p className="muted">No hay gestación abierta. Los eventos antiguos no se vinculan automáticamente. Puedes identificar un diagnóstico previo o registrar antecedentes desconocidos. Registrar esta gestación la guarda aunque luego canceles el parto o aborto.</p>
      <Field label="Identificar gestación"><select value={modo} onChange={(e) => { setModo(e.target.value); crear.reset() }}><option value="">Selecciona una opción…</option><option value="diagnostico">Usar diagnóstico positivo previo</option><option value="desconocidos">Registrar gestación con antecedentes desconocidos</option></select></Field>
      {modo === 'diagnostico' && <Field label="Diagnóstico positivo"><select value={diagnosticoId} onChange={(e) => setDiagnosticoId(e.target.value)}><option value="">Selecciona el diagnóstico…</option>{disponibles.map((d) => <option key={d.id} value={d.id}>{new Date(d.fechaDiagnostico).toLocaleString('es-BO', { timeZone: 'America/La_Paz' })}</option>)}</select></Field>}
      {modo === 'diagnostico' && diagnosticos.isError && <p role="alert">No se pudieron cargar los diagnósticos.</p>}
      {modo === 'desconocidos' && <div className="form-grid">
        <Field label="Fecha de confirmación"><input type="date" max={todayInBolivia()} value={fecha} onChange={(e) => setFecha(e.target.value)} /></Field>
        <Field label="Inicio estimado (opcional)" hint="Déjalo vacío si no se conoce."><input type="date" max={fecha} value={inicio} onChange={(e) => setInicio(e.target.value)} /></Field>
        <div className="form-full"><Field label="Antecedentes y evidencia de confirmación"><textarea maxLength={1000} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Ej.: comprada gestante; servicio y padre desconocidos. Indica cómo se confirmó." /></Field></div>
      </div>}
      {modo && <Button type="button" variant="secondary" loading={crear.isPending} disabled={modo === 'diagnostico' ? !diagnosticoId || diagnosticos.isError : !fecha || !observaciones.trim()} onClick={() => crear.mutate()}>Guardar gestación</Button>}
    </>}
    {crear.isError && <Alert tone="danger">{normalizeApiError(crear.error).message}</Alert>}
    {!!gestaciones.data?.length && <details><summary>Historial de gestaciones ({gestaciones.data.length})</summary>{gestaciones.data.map((g) => <p key={g.id}>{formatDate(g.fechaConfirmacion)} · {estados[g.estado]}{g.fechaCierre ? ` · ${formatDate(g.fechaCierre)}` : ''}</p>)}</details>}
  </div>
}
