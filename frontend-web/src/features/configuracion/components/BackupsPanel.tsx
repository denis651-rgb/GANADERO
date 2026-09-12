import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DatabaseBackup, Download, Eye, FolderOpen, RotateCcw, Stethoscope, Trash2 } from 'lucide-react'
import type { BackupInfo, BackupManifestInfo, BackupSettings } from '@/shared/api/http'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Field } from '@/shared/components/Field'
import { LoadingState } from '@/shared/components/LoadingState'
import { formatDate } from '@/shared/utils/date'
import { formatBytes } from '@/shared/utils/bytes'

const FRECUENCIA_LABEL: Record<BackupSettings['frecuencia'], string> = { DIARIA: 'Diaria', SEMANAL: 'Semanal', MENSUAL: 'Mensual' }
const ESTADO_LABEL: Record<BackupInfo['estado'], string> = {
  CREANDO: 'Creando…', CREADO_LOCALMENTE: 'Guardado localmente', COPIANDO_A_CARPETA_EXTERNA: 'Copiando a la carpeta externa…',
  COPIADO_A_CARPETA_EXTERNA: 'Copiado a la carpeta externa', ERROR_DE_COPIA: 'Error al copiar', INTEGRIDAD_INVALIDA: 'Integridad inválida',
}
const INTEGRIDAD_BADGE: Record<BackupInfo['integridad'], string> = { DESCONOCIDA: 'status-badge', VALIDA: 'status-badge status-activo', INVALIDA: 'status-badge status-badge-danger' }
const INTEGRIDAD_LABEL: Record<BackupInfo['integridad'], string> = { DESCONOCIDA: 'Sin verificar', VALIDA: 'Íntegro', INVALIDA: 'Corrupto' }
const CONFIRMACION_ESPERADA = 'RESTAURAR'

export function BackupsPanel() {
  const desktop = window.ganadero?.backups
  const diagnostics = window.ganadero?.diagnostics
  const client = useQueryClient()
  const [draft, setForm] = useState<BackupSettings | null>(null)
  const [restoreTarget, setRestoreTarget] = useState<{ path: string; manifest: BackupManifestInfo } | null>(null)
  const [confirmText, setConfirmText] = useState('')
  const [restoreResultMsg, setRestoreResultMsg] = useState<{ ok: boolean; mensaje: string } | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<BackupInfo | null>(null)
  const [diagnosticoResultMsg, setDiagnosticoResultMsg] = useState<string | null>(null)

  const settingsQuery = useQuery({ queryKey: ['backup-settings'], queryFn: () => desktop!.getSettings(), enabled: Boolean(desktop) })
  const listQuery = useQuery({ queryKey: ['backups-list'], queryFn: () => desktop!.list(), enabled: Boolean(desktop) })

  const form = draft ?? settingsQuery.data

  const guardarConfig = useMutation({
    mutationFn: (input: BackupSettings) => desktop!.saveSettings(input),
    onSuccess: (saved) => { setForm(saved); client.setQueryData(['backup-settings'], saved) },
  })
  const elegirCarpetaExterna = useMutation({
    mutationFn: () => desktop!.selectExternalFolder(),
    onSuccess: (carpeta) => { if (carpeta && form) setForm({ ...form, destinoExterno: carpeta }) },
  })
  const crear = useMutation({
    mutationFn: () => desktop!.createNow(),
    onSuccess: () => client.invalidateQueries({ queryKey: ['backups-list'] }),
  })
  const verificarTodos = useMutation({
    mutationFn: async () => {
      const lista = listQuery.data ?? []
      for (const respaldo of lista) await desktop!.verify(respaldo.nombreArchivo)
    },
    onSuccess: () => client.invalidateQueries({ queryKey: ['backups-list'] }),
  })
  const eliminar = useMutation({
    mutationFn: (nombre: string) => desktop!.deleteBackup(nombre),
    onSuccess: () => { setDeleteTarget(null); client.invalidateQueries({ queryKey: ['backups-list'] }) },
  })
  const elegirRestaurar = useMutation({
    mutationFn: async () => {
      const path = await desktop!.selectRestoreFile()
      if (!path) return null
      const info = await desktop!.inspectRestoreFile(path)
      return { path, manifest: info.manifest }
    },
    onSuccess: (result) => { if (result) setRestoreTarget(result) },
  })
  const restaurar = useMutation({
    mutationFn: () => desktop!.restore(restoreTarget!.path),
    onSuccess: (resultado) => {
      setRestoreTarget(null)
      setConfirmText('')
      setRestoreResultMsg(resultado)
      client.invalidateQueries({ queryKey: ['backups-list'] })
    },
  })
  const exportarDiagnostico = useMutation({
    mutationFn: () => diagnostics!.export(),
    onSuccess: (resultado) => setDiagnosticoResultMsg(resultado.cancelado ? null : `Diagnóstico guardado en ${resultado.path}`),
  })

  if (!desktop) {
    return <Card>
      <Alert tone="warning">Abre esta pantalla desde la aplicación Ganadero Desktop para crear, sincronizar y restaurar respaldos.</Alert>
    </Card>
  }

  const operacionEnCurso = crear.isPending || verificarTodos.isPending || restaurar.isPending || guardarConfig.isPending || eliminar.isPending
  const error = settingsQuery.error ?? listQuery.error ?? guardarConfig.error ?? crear.error ?? verificarTodos.error ?? eliminar.error ?? elegirRestaurar.error ?? exportarDiagnostico.error
  const lista = listQuery.data ?? []
  const ultimoRespaldo = lista[0]
  const ultimoErrorRespaldo = lista.find((r) => r.ultimoError)?.ultimoError

  return <Card>
    {error && <Alert tone="danger">{(error as Error).message}</Alert>}
    {restoreResultMsg && <Alert tone={restoreResultMsg.ok ? 'success' : 'danger'}>{restoreResultMsg.mensaje}</Alert>}
    {diagnosticoResultMsg && <Alert tone="success">{diagnosticoResultMsg}</Alert>}

    <div className="section-heading"><h4>Resumen</h4></div>
    <div className="metric-grid">
      <Card className="metric-card"><div><span>Último respaldo</span><strong>{ultimoRespaldo ? formatDate(ultimoRespaldo.fechaCreacion) : 'Ninguno'}</strong></div></Card>
      <Card className="metric-card"><div><span>Próximo respaldo</span><strong>{form?.automatico ? `${FRECUENCIA_LABEL[form.frecuencia]} a las ${form.hora}` : 'Desactivado'}</strong></div></Card>
      <Card className="metric-card"><div><span>Copia externa</span><strong>{form?.destinoExterno ? 'Configurada' : 'Sin configurar'}</strong></div></Card>
      <Card className="metric-card"><div><span>Último error</span><strong>{ultimoErrorRespaldo ?? 'Ninguno'}</strong></div></Card>
    </div>
    {form?.destinoExterno && <p className="muted">Ganadero confirma la copia a la carpeta sincronizada, pero no puede confirmar la subida a Google Drive sin usar su API — revisa el ícono de Google Drive para confirmar que terminó de subir.</p>}

    <div className="section-heading"><h4>Configuración</h4></div>
    {settingsQuery.isPending && <LoadingState message="Cargando configuración…" />}
    {form && <form className="form-grid" onSubmit={(event) => { event.preventDefault(); guardarConfig.mutate(form) }}>
      <Field label="Respaldo automático">
        <label className="checkbox-line"><input type="checkbox" checked={form.automatico} onChange={(event) => setForm({ ...form, automatico: event.target.checked })} /> Activar</label>
      </Field>
      <Field label="Frecuencia">
        <select value={form.frecuencia} disabled={!form.automatico} onChange={(event) => setForm({ ...form, frecuencia: event.target.value as BackupSettings['frecuencia'] })}>
          <option value="DIARIA">Diaria</option>
          <option value="SEMANAL">Semanal</option>
          <option value="MENSUAL">Mensual</option>
        </select>
      </Field>
      <Field label="Hora"><input type="time" value={form.hora} disabled={!form.automatico} onChange={(event) => setForm({ ...form, hora: event.target.value })} /></Field>
      <Field label="Carpeta local" hint="Se guarda siempre dentro de la carpeta de datos de Ganadero."><input value={form.destinoLocal} readOnly /></Field>
      <Field label="Carpeta sincronizada (Google Drive)" hint="Ej. G:\Mi unidad\Ganadero\Respaldos">
        <div className="picker-selected">
          <input value={form.destinoExterno ?? ''} readOnly placeholder="Sin configurar" />
          <Button type="button" variant="ghost" loading={elegirCarpetaExterna.isPending} onClick={() => elegirCarpetaExterna.mutate()}>Elegir carpeta…</Button>
        </div>
      </Field>
      <Field label="Retención diaria"><input type="number" min={0} value={form.retencionDiarios} onChange={(event) => setForm({ ...form, retencionDiarios: Number(event.target.value) })} /></Field>
      <Field label="Retención semanal"><input type="number" min={0} value={form.retencionSemanales} onChange={(event) => setForm({ ...form, retencionSemanales: Number(event.target.value) })} /></Field>
      <Field label="Retención mensual"><input type="number" min={0} value={form.retencionMensuales} onChange={(event) => setForm({ ...form, retencionMensuales: Number(event.target.value) })} /></Field>
      <div className="form-actions form-full"><Button type="submit" loading={guardarConfig.isPending}>Guardar configuración</Button></div>
    </form>}

    <div className="section-heading"><h4>Acciones</h4></div>
    <div className="inline-actions">
      <Button onClick={() => crear.mutate()} loading={crear.isPending} disabled={operacionEnCurso}><DatabaseBackup size={16} aria-hidden="true" />Crear respaldo ahora</Button>
      <Button variant="secondary" onClick={() => elegirRestaurar.mutate()} loading={elegirRestaurar.isPending} disabled={operacionEnCurso}><RotateCcw size={16} aria-hidden="true" />Restaurar respaldo…</Button>
      <Button variant="secondary" onClick={() => verificarTodos.mutate()} loading={verificarTodos.isPending} disabled={operacionEnCurso || lista.length === 0}><Eye size={16} aria-hidden="true" />Verificar respaldos</Button>
      <Button variant="ghost" onClick={() => desktop.openLocalFolder()}><FolderOpen size={16} aria-hidden="true" />Abrir carpeta local</Button>
      <Button variant="ghost" disabled={!form?.destinoExterno} onClick={() => desktop.openExternalFolder()}><FolderOpen size={16} aria-hidden="true" />Abrir carpeta externa</Button>
    </div>

    <div className="section-heading"><h4>Historial</h4></div>
    {listQuery.isPending && <LoadingState message="Cargando historial de respaldos…" />}
    {lista.length === 0 && !listQuery.isPending && <EmptyState title="Sin respaldos" description="Crea el primer respaldo con el botón de arriba." />}
    {lista.length > 0 && <div className="table-wrapper"><table><thead><tr>
      <th scope="col">Fecha</th><th scope="col">Nombre</th><th scope="col">Tamaño</th><th scope="col">Integridad</th>
      <th scope="col">Estado local</th><th scope="col">Acciones</th>
    </tr></thead><tbody>{lista.map((respaldo) => <tr key={respaldo.nombreArchivo}>
      <td>{formatDate(respaldo.fechaCreacion)}</td>
      <td>{respaldo.nombreArchivo}</td>
      <td>{formatBytes(respaldo.tamanoBytes)}</td>
      <td><span className={INTEGRIDAD_BADGE[respaldo.integridad]}>{INTEGRIDAD_LABEL[respaldo.integridad]}</span></td>
      <td>{ESTADO_LABEL[respaldo.estado]}{respaldo.ultimoError ? ` — ${respaldo.ultimoError}` : ''}</td>
      <td className="inline-actions">
        <Button variant="ghost" title="Verificar integridad" aria-label={`Verificar ${respaldo.nombreArchivo}`}
          onClick={() => desktop.verify(respaldo.nombreArchivo).then(() => client.invalidateQueries({ queryKey: ['backups-list'] }))}>
          <Eye size={16} aria-hidden="true" />
        </Button>
        <a className="button button-ghost" title="Descargar" aria-label={`Descargar ${respaldo.nombreArchivo}`}
          href={`${window.ganadero?.apiBaseUrl ?? ''}/api/v1/respaldos/${encodeURIComponent(respaldo.nombreArchivo)}/descargar`} target="_blank" rel="noreferrer">
          <Download size={16} aria-hidden="true" />
        </a>
        <Button variant="ghost" title="Eliminar" aria-label={`Eliminar ${respaldo.nombreArchivo}`} onClick={() => setDeleteTarget(respaldo)}>
          <Trash2 size={16} aria-hidden="true" />
        </Button>
      </td>
    </tr>)}</tbody></table></div>}

    <div className="section-heading"><h4>Diagnóstico</h4></div>
    <p className="muted">Si algo falla, exporta esta información antes de contactar soporte: incluye los registros de la aplicación y del backend, sin datos de la operación.</p>
    <div className="inline-actions">
      <Button variant="secondary" loading={exportarDiagnostico.isPending} onClick={() => exportarDiagnostico.mutate()}>
        <Stethoscope size={16} aria-hidden="true" />Exportar información de diagnóstico
      </Button>
      <Button variant="ghost" onClick={() => diagnostics!.openLogsFolder()}>
        <FolderOpen size={16} aria-hidden="true" />Abrir carpeta de logs
      </Button>
    </div>

    <ConfirmDialog
      open={Boolean(restoreTarget)}
      title="Restaurar base de datos"
      description="Esta acción reemplaza toda la información actual por la del respaldo elegido. Se creará un respaldo preventivo de la base actual antes de continuar."
      confirmLabel="Restaurar"
      variant="danger"
      loading={restaurar.isPending}
      disabled={confirmText !== CONFIRMACION_ESPERADA}
      error={restaurar.error}
      onClose={() => { if (!restaurar.isPending) { setRestoreTarget(null); setConfirmText('') } }}
      onConfirm={() => restaurar.mutate()}
    >
      {restoreTarget && <div className="page-stack">
        <p><strong>Fecha del respaldo:</strong> {formatDate(restoreTarget.manifest.fechaCreacion)}</p>
        <p><strong>Versión de la app:</strong> {restoreTarget.manifest.versionAplicacion}</p>
        <p><strong>Versión de la base:</strong> {restoreTarget.manifest.versionBaseDatos ?? '—'}</p>
        <Field label={`Escribe ${CONFIRMACION_ESPERADA} para confirmar`}>
          <input value={confirmText} onChange={(event) => setConfirmText(event.target.value)} autoComplete="off" />
        </Field>
      </div>}
    </ConfirmDialog>

    <ConfirmDialog
      open={Boolean(deleteTarget)}
      title="Eliminar respaldo"
      description={deleteTarget ? `Se eliminará ${deleteTarget.nombreArchivo} de forma permanente.` : undefined}
      confirmLabel="Eliminar"
      variant="danger"
      loading={eliminar.isPending}
      error={eliminar.error}
      onClose={() => { if (!eliminar.isPending) setDeleteTarget(null) }}
      onConfirm={() => eliminar.mutate(deleteTarget!.nombreArchivo)}
    >
      <p className="muted">No se puede deshacer.</p>
    </ConfirmDialog>
  </Card>
}
