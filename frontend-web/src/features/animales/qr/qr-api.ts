import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'
import type { Identificador } from '@/features/animales/types'
import type { QrImageFormat, QrImageSize, QrResolveResult } from '@/features/animales/qr/qr-types'

export async function generarQr(animalId: string, principal?: boolean) {
  return (await http.post<ApiResponse<Identificador>>(
    `/api/v1/animales/${animalId}/identificadores/qr`,
    { principal: principal ?? true },
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function reemplazarQr(animalId: string, identificadorId: string, motivo: string, principal?: boolean, version?: number) {
  return (await http.post<ApiResponse<Identificador>>(
    `/api/v1/animales/${animalId}/identificadores/${identificadorId}/reemplazar-qr`,
    { motivo, principal: principal ?? true, version },
    { headers: { 'Idempotency-Key': crypto.randomUUID() } },
  )).data.data
}

export async function resolverQr(payload: string) {
  return (await http.post<ApiResponse<QrResolveResult>>('/api/v1/qr/resolver', { payload })).data.data
}

export function qrImageUrl(animalId: string, identificadorId: string, options?: { format?: QrImageFormat; size?: QrImageSize; download?: boolean }) {
  const params = new URLSearchParams({
    format: options?.format ?? 'png',
    size: String(options?.size ?? 512),
  })
  if (options?.download) params.set('download', 'true')
  const base = http.defaults.baseURL ?? ''
  return `${base}/api/v1/animales/${animalId}/identificadores/${identificadorId}/qr?${params.toString()}`
}

export async function fetchQrImage(animalId: string, identificadorId: string, options?: { format?: QrImageFormat; size?: QrImageSize; download?: boolean }) {
  const response = await http.get<Blob>(qrImageUrl(animalId, identificadorId, options), { responseType: 'blob' })
  return response.data
}

export function qrMimeType(format: QrImageFormat) {
  return format === 'svg' ? 'image/svg+xml' : 'image/png'
}

export function downloadQr(animalId: string, identificadorId: string, filename: string, options?: { format?: QrImageFormat; size?: QrImageSize }) {
  const format = options?.format ?? 'png'
  return fetchQrImage(animalId, identificadorId, { format, size: options?.size ?? 512, download: true })
    .then((blob) => {
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = filename.endsWith(`.${format}`) ? filename : `${filename}.${format}`
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(url)
    })
}
