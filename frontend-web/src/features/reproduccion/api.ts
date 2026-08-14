import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export type SexoAnimal = 'MACHO' | 'HEMBRA'
export type TipoCelo = 'VISUAL' | 'TORO_MARCADOR' | 'PODOMETRO' | 'SENSOR' | 'OTRO'
export type IntensidadCelo = 'BAJA' | 'MEDIA' | 'ALTA'
export type EstadoRegistroReproduccion = 'ACTIVO' | 'ANULADO'
export type TipoServicio = 'MONTA_NATURAL' | 'INSEMINACION_ARTIFICIAL' | 'TRANSFERENCIA_EMBRIONARIA'
export type EstadoServicio = 'REGISTRADO' | 'PENDIENTE_DIAGNOSTICO' | 'GESTACION_CONFIRMADA' | 'NO_PRENADA' | 'FINALIZADO' | 'ANULADO'
export type ResultadoGestacion = 'POSITIVO' | 'NEGATIVO' | 'DUDOSO' | 'PERDIDA_GESTACION'
export type MetodoDiagnostico = 'PALPACION' | 'ECOGRAFIA' | 'SANGRE' | 'OTRO'
export type TipoParto = 'NORMAL' | 'PREMATURO' | 'DISTOCICO' | 'CESAREA' | 'OTRO'
export type DificultadParto = 'SIN_ASISTENCIA' | 'ASISTENCIA_LEVE' | 'ASISTENCIA_MODERADA' | 'ASISTENCIA_DIFICIL' | 'CESAREA'
export type EstadoNacimiento = 'VIVO' | 'MUERTO' | 'NATIMUERTO'
export type TipoDestete = 'NORMAL' | 'PRECOZ' | 'TEMPORAL' | 'FORZADO' | 'OTRO'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CeloResponse {
  id: string
  animalId: string
  codigoAnimal: string
  nombreAnimal?: string
  fechaDeteccion: string
  tipoDeteccion: TipoCelo
  intensidad?: IntensidadCelo
  observaciones?: string
  propiedadId?: string
  propiedadNombre?: string
  potreroId?: string
  potreroNombre?: string
  loteId?: string
  clienteUuid?: string
  estado: EstadoRegistroReproduccion
  version: number
}

export interface ServicioResponse {
  id: string
  hembraId: string
  codigoAnimal: string
  nombreAnimal?: string
  celoId?: string
  fechaServicio: string
  tipoServicio: TipoServicio
  machoId?: string
  codigoMacho?: string
  nombreMacho?: string
  codigoSemen?: string
  proveedorSemen?: string
  tecnicoId?: string
  numeroIntento: number
  fechaDiagnosticoRecomendada?: string
  observaciones?: string
  propiedadId?: string
  propiedadNombre?: string
  potreroId?: string
  potreroNombre?: string
  loteId?: string
  clienteUuid?: string
  estado: EstadoServicio
  version: number
}

export interface DiagnosticoGestacionResponse {
  id: string
  animalId: string
  codigoAnimal: string
  nombreAnimal?: string
  servicioId?: string
  fechaDiagnostico: string
  resultado: ResultadoGestacion
  metodo?: MetodoDiagnostico
  diasGestacionEstimados?: number
  fechaProbableParto?: string
  veterinarioId?: string
  observaciones?: string
  propiedadId?: string
  propiedadNombre?: string
  potreroId?: string
  potreroNombre?: string
  loteId?: string
  clienteUuid?: string
  estado: EstadoRegistroReproduccion
  version: number
}

export interface Parto {
  id: string
  empresaId: string
  madreId: string
  diagnosticoGestacionId?: string
  servicioId?: string
  fechaParto: string
  tipoParto: TipoParto
  dificultad: DificultadParto
  asistido: boolean
  responsableId?: string
  resultadoMadre?: string
  numeroCrias: number
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid?: string
  idempotencyKey?: string
  estado: EstadoRegistroReproduccion
  codigoMadre: string
  nombreMadre?: string
  machoId?: string
  codigoMacho?: string
  nombreMacho?: string
  potreroNombre?: string
  propiedadNombre?: string
  version: number
}

export interface CriaParto {
  id: string
  empresaId: string
  partoId: string
  animalCriaId?: string
  sexo: SexoAnimal
  pesoNacimientoKg: number
  estadoNacimiento: EstadoNacimiento
  horaNacimiento?: string
  observaciones?: string
  clienteUuid?: string
  idempotencyKey?: string
  codigoAnimal?: string
  nombreAnimal?: string
  version: number
}

export interface PartoResult {
  parto: Parto
  crias: CriaParto[]
}

export interface Aborto {
  id: string
  empresaId: string
  animalId: string
  gestacionId?: string
  servicioId?: string
  fechaEvento: string
  edadGestacionalEstimada?: number
  causa?: string
  diagnostico?: string
  veterinarioId?: string
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid?: string
  idempotencyKey?: string
  estado: EstadoRegistroReproduccion
  codigoAnimal: string
  nombreAnimal?: string
  potreroNombre?: string
  propiedadNombre?: string
  version: number
}

export interface Destete {
  id: string
  empresaId: string
  animalCriaId: string
  madreId: string
  fechaDestete: string
  pesoDesteteKg: number
  tipoDestete: TipoDestete
  motivo?: string
  responsableId?: string
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid?: string
  idempotencyKey?: string
  estado: EstadoRegistroReproduccion
  codigoAnimal: string
  nombreAnimal?: string
  potreroNombre?: string
  propiedadNombre?: string
  version: number
}

export interface CriaInput {
  sexo: SexoAnimal
  pesoNacimientoKg: number
  estadoNacimiento: EstadoNacimiento
  horaNacimiento?: string
  observaciones?: string
  crearAnimal: boolean
  codigoAnimal?: string
  nombreAnimal?: string
  potreroInicialId?: string
}

export interface RegistrarPartoInput {
  madreId: string
  diagnosticoGestacionId?: string
  servicioId?: string
  fechaParto: string
  tipoParto: TipoParto
  dificultad: DificultadParto
  asistido: boolean
  responsableId?: string
  resultadoMadre?: string
  observaciones?: string
  crias: CriaInput[]
}

export interface RegistrarAbortoInput {
  animalId: string
  gestacionId?: string
  servicioId?: string
  fechaEvento: string
  edadGestacionalEstimada?: number
  causa?: string
  diagnostico?: string
  veterinarioId?: string
  observaciones?: string
}

export interface RegistrarDesteteInput {
  animalCriaId: string
  madreId: string
  fechaDestete: string
  pesoDesteteKg: number
  tipoDestete: TipoDestete
  motivo?: string
  responsableId?: string
  observaciones?: string
}

export interface RegistrarCeloInput {
  animalId: string
  fechaDeteccion: string
  tipoDeteccion: TipoCelo
  intensidad?: IntensidadCelo
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid: string
}

export interface RegistrarServicioInput {
  hembraId: string
  celoId?: string
  fechaServicio: string
  tipoServicio: TipoServicio
  machoId?: string
  codigoSemen?: string
  proveedorSemen?: string
  tecnicoId?: string
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid: string
}

export interface RegistrarDiagnosticoInput {
  animalId: string
  servicioId?: string
  fechaDiagnostico: string
  resultado: ResultadoGestacion
  metodo?: MetodoDiagnostico
  diasGestacionEstimados?: number
  veterinarioId?: string
  observaciones?: string
  propiedadId?: string
  potreroId?: string
  loteId?: string
  clienteUuid: string
}

export const TIPO_CELO_LABELS: Record<TipoCelo, string> = {
  VISUAL: 'Visual',
  TORO_MARCADOR: 'Toro marcador',
  PODOMETRO: 'Podómetro',
  SENSOR: 'Sensor',
  OTRO: 'Otro',
}

export const INTENSIDAD_CELO_LABELS: Record<IntensidadCelo, string> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
}

export const ESTADO_REGISTRO_LABELS: Record<EstadoRegistroReproduccion, string> = {
  ACTIVO: 'Activo',
  ANULADO: 'Anulado',
}

export const TIPO_SERVICIO_LABELS: Record<TipoServicio, string> = {
  MONTA_NATURAL: 'Monta natural',
  INSEMINACION_ARTIFICIAL: 'Inseminación artificial',
  TRANSFERENCIA_EMBRIONARIA: 'Transferencia embrionaria',
}

export const ESTADO_SERVICIO_LABELS: Record<EstadoServicio, string> = {
  REGISTRADO: 'Registrado',
  PENDIENTE_DIAGNOSTICO: 'Pendiente diagnóstico',
  GESTACION_CONFIRMADA: 'Gestación confirmada',
  NO_PRENADA: 'No preñada',
  FINALIZADO: 'Finalizado',
  ANULADO: 'Anulado',
}

export const RESULTADO_GESTACION_LABELS: Record<ResultadoGestacion, string> = {
  POSITIVO: 'Positivo',
  NEGATIVO: 'Negativo',
  DUDOSO: 'Dudoso',
  PERDIDA_GESTACION: 'Pérdida de gestación',
}

export const METODO_DIAGNOSTICO_LABELS: Record<MetodoDiagnostico, string> = {
  PALPACION: 'Palpación',
  ECOGRAFIA: 'Ecografía',
  SANGRE: 'Análisis de sangre',
  OTRO: 'Otro',
}

export const TIPO_PARTO_LABELS: Record<TipoParto, string> = {
  NORMAL: 'Normal',
  PREMATURO: 'Prematuro',
  DISTOCICO: 'Distócico',
  CESAREA: 'Cesárea',
  OTRO: 'Otro',
}

export const DIFICULTAD_PARTO_LABELS: Record<DificultadParto, string> = {
  SIN_ASISTENCIA: 'Sin asistencia',
  ASISTENCIA_LEVE: 'Asistencia leve',
  ASISTENCIA_MODERADA: 'Asistencia moderada',
  ASISTENCIA_DIFICIL: 'Asistencia difícil',
  CESAREA: 'Cesárea',
}

export const ESTADO_NACIMIENTO_LABELS: Record<EstadoNacimiento, string> = {
  VIVO: 'Vivo',
  MUERTO: 'Muerto',
  NATIMUERTO: 'Natimuerto',
}

export const TIPO_DESTETE_LABELS: Record<TipoDestete, string> = {
  NORMAL: 'Normal',
  PRECOZ: 'Precoz',
  TEMPORAL: 'Temporal',
  FORZADO: 'Forzado',
  OTRO: 'Otro',
}

export function toIso(value: string) {
  return new Date(value).toISOString()
}

export async function listPartos(params?: { animalId?: string; propiedadId?: string; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<Parto>>>('/api/v1/reproduccion/partos', { params })).data.data
}

export async function registrarParto(input: RegistrarPartoInput) {
  return (await http.post<ApiResponse<PartoResult>>('/api/v1/reproduccion/partos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listAbortos(params?: { animalId?: string; propiedadId?: string; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<Aborto>>>('/api/v1/reproduccion/abortos', { params })).data.data
}

export async function registrarAborto(input: RegistrarAbortoInput) {
  return (await http.post<ApiResponse<Aborto>>('/api/v1/reproduccion/abortos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listDestetes(params?: { animalId?: string; propiedadId?: string; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<Destete>>>('/api/v1/reproduccion/destetes', { params })).data.data
}

export async function registrarDestete(input: RegistrarDesteteInput) {
  return (await http.post<ApiResponse<Destete>>('/api/v1/reproduccion/destetes', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listCelos(params?: { animalId?: string; propiedadId?: string; estado?: EstadoRegistroReproduccion; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<CeloResponse>>>('/api/v1/reproduccion/celos', { params })).data.data
}

export async function registrarCelo(input: RegistrarCeloInput) {
  return (await http.post<ApiResponse<CeloResponse>>('/api/v1/reproduccion/celos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function anularCelo(id: string, input: { motivo: string; version: number }) {
  return (await http.post<ApiResponse<CeloResponse>>(`/api/v1/reproduccion/celos/${id}/anular`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listServicios(params?: { animalId?: string; propiedadId?: string; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<ServicioResponse>>>('/api/v1/reproduccion/servicios', { params })).data.data
}

export async function registrarServicio(input: RegistrarServicioInput) {
  return (await http.post<ApiResponse<ServicioResponse>>('/api/v1/reproduccion/servicios', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listDiagnosticos(params?: { animalId?: string; propiedadId?: string; page?: number; size?: number }) {
  return (await http.get<ApiResponse<PageResponse<DiagnosticoGestacionResponse>>>('/api/v1/reproduccion/diagnosticos', { params })).data.data
}

export async function registrarDiagnostico(input: RegistrarDiagnosticoInput) {
  return (await http.post<ApiResponse<DiagnosticoGestacionResponse>>('/api/v1/reproduccion/diagnosticos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export function estadoServicioBadge(estado: EstadoServicio) {
  switch (estado) {
    case 'GESTACION_CONFIRMADA': return 'confirmed'
    case 'FINALIZADO': return 'valid'
    case 'ANULADO': return 'annulled'
    case 'NO_PRENADA': return 'danger'
    case 'PENDIENTE_DIAGNOSTICO': return 'warning'
    default: return 'pending'
  }
}

export function estadoRegistroBadge(estado: EstadoRegistroReproduccion) {
  return estado === 'ACTIVO' ? 'confirmed' : 'annulled'
}
