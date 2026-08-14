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
  pesoNacimientoKg?: number
  condicionCorporalActual?: number
  fotoPrincipalPath?: string
  observaciones?: string
  version: number
}

export type AnimalState = 'ACTIVO' | 'VENDIDO' | 'MUERTO' | 'PERDIDO' | 'TRANSFERIDO' | 'DESCARTADO'

export interface Raza { id: string; codigo: string; nombre: string; especie: string }
export interface CategoriaAnimal { id: string; codigo: string; nombre: string; sexoAplicable: 'MACHO' | 'HEMBRA' | 'AMBOS' }

export interface CreateAnimalInput {
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
}

export interface UpdateAnimalInput extends Omit<CreateAnimalInput, 'origen'> {
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
  categoria?: string
  sexo?: 'MACHO' | 'HEMBRA' | ''
  page: number
  size: number
}
