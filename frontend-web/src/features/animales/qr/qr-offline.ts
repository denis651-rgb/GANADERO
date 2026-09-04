import type { QrPayload } from '@/features/animales/qr/qr-types'

export function parseQrPayload(payloadText: string): QrPayload | null {
  try {
    const parsed = JSON.parse(payloadText) as Partial<QrPayload>
    if (parsed?.type !== 'GANADERO_ANIMAL') return null
    if (typeof parsed.animalId !== 'string' || typeof parsed.identifierId !== 'string') return null
    if (typeof parsed.version !== 'number' || typeof parsed.signature !== 'string') return null
    return parsed as QrPayload
  } catch {
    return null
  }
}
