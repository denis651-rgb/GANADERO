import { useState } from 'react'
import { AnimalSearchSelect } from './AnimalSearchSelect'
import { useMutation, useQuery } from '@tanstack/react-query'
import { listRazas } from '@/features/animales/api'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  DIFICULTAD_PARTO_LABELS,
  estadoRegistroBadge,
  ESTADO_NACIMIENTO_LABELS,
  ESTADO_REGISTRO_LABELS,
  registrarParto,
  TIPO_PARTO_LABELS,
  type EstadoNacimiento,
  type PageResponse,
  type Parto,
} from '@/features/reproduccion/api'
import type { ReproduccionCatalogs } from '@/features/reproduccion/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'
import { formatDate } from '@/shared/utils/date'
import './PartosPanel.css'
import { GestacionSelect } from './GestacionSelect'

interface PartosPanelProps {
  partos: PageResponse<Parto>
  isLoading: boolean
  error: unknown
  catalogs?: ReproduccionCatalogs
  refresh: () => void
}

interface CriaRow {
  key: number
  sexo: string
  pesoNacimientoKg: string
  estadoNacimiento: string
  horaNacimiento: string
  observaciones: string
  crearAnimal: boolean
  nombreAnimal: string
  razaPrincipalId: string
  potreroInicialId: string
}

let criaKey = 0

function nuevaCria(): CriaRow {
  return { key: criaKey++, sexo: 'HEMBRA', pesoNacimientoKg: '', estadoNacimiento: 'VIVO', horaNacimiento: '', observaciones: '', crearAnimal: true, nombreAnimal: '', razaPrincipalId: '', potreroInicialId: '' }
}

export function PartosPanel({ partos, isLoading, error, catalogs, refresh }: PartosPanelProps) {
  const { can } = useAuth()
  const canRegistrar = can('REPRODUCCION_REGISTRAR')
  const [showForm, setShowForm] = useState(false)
  const [madreId, setMadreId] = useState('')
  const [crias, setCrias] = useState<CriaRow[]>(() => [nuevaCria()])
  const razas = useQuery({ queryKey: ['razas'], queryFn: listRazas, enabled: showForm, staleTime: 300_000 })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return registrarParto({
        cicloGestacionId: String(data.get('cicloGestacionId') || ''),
        madreId: String(data.get('madreId')),
        fechaParto: String(data.get('fechaParto')),
        tipoParto: String(data.get('tipoParto')) as Parto['tipoParto'],
        dificultad: String(data.get('dificultad')) as Parto['dificultad'],
        asistido: data.get('asistido') === 'on',
        responsableId: String(data.get('responsableId') || '') || undefined,
        resultadoMadre: String(data.get('resultadoMadre') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
        crias: crias.map((fila) => ({
          sexo: fila.sexo as 'MACHO' | 'HEMBRA',
          pesoNacimientoKg: Number(fila.pesoNacimientoKg),
          estadoNacimiento: fila.estadoNacimiento as EstadoNacimiento,
          horaNacimiento: fila.horaNacimiento || undefined,
          observaciones: fila.observaciones || undefined,
          crearAnimal: fila.crearAnimal,
          nombreAnimal: fila.crearAnimal ? fila.nombreAnimal || undefined : undefined,
          razaPrincipalId: fila.crearAnimal ? fila.razaPrincipalId || undefined : undefined,
          potreroInicialId: fila.crearAnimal ? fila.potreroInicialId || undefined : undefined,
        })),
      })
    },
    onSuccess: () => { setShowForm(false); setCrias([nuevaCria()]); refresh() },
  })

  const actualizarCria = (key: number, campo: keyof CriaRow, valor: string | boolean) => {
    setCrias((prev) => prev.map((fila) => (fila.key === key ? { ...fila, [campo]: valor } : fila)))
  }

  const errorVisible = error ?? crear.error

  return <div className="page-stack partos-panel">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="partos-heading">
        <h2>Partos y crías</h2>
        {canRegistrar && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Registrar parto</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando partos…" />}
      {!isLoading && partos.content.length === 0 && <EmptyState title="No hay partos registrados" description="Registra el primer parto para llevar el control de nacimientos." />}
      {partos.content.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Partos</caption><thead><tr><th scope="col">Madre</th><th scope="col">Fecha</th><th scope="col">Tipo</th><th scope="col">Dificultad</th><th scope="col">Crías</th><th scope="col">Estado</th></tr></thead><tbody>{partos.content.map((parto) => <tr key={parto.id}>
        <td><strong className="parto-madre">{parto.nombreMadre || parto.codigoMadre || 'Sin nombre'}</strong>{parto.nombreMadre && parto.codigoMadre && <span className="table-secondary">{parto.codigoMadre}</span>}</td>
        <td>{formatDate(parto.fechaParto)}</td>
        <td>{TIPO_PARTO_LABELS[parto.tipoParto]}</td>
        <td>{DIFICULTAD_PARTO_LABELS[parto.dificultad]}</td>
        <td>{parto.numeroCrias}</td>
        <td><span className={`status-badge status-badge-${estadoRegistroBadge(parto.estado)}`}>{ESTADO_REGISTRO_LABELS[parto.estado]}</span></td>
      </tr>)}</tbody></table></div>}
      {partos.content.length > 0 && <div className="mobile-only">{partos.content.map((parto) => <div key={parto.id} className="mobile-entity-card">
        <div><strong className="parto-madre">{parto.nombreMadre || parto.codigoMadre || 'Sin nombre'}</strong>{parto.nombreMadre && parto.codigoMadre && <span className="table-secondary">{parto.codigoMadre}</span>}<p className="muted">{formatDate(parto.fechaParto)} · {TIPO_PARTO_LABELS[parto.tipoParto]}</p><p className="muted">{DIFICULTAD_PARTO_LABELS[parto.dificultad]} · {parto.numeroCrias} cría(s)</p><span className={`status-badge status-badge-${estadoRegistroBadge(parto.estado)}`}>{ESTADO_REGISTRO_LABELS[parto.estado]}</span></div>
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Registrar parto" onClose={() => setShowForm(false)} wide description="Registra el parto y los datos de cada cría.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <AnimalSearchSelect label="Madre" name="madreId" sexo="HEMBRA" value={madreId} onChange={setMadreId} />
        <GestacionSelect key={madreId} animalId={madreId} />
        <Field label="Fecha del parto" required><input name="fechaParto" type="date" required /></Field>
        <Field label="Tipo de parto" required><select name="tipoParto" required><option value="" disabled>Selecciona…</option>{Object.entries(TIPO_PARTO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <Field label="Dificultad" required><select name="dificultad" required><option value="" disabled>Selecciona…</option>{Object.entries(DIFICULTAD_PARTO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
        <label className="checkbox-line"><input type="checkbox" name="asistido" />Parto asistido</label>
        <Field label="Resultado de la madre"><input name="resultadoMadre" maxLength={30} placeholder="Ej. Buena, en observación…" /></Field>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={2} maxLength={1000} /></Field></div>
        <div className="form-full"><div className="form-section-title">Crías</div></div>
        {crias.map((fila) => <div key={fila.key} className="form-grid form-full">
          <Field label="Sexo" required><select value={fila.sexo} onChange={(event) => actualizarCria(fila.key, 'sexo', event.target.value)}><option value="MACHO">Macho</option><option value="HEMBRA">Hembra</option></select></Field>
          <Field label="Peso al nacer (kg)" required><input type="number" inputMode="decimal" min="0" step="0.01" required value={fila.pesoNacimientoKg} onChange={(event) => actualizarCria(fila.key, 'pesoNacimientoKg', event.target.value)} /></Field>
          <Field label="Estado al nacer" required><select value={fila.estadoNacimiento} onChange={(event) => actualizarCria(fila.key, 'estadoNacimiento', event.target.value)}>{Object.entries(ESTADO_NACIMIENTO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
          <Field label="Hora de nacimiento"><input type="time" value={fila.horaNacimiento} onChange={(event) => actualizarCria(fila.key, 'horaNacimiento', event.target.value)} /></Field>
          <div className="form-full"><label className="checkbox-line"><input type="checkbox" checked={fila.crearAnimal} onChange={(event) => actualizarCria(fila.key, 'crearAnimal', event.target.checked)} />Crear registro del animal en el inventario</label></div>
          {fila.crearAnimal && <>
            <Field label="Raza de la cría" required hint={razas.isError ? 'No se pudieron cargar las razas. Cierra y vuelve a abrir el formulario para reintentar.' : 'Selecciona la raza de esta cría; no se hereda automáticamente de la madre.'}>
              <select required value={fila.razaPrincipalId} onChange={(event) => actualizarCria(fila.key, 'razaPrincipalId', event.target.value)}>
                <option value="">{razas.isLoading ? 'Cargando razas…' : 'Selecciona la raza…'}</option>
                {razas.data?.map((raza) => <option key={raza.id} value={raza.id}>{raza.nombre}</option>)}
              </select>
            </Field>
            <Field label="Nombre del animal" required><input required maxLength={160} value={fila.nombreAnimal} onChange={(event) => actualizarCria(fila.key, 'nombreAnimal', event.target.value)} autoComplete="off" /></Field>
            <Field label="Potrero inicial"><select value={fila.potreroInicialId} onChange={(event) => actualizarCria(fila.key, 'potreroInicialId', event.target.value)}><option value="">Sin potrero</option>{catalogs?.paddocks.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></Field>
          </>}
          <Field label="Observaciones de la cría"><input maxLength={1000} value={fila.observaciones} onChange={(event) => actualizarCria(fila.key, 'observaciones', event.target.value)} /></Field>
          <div className="form-actions"><Button type="button" variant="ghost" onClick={() => setCrias((prev) => prev.filter((item) => item.key !== fila.key))}>Quitar cría</Button></div>
        </div>)}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={() => setCrias((prev) => [...prev, nuevaCria()])}>Agregar otra cría</Button>
          <Button type="submit" loading={crear.isPending}>Registrar parto</Button>
        </div>
      </form>
    </Modal>
  </div>
}
