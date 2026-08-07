import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLiveQuery } from 'dexie-react-hooks'
import { Image as ImageIcon, Maximize2, RefreshCw, Star, Trash2, Upload } from 'lucide-react'
import { eliminarDocumento, listDocumentos, marcarPrincipalDocumento, subirDocumento, type Documento } from '@/features/archivos/api'
import { compressImages } from '@/features/archivos/utils/imageCompress'
import { db } from '@/offline/db'
import { enqueueFile } from '@/offline/fileQueue'
import { normalizeApiError } from '@/shared/api/errors'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { LoadingState } from '@/shared/components/LoadingState'
import { Modal } from '@/shared/components/Modal'
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus'

export function FotosTab({ animalId }: { animalId: string }) {
  const client = useQueryClient()
  const online = useOnlineStatus()
  const inputRef = useRef<HTMLInputElement>(null)
  const [replacing, setReplacing] = useState(false)
  const [selected, setSelected] = useState<Documento | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Documento | null>(null)
  const [lastError, setLastError] = useState<string | null>(null)

  const fotos = useQuery({ queryKey: ['animal-documentos', animalId], queryFn: () => listDocumentos('ANIMAL', animalId), enabled: Boolean(animalId) })
  const pendientes = useLiveQuery(() => db.archivosPendientes.where('entityId').equals(animalId).toArray(), [animalId], [])

  const invalidateFotos = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ['animal-documentos', animalId] }),
      client.invalidateQueries({ queryKey: ['animal', animalId] }),
      client.invalidateQueries({ queryKey: ['animal-timeline', animalId] }),
    ])
  }

  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      const compressed = await compressImages(files)
      const failures: string[] = []
      for (const image of compressed) {
        try {
          if (online) {
            await subirDocumento(new File([image.blob], image.fileName, { type: image.mimeType }), 'ANIMAL', animalId, replacing)
          } else {
            await enqueueFile({ entityType: 'ANIMAL', entityId: animalId, file: image.blob, fileName: image.fileName, mimeType: image.mimeType, principal: replacing })
          }
        } catch (reason) {
          failures.push(normalizeApiError(reason).message)
        }
      }
      if (failures.length > 0) throw new Error(`${failures.length} fotografía(s) no se pudieron subir. ${failures[0]}`)
    },
    onSuccess: async () => {
      setReplacing(false)
      await invalidateFotos()
    },
    onError: (reason) => setLastError(normalizeApiError(reason).message),
  })

  const markPrincipal = useMutation({
    mutationFn: (id: string) => marcarPrincipalDocumento(id),
    onSuccess: async () => { await invalidateFotos() },
    onError: (reason) => setLastError(normalizeApiError(reason).message),
  })

  const remove = useMutation({
    mutationFn: ({ id, principal }: { id: string; principal: boolean }) => eliminarDocumento(id, principal),
    onSuccess: async () => {
      setDeleteTarget(null)
      if (selected && deleteTarget && selected.id === deleteTarget.id) setSelected(null)
      await invalidateFotos()
    },
    onError: (reason) => setLastError(normalizeApiError(reason).message),
  })

  const principal = fotos.data?.find((foto) => foto.esPrincipal)

  function openPicker(asPrincipal: boolean) {
    setReplacing(asPrincipal)
    inputRef.current?.click()
  }

  const pickFile = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''
    if (files.length > 0) {
      setLastError(null)
      upload.mutate(files)
    }
  }

  return <div className="page-stack">
    {lastError && <Alert tone="danger">{lastError}</Alert>}
    {upload.isSuccess && <Alert tone="success">Fotografías {replacing ? 'reemplazadas' : 'subidas'} correctamente.</Alert>}
    {!online && <Alert tone="info">Sin conexión: las fotos se guardarán en la cola local y se subirán al sincronizar.</Alert>}

    <Card>
      <div className="filter-heading">
        <span><ImageIcon size={18} />Fotografías</span>
        <div className="row-actions">
          <Button variant="secondary" onClick={() => openPicker(true)} disabled={!principal && !online}><RefreshCw size={16} />Reemplazar principal</Button>
          <Button onClick={() => openPicker(false)} loading={upload.isPending}><Upload size={16} />Subir fotos</Button>
          <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={(event) => void pickFile(event)} />
        </div>
      </div>

      {pendientes && pendientes.length > 0 && <div className="operation-list">
        {pendientes.map((file) => <article key={file.localId} className="operation-item">
          <div><strong>{file.fileName}</strong><span>Pendiente de subir</span></div>
          <div className="operation-meta">
            <span className={`status-badge status-${file.status.toLowerCase()}`}>{file.status}</span>
            {file.status === 'PROCESSING' && <small>{file.progress}%</small>}
          </div>
        </article>)}
      </div>}

      {fotos.isPending && <LoadingState message="Cargando fotografías…" />}
      {fotos.data?.length === 0 && <EmptyState title="Sin fotografías" description="Sube la primera fotografía del animal. La principal se muestra en la lista de animales." />}
      {fotos.data && fotos.data.length > 0 && <div className="foto-grid">
        {fotos.data.map((foto) => <figure key={foto.id} className="foto-tile">
          <button type="button" className="foto-thumb" onClick={() => setSelected(foto)} aria-label={`Ampliar ${foto.nombreOriginal}`}>
            <img src={foto.url} alt={foto.nombreOriginal} loading="lazy" />
            {foto.esPrincipal && <span className="foto-principal-badge"><Star size={12} /> Principal</span>}
          </button>
          <figcaption>
            <small>{foto.usuarioNombre ?? '—'} · {new Date(foto.createdAt).toLocaleDateString('es-BO')}</small>
            <button type="button" className="foto-expand" onClick={() => setSelected(foto)} aria-label="Ampliar"><Maximize2 size={14} /></button>
          </figcaption>
        </figure>)}
      </div>}
    </Card>

    <Modal open={Boolean(selected)} title={selected?.nombreOriginal ?? 'Fotografía'} onClose={() => { if (!markPrincipal.isPending && !remove.isPending) setSelected(null) }} wide>
      {selected && <div className="foto-view">
        <img src={selected.url} alt={selected.nombreOriginal} />
        <dl className="detail-list">
          <div><dt>Subida</dt><dd>{new Date(selected.createdAt).toLocaleString('es-BO')}</dd></div>
          <div><dt>Autor</dt><dd>{selected.usuarioNombre ?? '—'}</dd></div>
          <div><dt>Dimensiones</dt><dd>{selected.anchoPx && selected.altoPx ? `${selected.anchoPx} × ${selected.altoPx} px` : '—'}</dd></div>
          <div><dt>Peso</dt><dd>{(selected.tamanoBytes / 1024).toFixed(1)} KB</dd></div>
          <div><dt>Rol</dt><dd>{selected.esPrincipal ? 'Principal' : 'Secundaria'}</dd></div>
        </dl>
        <div className="row-actions">
          {!selected.esPrincipal && <Button variant="secondary" loading={markPrincipal.isPending} onClick={() => markPrincipal.mutate(selected.id)}><Star size={16} />Marcar principal</Button>}
          <Button variant="danger" loading={remove.isPending} onClick={() => setDeleteTarget(selected)}><Trash2 size={16} />Eliminar</Button>
        </div>
      </div>}
    </Modal>

    <Modal open={Boolean(deleteTarget)} title="Eliminar fotografía" onClose={() => { if (!remove.isPending) setDeleteTarget(null) }}>
      {deleteTarget && <div className="page-stack">
        <p className="muted">{deleteTarget.esPrincipal
          ? 'Esta es la fotografía principal del animal. Al eliminarla, el animal quedará sin fotografía principal. ¿Confirmas la eliminación?'
          : '¿Confirmas la eliminación de esta fotografía? La operación se registra en la auditoría y la línea de tiempo.'}</p>
        {remove.error && <Alert tone="danger">{normalizeApiError(remove.error).message}</Alert>}
        <div className="form-actions"><Button variant="danger" loading={remove.isPending} onClick={() => remove.mutate({ id: deleteTarget.id, principal: deleteTarget.esPrincipal })}><Trash2 size={16} />Eliminar definitivamente</Button></div>
      </div>}
    </Modal>
  </div>
}
