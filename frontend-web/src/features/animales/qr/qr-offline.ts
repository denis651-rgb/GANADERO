import { db } from '@/offline/db'
import type { QrPayload, QrResolveResult } from '@/features/animales/qr/qr-types'

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

export async function resolveQrOffline(payloadText: string): Promise<QrResolveResult> {
  const payload = parseQrPayload(payloadText)
  if (!payload) {
    return { valid: false, code: 'INVALID_QR', message: 'El contenido del QR no es un código válido de Ganadero.' }
  }
  const identifier = await db.identificadores.get(payload.identifierId)
  if (!identifier) {
    return { valid: false, code: 'QR_NOT_FOUND', message: 'El QR no está en los datos locales. Sincroniza para poder verificar el código.' }
  }
  const animal = await db.animalesResumen.get(payload.animalId)
  if (!animal) {
    return { valid: false, code: 'QR_NOT_FOUND', message: 'El animal asociado no está en los datos locales. Sincroniza para poder verificar el código.' }
  }
  if (identifier.version !== payload.version) {
    return {
      valid: false,
      code: 'QR_DESACTUALIZADO',
      message: `Este QR es de la versión ${payload.version}, pero el identificador actual es la versión ${identifier.version}. El QR fue reemplazado.`,
      animal: {
        id: animal.id,
        codigo: animal.code,
        nombre: animal.name,
        sexo: animal.sex,
        estado: animal.status,
        propiedadActualId: animal.propertyId,
        potreroActualId: animal.paddockId,
      },
      identifier: {
        id: identifier.id,
        tipo: identifier.tipo,
        valor: identifier.valor,
        estado: identifier.estado,
        principal: identifier.principal,
        fechaAsignacion: identifier.updatedAt,
      },
    }
  }
  return {
    valid: true,
    code: 'QR_VALIDO_LOCAL',
    message: 'QR encontrado en datos locales. La verificación oficial con firma requiere conexión.',
      animal: {
        id: animal.id,
        codigo: animal.code,
        nombre: animal.name,
        sexo: animal.sex,
        estado: animal.status,
        propiedadActualId: animal.propertyId,
        potreroActualId: animal.paddockId,
      },
    identifier: {
      id: identifier.id,
      tipo: identifier.tipo,
      valor: identifier.valor,
      estado: identifier.estado,
      principal: identifier.principal,
      fechaAsignacion: identifier.updatedAt,
    },
  }
}
