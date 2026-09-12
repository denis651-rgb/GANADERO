import {
  LUGAR_APLICACION_LABELS,
  TIPO_CALCULO_DOSIS_LABELS,
  UNIDAD_DOSIS_LABELS,
  VIA_ADMINISTRACION_LABELS,
  type AnimalElegibilidad,
  type PlanSanitarioItem,
} from '@/features/sanidad/api'
import type { PlanillaSanitariaAnimal, PlanillaSanitariaInput } from '@/shared/api/http'

function dosisTexto(item: PlanSanitarioItem): string | undefined {
  if (item.dosisTipoCalculo === 'NO_APLICA') return undefined
  if (item.dosisTipoCalculo === 'FIJA_POR_ANIMAL' && item.dosisCantidad != null) {
    return `${item.dosisCantidad} ${item.dosisUnidad ? UNIDAD_DOSIS_LABELS[item.dosisUnidad] : ''}`.trim()
  }
  return TIPO_CALCULO_DOSIS_LABELS[item.dosisTipoCalculo]
}

function edadTexto(animal: Pick<AnimalElegibilidad, 'edadDias' | 'edadEstimada'>): string | undefined {
  if (animal.edadDias == null) return undefined
  return `${animal.edadEstimada ? '≈ ' : ''}${animal.edadDias} días${animal.edadEstimada ? ' (estimada)' : ''}`
}

interface DatosPlanillaParams {
  actividad: PlanSanitarioItem
  fechaAplicacion: string
  propiedad?: string
  potrero?: string
  lote?: string
  animales: AnimalElegibilidad[]
}

/** Arma el payload común para exportar la planilla de campo, tanto a Excel (desktop) como a CSV. */
export function datosPlanilla({ actividad, fechaAplicacion, propiedad, potrero, lote, animales }: DatosPlanillaParams): PlanillaSanitariaInput {
  const animalesPlanilla: PlanillaSanitariaAnimal[] = animales.map((animal) => ({
    codigo: animal.codigo,
    nombre: animal.nombre,
    sexo: animal.sexo,
    edadTexto: edadTexto(animal),
  }))
  return {
    actividad: actividad.nombre,
    fecha: fechaAplicacion,
    propiedad: propiedad ?? '',
    potrero,
    lote,
    producto: actividad.productoRecomendadoTexto,
    dosisTexto: dosisTexto(actividad),
    viaAdministracion: actividad.viaAdministracionCodigo ? VIA_ADMINISTRACION_LABELS[actividad.viaAdministracionCodigo] : actividad.viaAdministracion,
    lugarAplicacion: actividad.lugarAplicacion ? LUGAR_APLICACION_LABELS[actividad.lugarAplicacion] : undefined,
    instrucciones: actividad.instruccionesVeterinario,
    animales: animalesPlanilla,
  }
}

/** Filas para el CSV de respaldo (cuando no corre en Ganadero Desktop): metadatos como pares
 * campo/valor, una línea en blanco y la tabla de animales con las mismas columnas del Excel. */
export function filasCsvPlanilla(input: PlanillaSanitariaInput): (string | number | undefined)[][] {
  const filas: (string | number | undefined)[][] = [
    ['Actividad', input.actividad],
    ['Fecha', input.fecha],
    ['Propiedad', input.propiedad],
  ]
  if (input.potrero) filas.push(['Potrero', input.potrero])
  if (input.lote) filas.push(['Lote', input.lote])
  if (input.producto) filas.push(['Producto', input.producto])
  if (input.dosisTexto) filas.push(['Dosis', input.dosisTexto])
  if (input.viaAdministracion) filas.push(['Vía', input.viaAdministracion])
  if (input.lugarAplicacion) filas.push(['Lugar', input.lugarAplicacion])
  if (input.instrucciones) filas.push(['Instrucciones', input.instrucciones])
  filas.push([])
  filas.push(['N°', 'Código', 'Nombre', 'Sexo', 'Edad', 'Aplicado', 'Dosis aplicada', 'Observaciones'])
  input.animales.forEach((animal, index) => {
    filas.push([index + 1, animal.codigo, animal.nombre ?? '', animal.sexo === 'HEMBRA' ? 'Hembra' : 'Macho', animal.edadTexto ?? '', '', '', ''])
  })
  return filas
}

export function nombreArchivoPlanilla(input: PlanillaSanitariaInput, extension: 'csv' | 'xlsx'): string {
  const slug = input.actividad
    .toLocaleLowerCase('es-BO')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return `planilla-${slug || 'jornada'}-${input.fecha}.${extension}`
}
