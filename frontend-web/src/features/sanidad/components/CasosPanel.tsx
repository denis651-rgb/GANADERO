import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useAuth } from '@/auth/auth-context'
import {
  cerrarCaso,
  crearCaso,
  ESTADO_CASO_LABELS,
  listEnfermedades,
  SEVERIDAD_LABELS,
  type CasoClinico,
  type CrearCasoInput,
  type SeveridadCaso,
} from '@/features/sanidad/api'
import type { SanidadCatalogs } from '@/features/sanidad/catalogs'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { normalizeApiError } from '@/shared/api/errors'

interface CasosPanelProps {
  casos: CasoClinico[]
  isLoading: boolean
  error: unknown
  catalogs?: SanidadCatalogs
  refresh: () => void
}

export function CasosPanel({ casos, isLoading, error, catalogs, refresh }: CasosPanelProps) {
  const { can } = useAuth()
  const canCrear = can('SANIDAD_CASO_CREAR')
  const [showForm, setShowForm] = useState(false)
  const [cerrando, setCerrando] = useState<CasoClinico | null>(null)
  const enfermedades = useQuery({ queryKey: ['sanidad-enfermedades'], queryFn: () => listEnfermedades(false) })

  const crear = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      const input: CrearCasoInput = {
        animalId: String(data.get('animalId')),
        fechaInicio: String(data.get('fechaInicio')),
        sintomas: String(data.get('sintomas')),
        enfermedadId: String(data.get('enfermedadId') || '') || undefined,
        diagnosticoTexto: String(data.get('diagnosticoTexto') || '') || undefined,
        severidad: String(data.get('severidad')) as SeveridadCaso,
        veterinarioId: String(data.get('veterinarioId') || '') || undefined,
        observaciones: String(data.get('observaciones') || '') || undefined,
      }
      return crearCaso(input)
    },
    onSuccess: () => { setShowForm(false); refresh() },
  })

  const cerrar = useMutation({
    mutationFn: (form: HTMLFormElement) => {
      const data = new FormData(form)
      return cerrarCaso(cerrando!.id, String(data.get('resultado')))
    },
    onSuccess: () => { setCerrando(null); refresh() },
  })

  const errorVisible = error ?? crear.error ?? cerrar.error

  return <div className="page-stack">
    {errorVisible && <Alert tone="danger">{normalizeApiError(errorVisible).message}</Alert>}
    <Card>
      <div className="inline-actions" style={{ justifyContent: 'space-between', width: '100%' }}>
        <h2>Casos clínicos</h2>
        {canCrear && <Button onClick={() => setShowForm(true)} disabled={!catalogs}><Plus size={18} aria-hidden="true" />Nuevo caso</Button>}
      </div>
      {isLoading && <LoadingState message="Cargando casos clínicos…" />}
      {!isLoading && casos.length === 0 && <EmptyState title="No hay casos clínicos" description="Registra el primer caso para dar seguimiento a la salud de tus animales." />}
      {casos.length > 0 && <div className="table-wrapper desktop-only"><table><caption className="visually-hidden">Casos clínicos</caption><thead><tr><th scope="col">Animal</th><th scope="col">Inicio</th><th scope="col">Síntomas</th><th scope="col">Enfermedad</th><th scope="col">Severidad</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>{casos.map((caso) => <tr key={caso.id}>
        <td><strong>{catalogs?.animalLabel(caso.animalId)}</strong></td>
        <td>{new Date(caso.fechaInicio).toLocaleDateString('es-BO')}</td>
        <td className="table-secondary">{caso.sintomas}</td>
        <td>{enfermedades.data?.find((item) => item.id === caso.enfermedadId)?.nombre ?? '—'}</td>
        <td><span className={`status-badge status-badge-${caso.severidad === 'CRITICA' ? 'danger' : caso.severidad === 'GRAVE' ? 'warning' : 'pending'}`}>{SEVERIDAD_LABELS[caso.severidad]}</span></td>
        <td><span className="status-badge">{ESTADO_CASO_LABELS[caso.estado]}</span></td>
        <td>{!['CERRADO', 'ANULADO'].includes(caso.estado) && <Button variant="ghost" onClick={() => setCerrando(caso)}>Cerrar caso</Button>}</td>
      </tr>)}</tbody></table></div>}
      {casos.length > 0 && <div className="mobile-only">{casos.map((caso) => <div key={caso.id} className="mobile-entity-card">
        <div><strong>{catalogs?.animalLabel(caso.animalId)}</strong><p className="muted">{caso.sintomas}</p><p className="muted">{new Date(caso.fechaInicio).toLocaleDateString('es-BO')} · {SEVERIDAD_LABELS[caso.severidad]} · {ESTADO_CASO_LABELS[caso.estado]}</p></div>
        {!['CERRADO', 'ANULADO'].includes(caso.estado) && <Button variant="ghost" onClick={() => setCerrando(caso)}>Cerrar caso</Button>}
      </div>)}</div>}
    </Card>

    <Modal open={showForm} title="Nuevo caso clínico" onClose={() => setShowForm(false)} wide description="Registra el caso clínico de un animal.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); crear.mutate(event.currentTarget) }}>
        <Field label="Animal" required><select name="animalId" required><option value="">Selecciona…</option>{catalogs?.animals.map((animal) => <option key={animal.id} value={animal.id}>{animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo}</option>)}</select></Field>
        <Field label="Fecha de inicio" required><input name="fechaInicio" type="date" required /></Field>
        <Field label="Enfermedad"><select name="enfermedadId"><option value="">Sin identificar</option>{enfermedades.data?.map((enfermedad) => <option key={enfermedad.id} value={enfermedad.id}>{enfermedad.nombre}</option>)}</select></Field>
        <Field label="Severidad" required><select name="severidad" required defaultValue="LEVE">{(Object.keys(SEVERIDAD_LABELS) as SeveridadCaso[]).map((severidad) => <option key={severidad} value={severidad}>{SEVERIDAD_LABELS[severidad]}</option>)}</select></Field>
        <Field label="Veterinario"><select name="veterinarioId"><option value="">Sin veterinario</option>{catalogs?.users.map((user) => <option key={user.id} value={user.usuarioId}>{user.nombres} {user.apellidos}</option>)}</select></Field>
        <div className="form-full"><Field label="Síntomas" required><textarea name="sintomas" required rows={3} maxLength={2000} placeholder="Describe los síntomas observados…" /></Field></div>
        <div className="form-full"><Field label="Diagnóstico"><textarea name="diagnosticoTexto" rows={3} maxLength={2000} /></Field></div>
        <div className="form-full"><Field label="Observaciones"><textarea name="observaciones" rows={3} maxLength={1000} /></Field></div>
        <div className="form-actions"><Button type="submit" loading={crear.isPending}>Crear caso</Button></div>
      </form>
    </Modal>

    <Modal open={Boolean(cerrando)} title="Cerrar caso clínico" onClose={() => setCerrando(null)} description="Registra el desenlace del caso clínico.">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); cerrar.mutate(event.currentTarget) }}>
        {cerrando && <Alert tone="info">Caso de {catalogs?.animalLabel(cerrando.animalId)} iniciado el {new Date(cerrando.fechaInicio).toLocaleDateString('es-BO')}.</Alert>}
        <Field label="Resultado" required><textarea name="resultado" required rows={3} maxLength={2000} placeholder="Recuperado, sin complicaciones…" /></Field>
        <div className="form-actions"><Button type="submit" loading={cerrar.isPending}>Cerrar caso</Button></div>
      </form>
    </Modal>
  </div>
}
