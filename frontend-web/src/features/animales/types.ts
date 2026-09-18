export interface AnimalSummary {
  id: string
  codigo: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  categoriaActualId: string
  fechaNacimiento?: string
  fechaNacimientoEstimada: boolean
  razaPrincipalId: string
  color?: string
  proposito: 'CARNE' | 'LECHE' | 'REPRODUCCION' | 'DOBLE_PROPOSITO'
  origen: 'NACIDO' | 'COMPRADO' | 'TRANSFERIDO'
  estado: AnimalState
  propiedadActualId: string
  potreroActualId: string
  loteActualId?: string
  fechaIngreso: string
  precioAdquisicion?: number
  pesoIngresoKg?: number
  pesoIngresoEstimado?: boolean
  pesoNacimientoKg?: number
  condicionCorporalActual?: number
  fotoPrincipalPath?: string
  observaciones?: string
  version: number
  edadDeclaradaValor?: number
  edadDeclaradaUnidad?: UnidadEdadDeclarada
  fechaReferenciaEdad?: string
  fuenteEdadDeclarada?: FuenteEdadDeclarada
  observacionEstimacion?: string
}

export type AnimalState = 'ACTIVO' | 'VENDIDO' | 'MUERTO' | 'PERDIDO' | 'TRANSFERIDO' | 'DESCARTADO'
export type UnidadEdadDeclarada = 'DIAS' | 'MESES' | 'ANIOS'
export type FuenteEdadDeclarada = 'PROVEEDOR' | 'ESTIMACION_CAMPO'
export type TipoCambioCategoria = 'AUTOMATICO' | 'MANUAL' | 'CORRECCION'

export interface Raza { id: string; codigo: string; nombre: string; especie: string }
export interface CategoriaAnimal {
  id: string
  codigo: string
  nombre: string
  sexoAplicable: 'MACHO' | 'HEMBRA' | 'AMBOS'
  edadMinMeses?: number
  edadMaxMeses?: number
  descripcion?: string
  activo: boolean
  clasificacionAutomatica: boolean
  ordenEvaluacion: number
}

export interface HistorialCategoriaAnimal {
  id: string
  categoriaAnteriorId?: string
  categoriaNuevaId: string
  fechaCambio: string
  tipoCambio: TipoCambioCategoria
  motivo?: string
  usuarioId?: string
  edadDias?: number
  edadConfirmada: boolean
}

export interface CreateAnimalInput {
  fechaNacimientoEstimada?: boolean
  fechaIngreso?: string
  color?: string
  pesoNacimientoKg?: number
  condicionCorporalActual?: number
  pesoIngresoKg?: number
  pesoIngresoEstimado?: boolean
  codigo?: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  fechaNacimiento?: string
  proposito: 'CARNE' | 'LECHE' | 'REPRODUCCION' | 'DOBLE_PROPOSITO'
  origen: 'NACIDO' | 'COMPRADO' | 'TRANSFERIDO'
  razaPrincipalId: string
  categoriaActualId: string
  propiedadActualId: string
  potreroActualId: string
  observaciones?: string
  edadDeclaradaValor?: number
  edadDeclaradaUnidad?: UnidadEdadDeclarada
  fechaReferenciaEdad?: string
  fuenteEdad?: FuenteEdadDeclarada
  observacionEstimacion?: string
  categoriaManualMotivo?: string
}

/** Alta masiva de compra (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, sección 7): origen queda fijo en COMPRADO. */
export interface AnimalLoteItemInput {
  codigo?: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  categoriaActualId: string
  fechaNacimiento?: string
  fechaNacimientoEstimada?: boolean
  pesoIngresoKg?: number
  pesoIngresoEstimado?: boolean
  observaciones?: string
  edadDeclaradaValor?: number
  edadDeclaradaUnidad?: UnidadEdadDeclarada
  fechaReferenciaEdad?: string
  fuenteEdad?: FuenteEdadDeclarada
  observacionEstimacion?: string
  categoriaManualMotivo?: string
}

export interface CrearAnimalesLoteInput {
  razaPrincipalId: string
  proposito: 'CARNE' | 'LECHE' | 'REPRODUCCION' | 'DOBLE_PROPOSITO'
  propiedadActualId: string
  potreroActualId: string
  fechaIngreso?: string
  precioAdquisicion?: number
  animales: AnimalLoteItemInput[]
}

export interface UpdateAnimalInput extends Omit<CreateAnimalInput, 'origen' | 'razaPrincipalId'> {
  /** Ausente cuando se manda razaNueva en su lugar (opción "Otra" del campo Raza). */
  razaPrincipalId?: string
  /** Nombre de una raza que todavía no existe en el catálogo: el backend la crea (o reutiliza
   * si ya existe una con ese nombre) y la usa para este animal. */
  razaNueva?: string
  quitarFechaNacimiento?: boolean
  confirmarFechaNacimiento?: boolean
  corregirPesoCompra?: boolean
  fechaNacimientoEstimada?: boolean
  color?: string
  fechaIngreso?: string
  precioAdquisicion?: number
  pesoNacimientoKg?: number
  condicionCorporalActual?: number
  fotoPrincipalPath?: string
  version: number
}

export interface AnimalEvent {
  id: string
  tipo: string
  fechaEvento: string
  estadoAnterior?: AnimalState
  estadoNuevo?: AnimalState
  motivo?: string
  registradoPor?: string
  titulo?: string
  descripcion?: string
  moduloOrigen?: string
  registroOrigen?: string
  dispositivo?: string
  metadata?: string
  createdBy?: string
}

export interface TimelineEvent {
  id: string
  tipo: string
  titulo?: string
  descripcion?: string
  fechaTecnica: string
  fechaEvento: string
  usuarioId?: string
  usuarioNombre?: string
  dispositivoId?: string
  moduloOrigen: string
  registroOrigenId?: string
  metadata: Record<string, unknown>
  origenSync: boolean
  idempotencyKey?: string
}

export type TipoIdentificador = 'ARETE' | 'QR' | 'RFID' | 'TATUAJE' | 'OTRO'
export type EstadoIdentificador = 'ACTIVO' | 'RETIRADO'

export interface Identificador {
  id: string
  animalId: string
  tipo: TipoIdentificador
  valor: string
  principal: boolean
  estado: EstadoIdentificador
  fechaAsignacion: string
  fechaRetiro?: string
  motivoRetiro?: string
  observaciones?: string
  payload?: string
  version: number
}

export interface AsignarIdentificadorInput {
  tipo: TipoIdentificador
  valor: string
  principal?: boolean
  observaciones?: string
}

export interface ActualizarIdentificadorInput {
  tipo?: TipoIdentificador
  valor?: string
  principal?: boolean
  observaciones?: string
  version?: number
}

export type TipoParentesco = 'MADRE' | 'PADRE'

export interface Parentesco {
  id: string
  animalId: string
  tipo: TipoParentesco
  animalPadreId?: string
  nombreExterno?: string
  razaExternaId?: string
  registroGenealogico?: string
  fechaRegistro: string
}

export interface CrearParentescoInput {
  tipo: TipoParentesco
  animalPadreId?: string
  nombreExterno?: string
  razaExternaId?: string
  registroGenealogico?: string
}

export interface AnimalFilters {
  search?: string
  estado?: AnimalState | ''
  propiedadId?: string
  potreroId?: string
  loteId?: string
  categoria?: string
  sexo?: 'MACHO' | 'HEMBRA' | ''
  page: number
  size: number
}
