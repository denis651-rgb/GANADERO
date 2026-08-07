export type QrImageFormat = 'png' | 'svg'
export type QrImageSize = 256 | 512 | 1024
export type PrintFormat = 'PEQUENO' | 'MEDIANO' | 'A4_MULTIPLE'

export interface QrPayload {
  type: 'GANADERO_ANIMAL'
  animalId: string
  identifierId: string
  version: number
  signature: string
}

export interface QrResolveAnimal {
  id: string
  codigo: string
  nombre?: string
  sexo: string
  estado: string
  propiedadActualId: string
  potreroActualId: string
}

export interface QrResolveIdentifier {
  id: string
  tipo: string
  valor: string
  estado: string
  principal: boolean
  fechaAsignacion: string
  fechaRetiro?: string
}

export interface QrResolveResult {
  valid: boolean
  code: string
  message: string
  animal?: QrResolveAnimal
  identifier?: QrResolveIdentifier
}

export interface PrintItem {
  animalId: string
  identifierId: string
  codigo: string
}

export const PRINT_FORMATS: Array<{ value: PrintFormat; label: string; description: string }> = [
  { value: 'PEQUENO', label: 'Pequeño', description: 'Etiqueta de 38 × 25 mm, ideal para caravanas.' },
  { value: 'MEDIANO', label: 'Mediano', description: 'Etiqueta de 52 × 32 mm, lectura a mayor distancia.' },
  { value: 'A4_MULTIPLE', label: 'Hojas A4 múltiple', description: '8 etiquetas por hoja para impresión masiva.' },
]
