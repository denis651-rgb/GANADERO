import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export type TipoActividad = 'VACUNACION' | 'DESPARASITACION' | 'VITAMINIZACION' | 'CONTROL' | 'PRUEBA_DIAGNOSTICA' | 'OTRO'
export type EstadoPlan = 'BORRADOR' | 'ACTIVO' | 'FINALIZADO' | 'ANULADO'
export type EstadoJornada = 'BORRADOR' | 'EN_PROCESO' | 'CONFIRMADA' | 'ANULADA'
export type EstadoCaso = 'ABIERTO' | 'EN_OBSERVACION' | 'EN_TRATAMIENTO' | 'CERRADO' | 'ANULADO'
export type SeveridadCaso = 'LEVE' | 'MODERADA' | 'GRAVE' | 'CRITICA'
export type EstadoTratamiento = 'BORRADOR' | 'ACTIVO' | 'FINALIZADO' | 'SUSPENDIDO' | 'ANULADO'
export type EstadoAplicacion = 'PENDIENTE' | 'APLICADA' | 'OMITIDA' | 'ATRASADA' | 'CANCELADA'
export type SexoAplicable = 'MACHO' | 'HEMBRA' | 'AMBOS'

export interface Enfermedad {
  id: string
  empresaId: string
  codigo: string
  nombre: string
  descripcion?: string
  esNotificable: boolean
  activo: boolean
  createdAt: string
  updatedAt: string
}

export interface PlanSanitario {
  id: string
  empresaId: string
  nombre: string
  descripcion?: string
  fechaInicio: string
  fechaFin?: string
  estado: EstadoPlan
  createdAt: string
  updatedAt: string
  version: number
}

export interface PlanSanitarioItem {
  id: string
  empresaId: string
  planId: string
  tipoActividad: TipoActividad
  productoId?: string
  productoRecomendadoTexto?: string
  categoriaAnimalId?: string
  sexoAplicable?: SexoAplicable
  edadMinDias?: number
  edadMaxDias?: number
  dosis?: number
  unidadDosis?: string
  frecuenciaDias?: number
  diasAlerta: number
  viaAdministracion?: string
  obligatorio: boolean
  activo: boolean
  version: number
}

export interface ProximaActividad {
  proximaAplicacion: string
  fechaAlerta: string
}

export interface JornadaSanitaria {
  id: string
  empresaId: string
  tipoJornada: TipoActividad
  fechaInicio: string
  fechaFin?: string
  propiedadId: string
  potreroId?: string
  loteGanaderoId?: string
  responsableId: string
  veterinarioId?: string
  estado: EstadoJornada
  observaciones?: string
  operationId?: string
  version: number
}

export interface CasoClinico {
  id: string
  empresaId: string
  animalId: string
  fechaInicio: string
  sintomas: string
  enfermedadId?: string
  diagnosticoTexto?: string
  severidad: SeveridadCaso
  estado: EstadoCaso
  veterinarioId?: string
  fechaCierre?: string
  resultado?: string
  observaciones?: string
  version: number
}

export interface Tratamiento {
  id: string
  empresaId: string
  casoClinicoId?: string
  animalId: string
  fechaInicio: string
  fechaFinEstimada: string
  fechaFinReal?: string
  diagnostico?: string
  veterinarioId?: string
  estado: EstadoTratamiento
  observaciones?: string
  version: number
}

export interface AplicacionTratamiento {
  id: string
  empresaId: string
  tratamientoDetalleId: string
  fechaProgramada: string
  fechaAplicada?: string
  dosisProgramada: number
  dosisAplicada?: number
  aplicadoPor?: string
  estado: EstadoAplicacion
  observaciones?: string
  version: number
}

export interface AplicacionSanitaria {
  id: string
  empresaId: string
  jornadaId: string
  planItemId?: string
  animalId: string
  productoId?: string
  loteProductoId?: string
  dosis?: number
  unidadDosis?: string
  viaAdministracion?: string
  fechaAplicacion: string
  proximaAplicacion?: string
  retiroCarneHasta?: string
  retiroLecheHasta?: string
  aplicadoPor?: string
  resultado?: string
  observaciones?: string
  idempotencyKey: string
  estado: string
  version: number
}

export interface ConfirmacionJornadaResult {
  jornada: JornadaSanitaria
  aplicaciones: AplicacionSanitaria[]
  totalProcesado: number
}

export interface AnimalElegibilidad {
  id: string
  codigo: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  estado: string
  edadDias?: number | null
  elegible: boolean
  motivos: string[]
}

export interface ResultadoElegibilidad {
  elegibles: AnimalElegibilidad[]
  noElegibles: AnimalElegibilidad[]
}

export interface CrearPlanInput {
  nombre: string
  descripcion?: string
  fechaInicio: string
  fechaFin?: string
}

export interface CrearItemInput {
  tipoActividad: TipoActividad
  productoId?: string
  productoRecomendadoTexto?: string
  categoriaAnimalId?: string
  sexoAplicable?: SexoAplicable
  edadMinDias?: number
  edadMaxDias?: number
  dosis?: number
  unidadDosis?: string
  frecuenciaDias?: number
  diasAlerta: number
  viaAdministracion?: string
  obligatorio: boolean
}

export interface CrearJornadaInput {
  tipoJornada: TipoActividad
  fechaInicio: string
  propiedadId: string
  potreroId?: string
  loteGanaderoId?: string
  responsableId: string
  veterinarioId?: string
  observaciones?: string
}

export interface ConfirmarJornadaInput {
  operationId: string
  version: number
  planItemId?: string
  productoId?: string
  loteProductoId?: string
  dosis?: number
  unidadDosis?: string
  viaAdministracion?: string
  fechaAplicacion: string
  resultado?: string
  observaciones?: string
}

export interface CrearCasoInput {
  animalId: string
  fechaInicio: string
  sintomas: string
  enfermedadId?: string
  diagnosticoTexto?: string
  severidad: SeveridadCaso
  veterinarioId?: string
  observaciones?: string
}

export interface DetalleTratamientoInput {
  productoId?: string
  loteProductoId?: string
  dosis: number
  unidadDosis: string
  frecuenciaHoras: number
  duracionDias: number
  viaAdministracion?: string
  retiroCarneDias: number
  retiroLecheDias: number
}

export interface CrearTratamientoInput {
  casoClinicoId?: string
  animalId: string
  fechaInicio: string
  fechaFinEstimada: string
  diagnostico?: string
  veterinarioId?: string
  observaciones?: string
  detalles: DetalleTratamientoInput[]
}

export const TIPO_ACTIVIDAD_LABELS: Record<TipoActividad, string> = {
  VACUNACION: 'Vacunación',
  DESPARASITACION: 'Desparasitación',
  VITAMINIZACION: 'Vitaminización',
  CONTROL: 'Control',
  PRUEBA_DIAGNOSTICA: 'Prueba diagnóstica',
  OTRO: 'Otro',
}

export const ESTADO_PLAN_LABELS: Record<EstadoPlan, string> = {
  BORRADOR: 'Borrador',
  ACTIVO: 'Activo',
  FINALIZADO: 'Finalizado',
  ANULADO: 'Anulado',
}

export const ESTADO_JORNADA_LABELS: Record<EstadoJornada, string> = {
  BORRADOR: 'Borrador',
  EN_PROCESO: 'En proceso',
  CONFIRMADA: 'Confirmada',
  ANULADA: 'Anulada',
}

export const ESTADO_CASO_LABELS: Record<EstadoCaso, string> = {
  ABIERTO: 'Abierto',
  EN_OBSERVACION: 'En observación',
  EN_TRATAMIENTO: 'En tratamiento',
  CERRADO: 'Cerrado',
  ANULADO: 'Anulado',
}

export const SEVERIDAD_LABELS: Record<SeveridadCaso, string> = {
  LEVE: 'Leve',
  MODERADA: 'Moderada',
  GRAVE: 'Grave',
  CRITICA: 'Crítica',
}

export const ESTADO_TRATAMIENTO_LABELS: Record<EstadoTratamiento, string> = {
  BORRADOR: 'Borrador',
  ACTIVO: 'Activo',
  FINALIZADO: 'Finalizado',
  SUSPENDIDO: 'Suspendido',
  ANULADO: 'Anulado',
}

export const ESTADO_APLICACION_LABELS: Record<EstadoAplicacion, string> = {
  PENDIENTE: 'Pendiente',
  APLICADA: 'Aplicada',
  OMITIDA: 'Omitida',
  ATRASADA: 'Atrasada',
  CANCELADA: 'Cancelada',
}

export async function listEnfermedades(incluirInactivas = false) {
  return (await http.get<ApiResponse<Enfermedad[]>>('/api/v1/sanidad/enfermedades', { params: { incluirInactivas } })).data.data
}

export async function crearEnfermedad(input: { codigo: string; nombre: string; descripcion?: string; esNotificable: boolean }) {
  return (await http.post<ApiResponse<Enfermedad>>('/api/v1/sanidad/enfermedades', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cambiarEstadoEnfermedad(id: string, activo: boolean) {
  return (await http.patch<ApiResponse<Enfermedad>>(`/api/v1/sanidad/enfermedades/${id}/activo`, { activo })).data.data
}

export async function listPlanes() {
  return (await http.get<ApiResponse<PlanSanitario[]>>('/api/v1/sanidad/planes')).data.data
}

export async function crearPlan(input: CrearPlanInput) {
  return (await http.post<ApiResponse<PlanSanitario>>('/api/v1/sanidad/planes', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cambiarEstadoPlan(id: string, estado: EstadoPlan, version: number) {
  return (await http.patch<ApiResponse<PlanSanitario>>(`/api/v1/sanidad/planes/${id}/estado`, { estado, version })).data.data
}

export async function listPlanItems(planId: string, incluirInactivos = false) {
  return (await http.get<ApiResponse<PlanSanitarioItem[]>>(`/api/v1/sanidad/planes/${planId}/items`, { params: { incluirInactivos } })).data.data
}

export async function crearPlanItem(planId: string, input: CrearItemInput) {
  return (await http.post<ApiResponse<PlanSanitarioItem>>(`/api/v1/sanidad/planes/${planId}/items`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cambiarEstadoItem(planId: string, itemId: string, activo: boolean, version: number) {
  return (await http.patch<ApiResponse<PlanSanitarioItem>>(`/api/v1/sanidad/planes/${planId}/items/${itemId}/activo`, { activo, version })).data.data
}

export async function calcularProxima(planId: string, itemId: string, fechaAplicacion: string) {
  return (await http.get<ApiResponse<ProximaActividad>>(`/api/v1/sanidad/planes/${planId}/items/${itemId}/proxima`, { params: { fechaAplicacion } })).data.data
}

export async function listJornadas() {
  return (await http.get<ApiResponse<JornadaSanitaria[]>>('/api/v1/jornadas-sanitarias')).data.data
}

export async function crearJornada(input: CrearJornadaInput) {
  return (await http.post<ApiResponse<JornadaSanitaria>>('/api/v1/jornadas-sanitarias', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listAnimalesElegibles(params: { propiedadId: string; loteId?: string; categoriaId?: string; sexo?: string }) {
  return (await http.get<ApiResponse<Array<{ id: string; codigo: string; nombre?: string; sexo: string; estado: string }>>>('/api/v1/jornadas-sanitarias/animales-elegibles', { params })).data.data
}

export async function obtenerElegibilidadJornada(jornadaId: string, planItemId: string, fechaAplicacion: string) {
  return (await http.get<ApiResponse<ResultadoElegibilidad>>(`/api/v1/jornadas-sanitarias/${jornadaId}/elegibilidad`, {
    params: { planItemId, fechaAplicacion },
  })).data.data
}

export async function seleccionarAnimales(jornadaId: string, input: { planItemId: string; fechaAplicacion: string; animalIds: string[] }) {
  return (await http.put<ApiResponse<string[]>>(`/api/v1/jornadas-sanitarias/${jornadaId}/animales`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function confirmarJornada(jornadaId: string, input: ConfirmarJornadaInput) {
  return (await http.post<ApiResponse<ConfirmacionJornadaResult>>(`/api/v1/jornadas-sanitarias/${jornadaId}/confirmar`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listCasos(animalId?: string) {
  return (await http.get<ApiResponse<CasoClinico[]>>('/api/v1/sanidad/casos-clinicos', { params: { animalId } })).data.data
}

export async function crearCaso(input: CrearCasoInput) {
  return (await http.post<ApiResponse<CasoClinico>>('/api/v1/sanidad/casos-clinicos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function cerrarCaso(id: string, resultado: string) {
  return (await http.post<ApiResponse<CasoClinico>>(`/api/v1/sanidad/casos-clinicos/${id}/cerrar`, { resultado }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listTratamientos(animalId?: string) {
  return (await http.get<ApiResponse<Tratamiento[]>>('/api/v1/sanidad/tratamientos', { params: { animalId } })).data.data
}

export async function crearTratamiento(input: CrearTratamientoInput) {
  return (await http.post<ApiResponse<Tratamiento>>('/api/v1/sanidad/tratamientos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function activarTratamiento(id: string) {
  return (await http.post<ApiResponse<AplicacionTratamiento[]>>(`/api/v1/sanidad/tratamientos/${id}/activar`, undefined, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function regenerarTratamiento(id: string) {
  return (await http.post<ApiResponse<AplicacionTratamiento[]>>(`/api/v1/sanidad/tratamientos/${id}/regenerar`, undefined, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listAplicaciones(tratamientoId: string) {
  return (await http.get<ApiResponse<AplicacionTratamiento[]>>(`/api/v1/sanidad/tratamientos/${tratamientoId}/aplicaciones`)).data.data
}

export async function aplicarTratamiento(tratamientoId: string, aplicacionId: string, input: { dosisAplicada: number; observaciones?: string; version: number }) {
  return (await http.post<ApiResponse<AplicacionTratamiento>>(`/api/v1/sanidad/tratamientos/${tratamientoId}/aplicaciones/${aplicacionId}/aplicar`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function marcarAtrasadas() {
  return (await http.post<ApiResponse<AplicacionTratamiento[]>>('/api/v1/sanidad/tratamientos/aplicaciones/marcar-atrasadas', undefined, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function finalizarTratamiento(id: string) {
  return (await http.post<ApiResponse<Tratamiento>>(`/api/v1/sanidad/tratamientos/${id}/finalizar`, undefined, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}
