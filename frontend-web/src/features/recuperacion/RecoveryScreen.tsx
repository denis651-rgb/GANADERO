import { useEffect, useState } from 'react'
import { AlertTriangle, FolderOpen, LogOut, RefreshCw, RotateCcw } from 'lucide-react'
import type { BackupInfo } from '@/shared/api/http'
import { Alert } from '@/shared/components/Alert'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { LoadingState } from '@/shared/components/LoadingState'

function joinRuta(carpeta: string, archivo: string): string {
  const separador = carpeta.includes('\\') ? '\\' : '/'
  return carpeta.endsWith(separador) ? `${carpeta}${archivo}` : `${carpeta}${separador}${archivo}`
}

/**
 * Se renderiza en vez del router normal cuando Electron no pudo iniciar el backend (base de
 * datos dañada, por ejemplo). No depende del backend para nada: usa únicamente window.ganadero,
 * cuyos métodos de respaldos ya saben leer/verificar directo de la carpeta local sin backend.
 */
export function RecoveryScreen() {
  const desktop = window.ganadero?.backups
  const [lista, setLista] = useState<BackupInfo[] | null>(null)
  const [buscando, setBuscando] = useState(false)
  const [restaurando, setRestaurando] = useState(false)
  const [resultado, setResultado] = useState<{ ok: boolean; mensaje: string } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!desktop) return
    desktop.list().then(setLista).catch((e: Error) => setError(e.message))
  }, [desktop])

  if (!desktop) {
    return <Card><Alert tone="danger">No se pudo inicializar el puente con Ganadero Desktop.</Alert></Card>
  }

  async function confirmarYRestaurar(path: string) {
    if (!window.confirm('¿Restaurar esta base de datos? Se perderá cualquier dato posterior a la fecha de este respaldo.')) return
    setRestaurando(true)
    setError(null)
    try {
      const res = await desktop!.restore(path)
      setResultado(res)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setRestaurando(false)
    }
  }

  async function restaurarUltimoValido() {
    setBuscando(true)
    setError(null)
    try {
      const settings = await desktop!.getSettings()
      const datos = await desktop!.list()
      const ordenados = [...datos].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion))
      let elegido: BackupInfo | null = null
      for (const respaldo of ordenados) {
        const verificado = await desktop!.verify(respaldo.nombreArchivo)
        if (verificado.integridad === 'VALIDA') {
          elegido = verificado
          break
        }
      }
      if (!elegido) {
        setError('No se encontró ningún respaldo con integridad válida en la carpeta local.')
        return
      }
      await confirmarYRestaurar(joinRuta(settings.destinoLocal, elegido.nombreArchivo))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBuscando(false)
    }
  }

  async function elegirOtro() {
    setError(null)
    try {
      const path = await desktop!.selectRestoreFile()
      if (path) await confirmarYRestaurar(path)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const ocupado = buscando || restaurando

  return (
    <div className="page-stack" style={{ maxWidth: 620, margin: '10vh auto', padding: '0 16px' }}>
      <Card>
        <h1><AlertTriangle size={22} aria-hidden="true" style={{ verticalAlign: 'middle', marginRight: 8 }} />Ganadero no pudo abrir la base de datos.</h1>
        <p className="muted">Esto puede pasar si la aplicación se cerró de forma abrupta o si el archivo de datos se dañó. Podés restaurar un respaldo para recuperar tu información.</p>

        {resultado && <Alert tone={resultado.ok ? 'success' : 'danger'}>{resultado.mensaje}</Alert>}
        {error && <Alert tone="danger">{error}</Alert>}
        {restaurando && <LoadingState message="Restaurando la base de datos, no cierres Ganadero…" />}

        {!restaurando && <div className="page-stack">
          <Button onClick={restaurarUltimoValido} loading={buscando} disabled={ocupado}>
            <RotateCcw size={16} aria-hidden="true" />Restaurar el último respaldo válido
          </Button>
          <Button variant="secondary" onClick={elegirOtro} disabled={ocupado}>
            <RotateCcw size={16} aria-hidden="true" />Seleccionar otro respaldo…
          </Button>
          <Button variant="ghost" onClick={() => desktop!.openLocalFolder()} disabled={ocupado}>
            <FolderOpen size={16} aria-hidden="true" />Abrir carpeta de respaldos
          </Button>
          <Button variant="ghost" onClick={() => window.location.reload()} disabled={ocupado}>
            <RefreshCw size={16} aria-hidden="true" />Intentar de nuevo
          </Button>
          <Button variant="ghost" onClick={() => window.ganadero?.quit?.()} disabled={ocupado}>
            <LogOut size={16} aria-hidden="true" />Cerrar Ganadero
          </Button>
        </div>}

        {lista && lista.length === 0 && <Alert tone="warning">No se encontró ningún respaldo en la carpeta local. Usa "Seleccionar otro respaldo" si tenés uno en otra ubicación (por ejemplo, la carpeta sincronizada con Google Drive).</Alert>}
      </Card>
    </div>
  )
}
