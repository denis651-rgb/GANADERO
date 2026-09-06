export type AccionLote = 'MANTENER_LOTE' | 'CAMBIAR_A_LOTE_EXISTENTE' | 'CREAR_NUEVO_LOTE' | 'DEJAR_SIN_LOTE'
export type ModalidadMovimientoLote = 'LOTE_COMPLETO' | 'SELECCION_PARCIAL'
export type SeveridadRestriccion = 'BLOQUEANTE' | 'ADVERTENCIA' | 'INFORMATIVA'
export type EstadoPreparacionLote = 'VIGENTE' | 'CONFIRMADA' | 'EXPIRADA' | 'CANCELADA'

export interface RestriccionSanitaria {
  tipo: string
  severidad: SeveridadRestriccion
  mensaje: string
}

export interface MiembroPreparacionLote {
  animalId: string
  codigo?: string
  nombre?: string
  estado?: string
  propiedadOrigenId?: string
  potreroOrigenId?: string
  loteOrigenId?: string
  animalVersion: number
  elegible: boolean
  motivoExclusion?: string
  seleccionadoPorDefecto: boolean
  restricciones: RestriccionSanitaria[]
}

export interface PreparacionMovimientoLote {
  id: string
  loteOrigenId: string
  propiedadOrigenId?: string
  potreroOrigenId?: string
  modalidad: ModalidadMovimientoLote
  destinoPropiedadId: string
  destinoPotreroId: string
  accionLote: AccionLote
  loteDestinoId?: string
  nuevoLoteNombre?: string
  fechaEfectiva: string
  motivo?: string
  observaciones?: string
  estado: EstadoPreparacionLote
  fechaCaptura: string
  fechaExpiracion: string
  version: number
  totalEncontrados: number
  totalElegibles: number
  totalExcluidos: number
  miembros: MiembroPreparacionLote[]
}

export interface NuevoLoteInput {
  nombre: string
  codigo?: string
  descripcion?: string
}

export interface PrepararMovimientoLoteInput {
  destinoPropiedadId: string
  destinoPotreroId: string
  accionLote: AccionLote
  loteDestinoId?: string
  nuevoLote?: NuevoLoteInput
  fechaEfectiva?: string
  motivo?: string
  observaciones?: string
  modalidadDeclarada?: ModalidadMovimientoLote
}

export interface AutorizacionInput {
  animalId: string
  tipoRestriccion: string
  motivo: string
}

export interface ConfirmarMovimientoLoteInput {
  version: number
  animalIds: string[]
  autorizaciones?: AutorizacionInput[]
}

export interface ResultadoMovimientoLote {
  movimientoId: string
  loteOrigenId: string
  loteDestinoId?: string
  animalesMovidos: number
  animalesPermanecenEnOrigen: number
  loteOrigenVacio: boolean
  identidadTransferida: boolean
  tipoMovimiento: string
}
