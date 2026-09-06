import type { FuenteEdadDeclarada, UnidadEdadDeclarada } from '@/features/animales/types'
import type { ProveedorNuevoInput } from '@/features/proveedores/types'

export type EstadoCompra = 'BORRADOR' | 'CONFIRMADA' | 'ANULADA'
export type ModalidadPrecio = 'POR_UNIDAD' | 'POR_TROPA'
export type TipoPeso = 'MEDIDO' | 'ESTIMADO'

export interface Compra {
  id: string
  codigo: string
  proveedorId: string
  fechaRecepcion: string
  modalidad: ModalidadPrecio
  moneda: string
  cantidadAnimales: number
  precioUnitario?: number
  precioTotal: number
  precioUnitarioReferencial: number
  propiedadId: string
  potreroId: string
  loteGanaderoId?: string
  proposito?: string
  observaciones?: string
  estado: EstadoCompra
  motivoAnulacion?: string
  fechaAnulacion?: string
  origenMigracion?: string
  version: number
}

export interface CompraDetalle {
  id: string
  animalId?: string
  numeroLinea: number
  precioAsignado: number
  pesoIngresoKg?: number
  tipoPeso?: TipoPeso
  metodoPeso?: string
  codigoSolicitado?: string
  nombre?: string
  observaciones?: string
}

export interface CompraDetalleInput {
  codigoSolicitado?: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  razaId?: string
  proposito?: string
  fechaNacimiento?: string
  fechaNacimientoEstimada?: boolean
  edadDeclaradaValor?: number
  edadDeclaradaUnidad?: UnidadEdadDeclarada
  fechaReferenciaEdad?: string
  fuenteEdadDeclarada?: FuenteEdadDeclarada
  observacionEstimacion?: string
  categoriaActualId?: string
  categoriaManualMotivo?: string
  precioOverride?: number
  pesoIngresoKg?: number
  tipoPeso?: TipoPeso
  metodoPeso?: string
  propiedadId?: string
  potreroId?: string
  loteGanaderoId?: string
  observaciones?: string
}

export interface CompraInput {
  proveedorId?: string
  proveedorNuevo?: ProveedorNuevoInput
  fechaRecepcion: string
  modalidad: ModalidadPrecio
  moneda: string
  precioUnitario?: number
  precioTotal?: number
  propiedadId: string
  potreroId: string
  loteGanaderoId?: string
  proposito?: string
  observaciones?: string
  detalles: CompraDetalleInput[]
}

export interface DependenciaCompra {
  tipo: string
  detalle: string
}

export interface CompraPage {
  content: Compra[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ResumenCompraAnimal {
  codigo: string
  fechaRecepcion: string
  modalidad: ModalidadPrecio
  moneda: string
  precioAsignado?: number
  proveedorNombre?: string
  proveedorTelefono?: string
  proveedorDocumento?: string
}

/** Bolivia no observa horario de verano (UTC-4 fijo); se usa mediodía para evitar cambios de día por zona horaria. */
export function fechaRecepcionInstant(fechaYYYYMMDD: string): string {
  return `${fechaYYYYMMDD}T12:00:00-04:00`
}
