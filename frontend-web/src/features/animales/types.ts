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
  codigo: string
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
  tipo: 'NACIMIENTO' | 'COMPRA' | 'INGRESO' | 'CAMBIO_ESTADO' | 'ACTUALIZACION'
  fechaEvento: string
  estadoAnterior?: AnimalState
  estadoNuevo?: AnimalState
  motivo?: string
  registradoPor: string
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
