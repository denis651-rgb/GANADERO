import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import jsQR from 'jsqr'
import { ScanLine, CameraOff } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { resolverQr } from '@/features/animales/qr/qr-api'
import { parseQrPayload, resolveQrOffline } from '@/features/animales/qr/qr-offline'
import { normalizeApiError } from '@/shared/api/errors'
import type { QrResolveResult } from '@/features/animales/qr/qr-types'

type ScanStatus = 'idle' | 'starting' | 'scanning' | 'camera-error'

function formatFecha(fecha?: string) {
  if (!fecha) return '—'
  const date = new Date(fecha)
  return isNaN(date.getTime()) ? fecha : date.toLocaleDateString('es-BO')
}

export function QrScanner() {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef<number>(0)
  const lockedRef = useRef(false)
  const [status, setStatus] = useState<ScanStatus>('idle')
  const [statusMessage, setStatusMessage] = useState('')
  const [result, setResult] = useState<QrResolveResult | null>(null)
  const [manual, setManual] = useState('')

  const stopCamera = useCallback(() => {
    cancelAnimationFrame(rafRef.current)
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (videoRef.current) videoRef.current.srcObject = null
  }, [])

  const resolve = useCallback(async (payload: string) => {
    lockedRef.current = true
    setResult(null)
    stopCamera()
    setStatus('idle')
    try {
      const online = await resolverQr(payload)
      setResult(online)
      return
    } catch (reason) {
      const error = normalizeApiError(reason)
      if (error.code === 'NETWORK_ERROR') {
        const offline = await resolveQrOffline(payload)
        setResult(offline)
        return
      }
      setResult({ valid: false, code: error.code ?? 'ERROR', message: error.message })
    }
  }, [stopCamera])

  function scanLoop() {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas || video.readyState < 2) {
      rafRef.current = requestAnimationFrame(scanLoop)
      return
    }
    const width = video.videoWidth
    const height = video.videoHeight
    if (width === 0 || height === 0) {
      rafRef.current = requestAnimationFrame(scanLoop)
      return
    }
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) {
      rafRef.current = requestAnimationFrame(scanLoop)
      return
    }
    context.drawImage(video, 0, 0, width, height)
    const imageData = context.getImageData(0, 0, width, height)
    const code = jsQR(imageData.data, width, height, { inversionAttempts: 'dontInvert' })
    if (code?.data) {
      if (parseQrPayload(code.data)) {
        void resolve(code.data)
        return
      }
      setStatusMessage('QR detectado pero no corresponde a un código de Ganadero.')
    }
    rafRef.current = requestAnimationFrame(scanLoop)
  }

  async function startCamera() {
    if (streamRef.current) return
    lockedRef.current = false
    setStatus('starting')
    setResult(null)
    setStatusMessage('')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      })
      if (!videoRef.current) {
        stream.getTracks().forEach((track) => track.stop())
        return
      }
      streamRef.current = stream
      videoRef.current.srcObject = stream
      videoRef.current.setAttribute('playsinline', 'true')
      await videoRef.current.play()
      setStatus('scanning')
      rafRef.current = requestAnimationFrame(scanLoop)
    } catch {
      setStatus('camera-error')
      setStatusMessage('No se pudo acceder a la cámara. Usa el campo manual para pegar el contenido del QR.')
    }
  }

  async function scanAnother() {
    stopCamera()
    lockedRef.current = false
    setResult(null)
    setStatusMessage('')
    setStatus('idle')
    await startCamera()
  }

  useEffect(() => {
    return () => {
      cancelAnimationFrame(rafRef.current)
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [])

  const submitManual = (event: React.FormEvent) => {
    event.preventDefault()
    if (manual.trim()) void resolve(manual.trim())
  }

  return (
    <div className="qr-scanner">
      {!result && (
        <>
          <div>
            <video ref={videoRef} muted playsInline />
            <canvas ref={canvasRef} className="qr-scanner-canvas" />
          </div>
          <div className="qr-scanner-caption">
            <ScanLine size={16} />
            {status === 'scanning' ? 'Apuntando a un código QR de Ganadero…' : statusMessage || 'El escáner resuelve el QR verificando su firma en el servidor y usa los datos locales si estás sin conexión.'}
          </div>
          {status !== 'scanning' && (
            <div>
              <Button onClick={() => void startCamera()} loading={status === 'starting'}>
                <CameraOff size={16} /> {status === 'camera-error' ? 'Reintentar cámara' : 'Activar cámara'}
              </Button>
            </div>
          )}
          <form className="form-grid" onSubmit={submitManual}>
            <div className="field form-full">
              <label className="field-label" htmlFor="qr-manual">Contenido del QR (manual)</label>
              <div className="field-control">
                <input id="qr-manual" value={manual} onChange={(event) => setManual(event.target.value)} placeholder="Pega aquí el contenido del QR copiado…" />
              </div>
            </div>
            <div className="form-actions form-full">
              <Button type="submit" disabled={!manual.trim()}>Resolver QR</Button>
            </div>
          </form>
        </>
      )}

      {result && (
        <div className={result.valid ? 'alert alert-success' : 'alert alert-danger'}>
          <div>
            <span><strong>{result.message}</strong></span>
            {result.animal && (
              <span>
                Animal <strong>{result.animal.codigo}</strong>{result.animal.nombre ? ` · ${result.animal.nombre}` : ''} · Estado {result.animal.estado}
              </span>
            )}
            {result.identifier && (
              <span>
                Identificador <strong>{result.identifier.valor}</strong> · Principal: {result.identifier.principal ? 'sí' : 'no'} · Asignado: {formatFecha(result.identifier.fechaAsignacion)}
              </span>
            )}
            {!result.valid && result.code === 'QR_NOT_FOUND' && (
              <span>El código puede pertenecer a otra empresa o haber sido retirado.</span>
            )}
          </div>
        </div>
      )}

      {result && result.animal && (
        <div className="row-actions">
          <Link className="button button-secondary" to={`/animales/${result.animal.id}`}>Ver ficha del animal</Link>
          <Button variant="primary" onClick={() => void scanAnother()}>Escanear otro</Button>
        </div>
      )}
      {result && !result.animal && (
        <div>
          <Button variant="secondary" onClick={() => void scanAnother()}>Escanear otro</Button>
        </div>
      )}
    </div>
  )
}
