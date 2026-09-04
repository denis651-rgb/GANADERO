import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AnimalSummary } from '@/features/animales/types'
import {
  crearExamenReproductivo,
  getConfiguracionSanitaria,
  ENFERMEDAD_REPRODUCTIVA_LABELS,
  listExamenesReproductivos,
  RESULTADO_EXAMEN_REPRODUCTIVO_LABELS,
  RESULTADO_PRUEBA_REPRODUCTIVA_LABELS,
  type CrearExamenReproductivoInput,
  type EnfermedadReproductiva,
  type PruebaReproductiva,
  type ResultadoExamenReproductivo,
  type ResultadoPruebaReproductiva,
} from '@/features/sanidad/api'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface ExamenReproductivoModalProps {
  animal: AnimalSummary
  onClose: () => void
  onSaved: () => void
}

const ENFERMEDADES = Object.keys(ENFERMEDAD_REPRODUCTIVA_LABELS) as EnfermedadReproductiva[]

export function ExamenReproductivoModal({ animal, onClose, onSaved }: ExamenReproductivoModalProps) {
  const client = useQueryClient()
  const config = useQuery({ queryKey: ['sanidad-configuracion'], queryFn: getConfiguracionSanitaria })
  const edadMinima = animal.sexo === 'MACHO' ? config.data?.edadMinMachoMeses : config.data?.edadMinHembraMeses
  const historial = useQuery({
    queryKey: ['sanidad-examen-reproductivo', animal.id],
    queryFn: () => listExamenesReproductivos(animal.id),
  })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const num = (name: string) => { const v = data.get(name); return v ? Number(v) : undefined }
      const str = (name: string) => String(data.get(name) || '') || undefined
      const pruebas: PruebaReproductiva[] = ENFERMEDADES.map((enfermedad) => ({
        enfermedad,
        resultado: String(data.get(`prueba_${enfermedad}`)) as ResultadoPruebaReproductiva,
      }))
      const input: CrearExamenReproductivoInput = {
        animalId: animal.id,
        fecha: String(data.get('fecha')),
        resultado: String(data.get('resultado')) as ResultadoExamenReproductivo,
        observaciones: str('observaciones'),
        pruebas,
        ...(animal.sexo === 'MACHO' ? {
          circunferenciaEscrotalCm: num('circunferenciaEscrotalCm'),
          motilidadEspermaticaPct: num('motilidadEspermaticaPct'),
          morfologiaPct: num('morfologiaPct'),
          libido: str('libido'),
          capacidadServicio: str('capacidadServicio'),
        } : {
          pesoKg: num('pesoKg'),
          porcentajePesoAdulto: num('porcentajePesoAdulto'),
          condicionCorporal: num('condicionCorporal'),
          desarrolloReproductivo: str('desarrolloReproductivo'),
        }),
      }
      return crearExamenReproductivo(input)
    },
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['sanidad-examen-reproductivo', animal.id] })
      onSaved()
    },
  })

  return <Modal open title="Registrar examen reproductivo" onClose={onClose} wide description={`Examen reproductivo de ${animal.codigo}.`}>
    <div className="page-stack">
      <p className="muted">{edadMinima ? `Edad mínima configurada: ${edadMinima} meses. Se comprueba en la fecha del examen.` : 'Configure las edades mínimas en Sanidad → Planes sanitarios antes de guardar.'}</p>
      <p className="muted">La edad no acredita aptitud. Para APTO complete la evaluación física y reproductiva y documente la conclusión en observaciones. Una fecha de nacimiento estimada no acredita por sí sola la madurez.</p>
      {config.error && <Alert tone="danger">{normalizeApiError(config.error).message}<button type="button" onClick={() => void config.refetch()}>Reintentar</button></Alert>}
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Fecha" required><input name="fecha" type="date" required /></Field>
        <Field label="Resultado general" required><select name="resultado" required defaultValue="OBSERVACION">{(Object.keys(RESULTADO_EXAMEN_REPRODUCTIVO_LABELS) as ResultadoExamenReproductivo[]).map((resultado) => <option key={resultado} value={resultado}>{RESULTADO_EXAMEN_REPRODUCTIVO_LABELS[resultado]}</option>)}</select></Field>

        {animal.sexo === 'MACHO' && <>
          <Field label="Circunferencia escrotal (cm)"><input name="circunferenciaEscrotalCm" type="number" inputMode="decimal" step="0.1" /></Field>
          <Field label="Motilidad espermática (%)"><input name="motilidadEspermaticaPct" type="number" inputMode="decimal" step="0.1" min="0" max="100" /></Field>
          <Field label="Morfología (%)"><input name="morfologiaPct" type="number" inputMode="decimal" step="0.1" min="0" max="100" /></Field>
          <Field label="Libido"><input name="libido" maxLength={200} /></Field>
          <Field label="Capacidad de servicio"><input name="capacidadServicio" maxLength={200} /></Field>
        </>}

        {animal.sexo === 'HEMBRA' && <>
          <Field label="Peso (kg)"><input name="pesoKg" type="number" inputMode="decimal" step="0.1" /></Field>
          <Field label="Porcentaje de peso adulto (%)"><input name="porcentajePesoAdulto" type="number" inputMode="decimal" step="0.1" min="0" max="100" /></Field>
          <Field label="Condición corporal"><input name="condicionCorporal" type="number" inputMode="decimal" step="0.1" min="1" max="5" /></Field>
          <Field label="Desarrollo reproductivo"><input name="desarrolloReproductivo" maxLength={200} /></Field>
        </>}

        <div className="form-full"><h3>Checklist de enfermedades</h3></div>
        {ENFERMEDADES.map((enfermedad) => <Field key={enfermedad} label={ENFERMEDAD_REPRODUCTIVA_LABELS[enfermedad]}>
          <select name={`prueba_${enfermedad}`} defaultValue="NO_REALIZADO">
            {(Object.keys(RESULTADO_PRUEBA_REPRODUCTIVA_LABELS) as ResultadoPruebaReproductiva[]).map((resultado) => <option key={resultado} value={resultado}>{RESULTADO_PRUEBA_REPRODUCTIVA_LABELS[resultado]}</option>)}
          </select>
        </Field>)}

        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" disabled={!edadMinima || Boolean(config.error)} loading={crear.isPending}>Guardar examen</Button></div>
      </form>
      {crear.error && <Alert tone="danger">{normalizeApiError(crear.error).message}</Alert>}

      <h3>Exámenes previos</h3>
      {historial.isPending && <LoadingState message="Cargando exámenes…" />}
      {historial.data?.length === 0 && <p className="muted">Todavía no hay exámenes reproductivos registrados para este animal.</p>}
      {historial.data && historial.data.length > 0 && <ul className="attention-list">{historial.data.map((examen) => <li key={examen.id} className={examen.resultado === 'NO_APTO' ? 'attention-danger' : examen.resultado === 'OBSERVACION' ? 'attention-warning' : undefined}>
        <div><strong>{new Date(examen.fecha).toLocaleDateString('es-BO')} · {RESULTADO_EXAMEN_REPRODUCTIVO_LABELS[examen.resultado]}</strong>
        <span>{examen.pruebas.map((prueba) => `${ENFERMEDAD_REPRODUCTIVA_LABELS[prueba.enfermedad]}: ${RESULTADO_PRUEBA_REPRODUCTIVA_LABELS[prueba.resultado]}`).join(' · ')}</span></div>
      </li>)}</ul>}
    </div>
  </Modal>
}
