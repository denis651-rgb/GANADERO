import { http } from '@/shared/api/http'
import type { ApiResponse } from '@/shared/api/types'

export interface ConfiguracionSanitaria {
  edadMinMachoMeses: number | null
  edadMinHembraMeses: number | null
  version: number
}
export async function getConfiguracionSanitaria() {
  return (await http.get<ApiResponse<ConfiguracionSanitaria>>('/api/v1/sanidad/configuracion')).data.data
}
export async function guardarConfiguracionSanitaria(input: ConfiguracionSanitaria) {
  return (await http.put<ApiResponse<ConfiguracionSanitaria>>('/api/v1/sanidad/configuracion', input)).data.data
}

export type TipoActividad = 'VACUNACION' | 'DESPARASITACION' | 'VITAMINIZACION' | 'PRUEBA_DIAGNOSTICA' | 'CONTROL_ECTOPARASITARIO' | 'VIGILANCIA' | 'TRATAMIENTO_PREVENTIVO' | 'OTRA'
export type OrigenRegulatorio = 'OBLIGATORIO_SENASAG' | 'CAMPANA_RIESGO' | 'RECOMENDADO_VETERINARIO' | 'CONFIGURABLE_ESTABLECIMIENTO'

/** Cómo se genera el calendario y quién resulta elegible para una actividad. */
export type ModalidadActividad = 'POR_EDAD' | 'PERIODICA' | 'FECHA_PROGRAMADA' | 'POR_HALLAZGO' | 'MANUAL'
export type UnidadEdadActividad = 'DIAS' | 'MESES' | 'ANIOS'
export type UnidadFrecuencia = 'DIAS' | 'SEMANAS' | 'MESES' | 'ANIOS'
export type PoliticaEdadEstimada = 'PERMITIR' | 'EXCLUIR'
export type PoliticaEdadDesconocida = 'EXCLUIR' | 'INCLUIR_MANUAL'
export type ReferenciaCalculoPeriodica = 'FECHA_DE_INGRESO' | 'FECHA_DE_NACIMIENTO' | 'ULTIMA_APLICACION' | 'FECHA_INICIAL_DEL_PLAN' | 'FECHA_CONFIGURADA'
export type TipoCalculoDosis = 'FIJA_POR_ANIMAL' | 'POR_PESO' | 'SEGUN_INDICACION' | 'NO_APLICA'
export type UnidadDosis = 'ML' | 'MG' | 'G' | 'TABLETA' | 'DOSIS' | 'GOTA' | 'APLICACION' | 'ML_POR_KG' | 'ML_POR_10KG' | 'ML_POR_50KG' | 'MG_POR_KG' | 'OTRA'
export type ViaAdministracion = 'SUBCUTANEA' | 'INTRAMUSCULAR' | 'INTRAVENOSA' | 'ORAL' | 'TOPICA' | 'POUR_ON' | 'INTRANASAL' | 'OTRA' | 'NO_APLICA'
export type LugarAplicacion = 'CUELLO' | 'TABLA_DEL_CUELLO' | 'REGION_ESCAPULAR' | 'LOMO' | 'LINEA_DORSAL' | 'BOCA' | 'FOSA_NASAL' | 'TODO_EL_CUERPO' | 'OTRO' | 'NO_APLICA'
export type EstadoEventoCalendario = 'PROYECTADO' | 'PROGRAMADO' | 'EN_PREPARACION' | 'REALIZADO' | 'VENCIDO' | 'OMITIDO' | 'CANCELADO'
export type EstadoAplicacionSanitaria = 'APLICADO' | 'NO_APLICADO' | 'APLICADO_PARCIAL' | 'RECHAZADO' | 'POSPUESTO' | 'ANULADO'

/**
 * Forma de `modalidadConfig` según la modalidad. El backend NO incluye un discriminador dentro
 * del propio JSON (el tipo concreto se decide por el campo hermano `modalidad`, igual que en el
 * dominio Java) — al leerlo, castea según `item.modalidad`, no busques una propiedad "tipo" aquí.
 */
export interface PorEdadConfig { edadObjetivoValor: number; edadUnidad: UnidadEdadActividad; ventanaAnticipadaDias: number; ventanaPosteriorDias: number; politicaEdadEstimada: PoliticaEdadEstimada; politicaEdadDesconocida: PoliticaEdadDesconocida; unaVezEnLaVida: boolean }
export interface PeriodicaConfig { frecuenciaValor: number; frecuenciaUnidad: UnidadFrecuencia; referenciaCalculo: ReferenciaCalculoPeriodica; toleranciaAnticipadaDias: number; toleranciaPosteriorDias: number }
export interface FechaProgramadaConfig { fechaProgramada: string; unicaVez: boolean; reglaRepeticion?: string; zonaHoraria?: string; ventanaEjecucionHoras?: number }
export interface PorHallazgoConfig { tiposHallazgo: string[]; severidadMinima?: string; accionRecomendada?: string; plazoDias?: number; requiereValidacionVeterinaria: boolean }
export type ManualConfig = Record<string, never>
export type ModalidadConfig = PorEdadConfig | PeriodicaConfig | FechaProgramadaConfig | PorHallazgoConfig | ManualConfig
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
  identidadLogicaId: string
  numeroVersion: number
  versionAnteriorId?: string
  vigenteDesde: string
  vigenteHasta?: string
  motivoVersion?: string
  codigoInterno?: string
  nombre: string
  descripcion?: string
  tipoActividad: TipoActividad
  modalidad: ModalidadActividad
  modalidadConfig: ModalidadConfig
  productoId?: string
  productoRecomendadoTexto?: string
  principioActivo?: string
  instruccionesVeterinario?: string
  observaciones?: string
  categoriaAnimalId?: string
  sexoAplicable?: SexoAplicable
  edadMinDias?: number
  edadMaxDias?: number
  edadUnidad: UnidadEdadActividad
  dosis?: number
  unidadDosis?: string
  dosisCantidad?: number
  dosisUnidad?: UnidadDosis
  dosisUnidadDetalle?: string
  dosisTipoCalculo: TipoCalculoDosis
  dosisPesoReferenciaKg?: number
  dosisMinima?: number
  dosisMaxima?: number
  frecuenciaDias?: number
  diasAlerta: number
  viaAdministracion?: string
  viaAdministracionCodigo?: ViaAdministracion
  viaAdministracionDetalle?: string
  lugarAplicacion?: LugarAplicacion
  lugarAplicacionDetalle?: string
  categoriasAplicables: string[]
  obligatorio: boolean
  origenRegulatorio: OrigenRegulatorio
  especieAplicable: string
  permiteEdadDesconocida: boolean
  requiereRevision: boolean
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
  dosisRecomendada?: number
  dosisAplicada?: number
  pesoUtilizadoKg?: number
  pesoTipo?: 'MEDIDO' | 'ESTIMADO'
  pesoFecha?: string
  productoAplicadoTexto?: string
  motivoCambioProducto?: string
  motivoAjusteDosis?: string
  viaAdministracion?: string
  lugarAplicacion?: LugarAplicacion
  versionActividadId?: string
  instruccionesAplicadasTexto?: string
  eventoCalendarioId?: string
  fechaAplicacion: string
  proximaAplicacion?: string
  retiroCarneHasta?: string
  retiroLecheHasta?: string
  aplicadoPor?: string
  resultado?: string
  observaciones?: string
  idempotencyKey: string
  estado: EstadoAplicacionSanitaria
  version: number
}

export interface EventoCalendarioSanitario {
  id: string
  actividadId: string
  animalId: string
  cicloClave: string
  fechaPrevista: string
  ventanaDesde?: string
  ventanaHasta?: string
  estado: EstadoEventoCalendario
  origenModalidad: ModalidadActividad
  hallazgoOrigenTipo?: string
  hallazgoOrigenId?: string
  jornadaId?: string
  prioridad: string
  createdAt: string
  version: number
}

/** Historial declarado al ingreso (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, secciones 3.2 y 7). */
export type OrigenRegistroAplicacion = 'APLICADA_FINCA' | 'DECLARADA_PROVEEDOR'

export interface AplicacionDeclarada {
  id: string
  animalId: string
  planItemId?: string
  fechaAplicacion: string
  proximaAplicacion?: string
  dosis?: number
  unidadDosis?: string
  observaciones?: string
  origenRegistro: OrigenRegistroAplicacion
  estado: string
  version: number
}

export interface RegistrarAplicacionDeclaradaInput {
  animalId: string
  tipoActividad: TipoActividad
  planItemId?: string
  fechaAplicacion: string
  dosis?: number
  unidadDosis?: string
  productoTexto?: string
  observaciones?: string
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
  edadEstimada: boolean
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
  codigoInterno?: string
  nombre: string
  descripcion?: string
  tipoActividad: TipoActividad
  modalidad: ModalidadActividad
  modalidadConfig: ModalidadConfig
  productoRecomendadoTexto?: string
  principioActivo?: string
  instruccionesVeterinario?: string
  observaciones?: string
  dosisCantidad?: number
  dosisUnidad?: UnidadDosis
  dosisUnidadDetalle?: string
  dosisTipoCalculo: TipoCalculoDosis
  dosisPesoReferenciaKg?: number
  dosisMinima?: number
  dosisMaxima?: number
  viaAdministracionCodigo?: ViaAdministracion
  viaAdministracionDetalle?: string
  lugarAplicacion?: LugarAplicacion
  lugarAplicacionDetalle?: string
  categoriasAplicables?: string[]
  sexoAplicable?: SexoAplicable
  edadMinDias?: number
  edadMaxDias?: number
  edadUnidad?: UnidadEdadActividad
  permiteEdadDesconocida?: boolean
  diasAlerta: number
  obligatorio: boolean
  origenRegulatorio: OrigenRegulatorio
  especieAplicable?: string
  motivoVersion?: string
  fechaVigencia?: string
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

export interface ActualizarJornadaInput extends CrearJornadaInput {
  version: number
}

export interface AnularJornadaInput {
  motivo: string
  version: number
}

export interface ConfirmarJornadaInput {
  operationId: string
  version: number
  planItemId: string
  dosisAplicada?: number
  motivoAjusteDosis?: string
  unidadDosis?: string
  productoAplicadoTexto?: string
  motivoCambioProducto?: string
  viaAdministracion?: string
  lugarAplicacion?: LugarAplicacion
  fechaAplicacion: string
  resultado?: string
  observaciones?: string
  retiroCarneDias?: number
  retiroLecheDias?: number
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

/** Control neonatal (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, sección 4.c): checklist de terneros recién nacidos, independiente del plan sanitario activo. */
export type MomentoControlNeonatal = 'DIA_0' | 'PRIMERA_SEMANA'
export type EstadoCalostrado = 'CORRECTO' | 'INSUFICIENTE' | 'DESCONOCIDO' | 'NO_APLICA'

export interface ControlNeonatal {
  id: string
  empresaId: string
  animalId: string
  fechaControl: string
  momento: MomentoControlNeonatal
  calostrado: EstadoCalostrado
  ombligoDesinfectado: boolean
  ombligoEstado?: string
  diarrea: boolean
  estadoGeneral?: string
  lactancia?: string
  temperaturaC?: number
  observaciones?: string
  version: number
}

export interface CrearControlNeonatalInput {
  animalId: string
  fechaControl: string
  momento: MomentoControlNeonatal
  calostrado: EstadoCalostrado
  ombligoDesinfectado: boolean
  ombligoEstado?: string
  diarrea: boolean
  estadoGeneral?: string
  lactancia?: string
  temperaturaC?: number
  observaciones?: string
}

/** Control ectoparasitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, sección 4.d): puede registrarse contra un animal o un lote_ganadero completo, nunca ambos ni ninguno. */
export type TipoEctoparasito = 'GARRAPATA' | 'MOSCA_CUERNOS' | 'TORSALO' | 'PIOJOS' | 'OTRO'
export type NivelCargaParasitaria = 'BAJO' | 'MEDIO' | 'ALTO'

export interface ControlEctoparasitario {
  id: string
  empresaId: string
  animalId?: string
  loteGanaderoId?: string
  tipo: TipoEctoparasito
  nivelCarga: NivelCargaParasitaria
  tratado: boolean
  producto?: string
  principioActivo?: string
  fecha: string
  observaciones?: string
  version: number
}

export interface CrearControlEctoparasitarioInput {
  animalId?: string
  loteGanaderoId?: string
  tipo: TipoEctoparasito
  nivelCarga: NivelCargaParasitaria
  tratado: boolean
  producto?: string
  principioActivo?: string
  fecha: string
  observaciones?: string
}

/** Examen reproductivo (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, sección 4.e): aptitud de toros y vaquillas antes del servicio. El sexo NO se duplica acá, se lee de `animal.sexo`. */
export type ResultadoExamenReproductivo = 'APTO' | 'NO_APTO' | 'OBSERVACION'
export type EnfermedadReproductiva = 'IBR' | 'BVD' | 'BRUCELOSIS' | 'LEPTOSPIROSIS' | 'TRICOMONIASIS' | 'CAMPYLOBACTERIOSIS'
export type ResultadoPruebaReproductiva = 'NEGATIVO' | 'POSITIVO' | 'NO_REALIZADO'

export interface PruebaReproductiva {
  enfermedad: EnfermedadReproductiva
  resultado: ResultadoPruebaReproductiva
}

export interface ExamenReproductivo {
  id: string
  empresaId: string
  animalId: string
  fecha: string
  resultado: ResultadoExamenReproductivo
  veterinarioId?: string
  circunferenciaEscrotalCm?: number
  motilidadEspermaticaPct?: number
  morfologiaPct?: number
  libido?: string
  capacidadServicio?: string
  pesoKg?: number
  porcentajePesoAdulto?: number
  condicionCorporal?: number
  desarrolloReproductivo?: string
  observaciones?: string
  version: number
  pruebas: PruebaReproductiva[]
}

export interface CrearExamenReproductivoInput {
  animalId: string
  fecha: string
  resultado: ResultadoExamenReproductivo
  veterinarioId?: string
  circunferenciaEscrotalCm?: number
  motilidadEspermaticaPct?: number
  morfologiaPct?: number
  libido?: string
  capacidadServicio?: string
  pesoKg?: number
  porcentajePesoAdulto?: number
  condicionCorporal?: number
  desarrolloReproductivo?: string
  observaciones?: string
  pruebas: PruebaReproductiva[]
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
  PRUEBA_DIAGNOSTICA: 'Prueba diagnóstica',
  CONTROL_ECTOPARASITARIO: 'Control ectoparasitario',
  VIGILANCIA: 'Vigilancia epidemiológica',
  TRATAMIENTO_PREVENTIVO: 'Tratamiento preventivo',
  OTRA: 'Otra actividad',
}

/** Verbo de acción específico por tipo (sección 3: nunca usar "aplicar vacuna" para otro tipo). */
export const TIPO_ACTIVIDAD_ACCION_LABELS: Record<TipoActividad, string> = {
  VACUNACION: 'Aplicar vacuna',
  DESPARASITACION: 'Realizar desparasitación',
  VITAMINIZACION: 'Administrar vitaminas',
  PRUEBA_DIAGNOSTICA: 'Tomar muestra diagnóstica',
  CONTROL_ECTOPARASITARIO: 'Revisar presencia de ectoparásitos',
  VIGILANCIA: 'Ejecutar vigilancia sanitaria',
  TRATAMIENTO_PREVENTIVO: 'Aplicar tratamiento preventivo',
  OTRA: 'Ejecutar actividad',
}

export const MODALIDAD_ACTIVIDAD_LABELS: Record<ModalidadActividad, string> = {
  POR_EDAD: 'Por edad',
  PERIODICA: 'Periódica',
  FECHA_PROGRAMADA: 'Fecha programada',
  POR_HALLAZGO: 'Por hallazgo',
  MANUAL: 'Manual',
}

export const UNIDAD_EDAD_ACTIVIDAD_LABELS: Record<UnidadEdadActividad, string> = {
  DIAS: 'días', MESES: 'meses', ANIOS: 'años',
}

export const UNIDAD_FRECUENCIA_LABELS: Record<UnidadFrecuencia, string> = {
  DIAS: 'días', SEMANAS: 'semanas', MESES: 'meses', ANIOS: 'años',
}

export const REFERENCIA_CALCULO_PERIODICA_LABELS: Record<ReferenciaCalculoPeriodica, string> = {
  FECHA_DE_INGRESO: 'Fecha de ingreso',
  FECHA_DE_NACIMIENTO: 'Fecha de nacimiento',
  ULTIMA_APLICACION: 'Última aplicación',
  FECHA_INICIAL_DEL_PLAN: 'Fecha inicial del plan',
  FECHA_CONFIGURADA: 'Fecha configurada',
}

export const TIPO_CALCULO_DOSIS_LABELS: Record<TipoCalculoDosis, string> = {
  FIJA_POR_ANIMAL: 'Fija por animal',
  POR_PESO: 'Por peso',
  SEGUN_INDICACION: 'Según indicación',
  NO_APLICA: 'No aplica (sin medicamento)',
}

export const UNIDAD_DOSIS_LABELS: Record<UnidadDosis, string> = {
  ML: 'ml', MG: 'mg', G: 'g', TABLETA: 'tableta', DOSIS: 'dosis', GOTA: 'gota', APLICACION: 'aplicación',
  ML_POR_KG: 'ml por kg', ML_POR_10KG: 'ml por 10 kg', ML_POR_50KG: 'ml por 50 kg', MG_POR_KG: 'mg por kg', OTRA: 'otra',
}

export const VIA_ADMINISTRACION_LABELS: Record<ViaAdministracion, string> = {
  SUBCUTANEA: 'Subcutánea', INTRAMUSCULAR: 'Intramuscular', INTRAVENOSA: 'Intravenosa', ORAL: 'Oral',
  TOPICA: 'Tópica', POUR_ON: 'Pour-on', INTRANASAL: 'Intranasal', OTRA: 'Otra', NO_APLICA: 'No aplica',
}

export const LUGAR_APLICACION_LABELS: Record<LugarAplicacion, string> = {
  CUELLO: 'Cuello', TABLA_DEL_CUELLO: 'Tabla del cuello', REGION_ESCAPULAR: 'Región escapular', LOMO: 'Lomo',
  LINEA_DORSAL: 'Línea dorsal', BOCA: 'Boca', FOSA_NASAL: 'Fosa nasal', TODO_EL_CUERPO: 'Todo el cuerpo',
  OTRO: 'Otro', NO_APLICA: 'No aplica',
}

export const ESTADO_EVENTO_CALENDARIO_LABELS: Record<EstadoEventoCalendario, string> = {
  PROYECTADO: 'Proyectado', PROGRAMADO: 'Programado', EN_PREPARACION: 'En preparación', REALIZADO: 'Realizado',
  VENCIDO: 'Vencido', OMITIDO: 'Omitido', CANCELADO: 'Cancelado',
}

export const ESTADO_APLICACION_SANITARIA_LABELS: Record<EstadoAplicacionSanitaria, string> = {
  APLICADO: 'Aplicado', NO_APLICADO: 'No aplicado', APLICADO_PARCIAL: 'Aplicado parcial', RECHAZADO: 'Rechazado',
  POSPUESTO: 'Pospuesto', ANULADO: 'Anulado',
}

/**
 * Por qué existe la actividad en el plan (no reemplaza tipoActividad, que dice qué es).
 * "Obligatorio SENASAG" no se puede desactivar sin justificar; el resto queda a criterio
 * del productor/veterinario.
 */
export const ORIGEN_REGULATORIO_LABELS: Record<OrigenRegulatorio, string> = {
  OBLIGATORIO_SENASAG: 'Obligatorio SENASAG',
  CAMPANA_RIESGO: 'Según campaña/riesgo',
  RECOMENDADO_VETERINARIO: 'Recomendado',
  CONFIGURABLE_ESTABLECIMIENTO: 'Configurable',
}

/** Chip de clasificación regulatoria: obligatorio en rojo, campaña en ámbar, el resto neutro. */
export const ORIGEN_REGULATORIO_BADGE_CLASS: Record<OrigenRegulatorio, string> = {
  OBLIGATORIO_SENASAG: 'status-badge-danger',
  CAMPANA_RIESGO: 'status-badge-warning',
  RECOMENDADO_VETERINARIO: 'status-badge-pending',
  CONFIGURABLE_ESTABLECIMIENTO: 'status-badge',
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

/** Jerarquía visual de severidad de un caso clínico: crítica en rojo, grave en ámbar, el resto neutro. */
export const SEVERIDAD_BADGE_CLASS: Record<SeveridadCaso, string> = {
  LEVE: 'status-badge-pending',
  MODERADA: 'status-badge-pending',
  GRAVE: 'status-badge-warning',
  CRITICA: 'status-badge-danger',
}

export const ESTADO_TRATAMIENTO_LABELS: Record<EstadoTratamiento, string> = {
  BORRADOR: 'Borrador',
  ACTIVO: 'Activo',
  FINALIZADO: 'Finalizado',
  SUSPENDIDO: 'Suspendido',
  ANULADO: 'Anulado',
}

export const MOMENTO_CONTROL_NEONATAL_LABELS: Record<MomentoControlNeonatal, string> = {
  DIA_0: 'Día 0',
  PRIMERA_SEMANA: 'Primera semana',
}

export const ESTADO_CALOSTRADO_LABELS: Record<EstadoCalostrado, string> = {
  CORRECTO: 'Correcto',
  INSUFICIENTE: 'Insuficiente',
  DESCONOCIDO: 'Desconocido',
  NO_APLICA: 'No aplica',
}

export const TIPO_ECTOPARASITO_LABELS: Record<TipoEctoparasito, string> = {
  GARRAPATA: 'Garrapata',
  MOSCA_CUERNOS: 'Mosca de los cuernos',
  TORSALO: 'Tórsalo',
  PIOJOS: 'Piojos',
  OTRO: 'Otro',
}

export const NIVEL_CARGA_PARASITARIA_LABELS: Record<NivelCargaParasitaria, string> = {
  BAJO: 'Bajo',
  MEDIO: 'Medio',
  ALTO: 'Alto',
}

/** Jerarquía visual de carga parasitaria: alto en rojo, medio en ámbar, bajo neutro. */
export const NIVEL_CARGA_PARASITARIA_BADGE_CLASS: Record<NivelCargaParasitaria, string> = {
  BAJO: 'status-badge-pending',
  MEDIO: 'status-badge-warning',
  ALTO: 'status-badge-danger',
}

export const RESULTADO_EXAMEN_REPRODUCTIVO_LABELS: Record<ResultadoExamenReproductivo, string> = {
  APTO: 'Apto',
  NO_APTO: 'No apto',
  OBSERVACION: 'En observación',
}

/** Jerarquía visual: no apto en rojo, observación en ámbar, apto neutro. */
export const RESULTADO_EXAMEN_REPRODUCTIVO_BADGE_CLASS: Record<ResultadoExamenReproductivo, string> = {
  APTO: 'status-badge-pending',
  OBSERVACION: 'status-badge-warning',
  NO_APTO: 'status-badge-danger',
}

export const ENFERMEDAD_REPRODUCTIVA_LABELS: Record<EnfermedadReproductiva, string> = {
  IBR: 'IBR',
  BVD: 'BVD',
  BRUCELOSIS: 'Brucelosis',
  LEPTOSPIROSIS: 'Leptospirosis',
  TRICOMONIASIS: 'Tricomoniasis',
  CAMPYLOBACTERIOSIS: 'Campylobacteriosis',
}

export const RESULTADO_PRUEBA_REPRODUCTIVA_LABELS: Record<ResultadoPruebaReproductiva, string> = {
  NEGATIVO: 'Negativo',
  POSITIVO: 'Positivo',
  NO_REALIZADO: 'No realizado',
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

export async function actualizarPlanItem(planId: string, itemId: string, version: number, input: CrearItemInput) {
  return (await http.put<ApiResponse<PlanSanitarioItem>>(`/api/v1/sanidad/planes/${planId}/items/${itemId}`, input, {
    params: { version }, headers: { 'Idempotency-Key': crypto.randomUUID() },
  })).data.data
}

export async function listVersionesItem(planId: string, itemId: string) {
  return (await http.get<ApiResponse<PlanSanitarioItem[]>>(`/api/v1/sanidad/planes/${planId}/items/${itemId}/versiones`)).data.data
}

export async function listConflictosActivacion(planId: string) {
  return (await http.get<ApiResponse<PlanSanitario[]>>(`/api/v1/sanidad/planes/${planId}/conflictos`)).data.data
}

export async function listCalendarioSanitario(params?: { estado?: EstadoEventoCalendario; animalId?: string }) {
  return (await http.get<ApiResponse<EventoCalendarioSanitario[]>>('/api/v1/sanidad/calendario', { params })).data.data
}

/** Registra lo que el vendedor certifica sobre un animal comprado, sin pasar por una jornada. */
export async function registrarAplicacionDeclarada(input: RegistrarAplicacionDeclaradaInput) {
  return (await http.post<ApiResponse<AplicacionDeclarada>>('/api/v1/sanidad/aplicaciones/declaradas', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listJornadas() {
  return (await http.get<ApiResponse<JornadaSanitaria[]>>('/api/v1/jornadas-sanitarias')).data.data
}

export async function crearJornada(input: CrearJornadaInput) {
  return (await http.post<ApiResponse<JornadaSanitaria>>('/api/v1/jornadas-sanitarias', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function actualizarJornada(jornadaId: string, input: ActualizarJornadaInput) {
  return (await http.put<ApiResponse<JornadaSanitaria>>(`/api/v1/jornadas-sanitarias/${jornadaId}`, input)).data.data
}

export async function anularJornada(jornadaId: string, input: AnularJornadaInput) {
  return (await http.post<ApiResponse<JornadaSanitaria>>(`/api/v1/jornadas-sanitarias/${jornadaId}/anular`, input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
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

export async function crearControlNeonatal(input: CrearControlNeonatalInput) {
  return (await http.post<ApiResponse<ControlNeonatal>>('/api/v1/sanidad/control-neonatal', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listControlesNeonatales(animalId?: string) {
  return (await http.get<ApiResponse<ControlNeonatal[]>>('/api/v1/sanidad/control-neonatal', { params: { animalId } })).data.data
}

export async function crearControlEctoparasitario(input: CrearControlEctoparasitarioInput) {
  return (await http.post<ApiResponse<ControlEctoparasitario>>('/api/v1/sanidad/control-ectoparasitario', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listControlesEctoparasitarios(params: { animalId?: string; loteGanaderoId?: string }) {
  return (await http.get<ApiResponse<ControlEctoparasitario[]>>('/api/v1/sanidad/control-ectoparasitario', { params })).data.data
}

export async function getPrincipiosActivosRecientes(params: { animalId?: string; loteGanaderoId?: string }) {
  return (await http.get<ApiResponse<string[]>>('/api/v1/sanidad/control-ectoparasitario/principios-recientes', { params })).data.data
}

export async function crearExamenReproductivo(input: CrearExamenReproductivoInput) {
  return (await http.post<ApiResponse<ExamenReproductivo>>('/api/v1/sanidad/examenes-reproductivos', input, { headers: { 'Idempotency-Key': crypto.randomUUID() } })).data.data
}

export async function listExamenesReproductivos(animalId?: string) {
  return (await http.get<ApiResponse<ExamenReproductivo[]>>('/api/v1/sanidad/examenes-reproductivos', { params: { animalId } })).data.data
}
