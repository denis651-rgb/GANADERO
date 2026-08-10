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
  responsableNombre?: string
  propiedadId?: string
  propiedadNombre?: string
  potreroId?: string
  potreroNombre?: string
  loteId?: string
  loteNombre?: string
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

export interface EvolucionPesaje {
  fecha: string
  pesoKg: number
  condicionCorporal?: number
  tipo: TipoPesaje
}

export interface PesajeIndicadorAnimal {
  animalId: string
  codigoAnimal?: string
  nombreAnimal?: string
  ultimoPesoKg?: number
  fechaUltimoPesaje?: string
  pesoAnteriorKg?: number
  fechaPesoAnterior?: string
  variacionKg?: number
  variacionPct?: number
  gananciaDiariaKg?: number
  promedioLoteKg?: number
  animalesPesadosLote?: number
  diferenciaVsLoteKg?: number
  diferenciaVsLotePct?: number
  evolucion: EvolucionPesaje[]
}

export interface PesajeIndicadorLote {
  loteId: string
  codigoLote?: string
  nombreLote?: string
  animalesTotales?: number
  animalesPesados?: number
  animalesSinPesaje?: number
  pesoPromedioKg?: number
  pesoMinimoKg?: number
  pesoMaximoKg?: number
  fechaPrimerPesaje?: string
  fechaUltimoPesaje?: string
}

export interface PesajeSinPesaje {
  animalId: string
  codigoAnimal?: string
  nombreAnimal?: string
  ultimoPesaje?: string
  pesoUltimoKg?: number
  diasSinPesaje: number
}

export interface PesajeSinPesajePage {
  content: PesajeSinPesaje[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PesajeMasivoItemInput {
  id: string
  animalId: string
  fecha?: string
  pesoKg: number
  tipo?: TipoPesaje
  condicionCorporal?: number
  bascula?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  observaciones?: string
}

export interface PesajeMasivoInput {
  fecha?: string
  dispositivo?: string
  observaciones?: string
  items: PesajeMasivoItemInput[]
}

export interface PesajeMasivoItemResultado {
  animalId: string
  codigoAnimal?: string
  nombreAnimal?: string
  ok: boolean
  pesaje?: Pesaje
  errorCode?: string
  errorMessage?: string
}

export interface PesajeMasivoResultado {
  items: PesajeMasivoItemResultado[]
  registrados: number
  conError: number
}
