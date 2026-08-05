export type TipoPesaje = 'RUTINA' | 'NACIMIENTO' | 'DESTETE' | 'ENTRADA' | 'VENTA' | 'PESADA_ESPECIAL'
export type EstadoPesaje = 'ACTIVO' | 'ANULADO'

export interface Pesaje {
  id: string
  animalId: string
  codigoAnimal?: string
  nombreAnimal?: string
  fecha: string
  pesoKg: number
  tipo: TipoPesaje
  condicionCorporal?: number
  bascula?: string
  responsableId?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  dispositivo?: string
  clienteUuid?: string
  estado: EstadoPesaje
  motivoAnulacion?: string
  observaciones?: string
  version: number
}

export interface PesajeFilters {
  animalId?: string
  propiedadId?: string
  page: number
  size: number
}

export interface RegistrarPesajeInput {
  animalId: string
  fecha?: string
  pesoKg: number
  tipo?: TipoPesaje
  condicionCorporal?: number
  bascula?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  dispositivo?: string
  clienteUuid?: string
  idempotencyKey?: string
  observaciones?: string
}

export interface PesajeLoteInput {
  loteId: string
  pesoKg: number
  fecha?: string
  dispositivo?: string
  idempotencyKey?: string
  observaciones?: string
}

export interface AnularPesajeInput {
  motivo: string
  version: number
}
