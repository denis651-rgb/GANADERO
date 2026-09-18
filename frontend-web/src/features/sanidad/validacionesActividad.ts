import type { LugarAplicacion, ModalidadActividad, TipoCalculoDosis, ViaAdministracion } from '@/features/sanidad/api'

/*
 * Validaciones del formulario de planes y actividades. Son el espejo de las que hace el backend
 * (PlanSanitarioService): se repiten acá solo para avisar antes de enviar, el backend sigue siendo
 * quien decide.
 */

export function errorFechaFin(inicio: string, fin: string): string | undefined {
  if (inicio && fin && fin < inicio) return 'La fecha de fin no puede ser anterior a la fecha de inicio.'
  return undefined
}

export interface ErroresViaLugar { lugar?: string; detalleVia?: string; detalleLugar?: string }

/** Compatibilidad entre vía y lugar anatómico. «Otro» lugar exige su detalle aunque todavía no haya vía. */
export function erroresViaLugar(
  via: ViaAdministracion | '', lugar: LugarAplicacion | '', detalleVia: string, detalleLugar: string,
): ErroresViaLugar {
  const errores: ErroresViaLugar = {}
  if (lugar === 'OTRO' && !detalleLugar.trim()) errores.detalleLugar = 'Indica el detalle del lugar.'
  if (!via) return errores
  const sinLugar = !lugar || lugar === 'NO_APLICA'
  if ((via === 'SUBCUTANEA' || via === 'INTRAMUSCULAR' || via === 'INTRAVENOSA') && sinLugar) {
    errores.lugar = 'Una vía inyectable requiere indicar el lugar anatómico.'
  } else if (via === 'ORAL' && !sinLugar && lugar !== 'BOCA') {
    errores.lugar = 'La vía oral solo admite «Boca» o «No aplica» como lugar.'
  } else if (via === 'POUR_ON' && lugar && lugar !== 'LINEA_DORSAL' && lugar !== 'LOMO') {
    errores.lugar = 'Pour-on solo admite «Línea dorsal» o «Lomo» como lugar.'
  }
  if (via === 'OTRA' && !detalleVia.trim()) errores.detalleVia = 'Indica el detalle de la vía.'
  return errores
}

export interface ErroresDosis { cantidad?: string; minima?: string; maxima?: string }

/** Cantidad obligatoria en dosis por peso; toda cantidad, mínima y máxima escritas deben ser mayores que cero. */
export function erroresDosis(tipo: TipoCalculoDosis, cantidad: string, minima: string, maxima: string): ErroresDosis {
  const errores: ErroresDosis = {}
  const positivo = (valor: string) => Number(valor) > 0
  if (tipo !== 'NO_APLICA') {
    if (tipo === 'POR_PESO' && cantidad.trim() === '') errores.cantidad = 'La dosis por peso requiere una cantidad.'
    else if (cantidad.trim() !== '' && !positivo(cantidad)) errores.cantidad = 'La cantidad debe ser mayor que cero.'
  }
  if (tipo === 'POR_PESO') {
    if (minima.trim() !== '' && !positivo(minima)) errores.minima = 'La dosis mínima debe ser mayor que cero.'
    if (maxima.trim() !== '' && !positivo(maxima)) errores.maxima = 'La dosis máxima debe ser mayor que cero.'
    if (!errores.minima && !errores.maxima && minima.trim() !== '' && maxima.trim() !== '' && Number(minima) > Number(maxima)) {
      errores.maxima = 'La dosis máxima no puede ser menor que la mínima.'
    }
  }
  return errores
}

export function errorHallazgos(modalidad: ModalidadActividad, tipos: string[]): string | undefined {
  return modalidad === 'POR_HALLAZGO' && tipos.length === 0 ? 'Elige al menos un hallazgo que active la actividad.' : undefined
}

/** Instante ISO → valor de un `<input type="datetime-local">` en la hora local, la misma con la que se convierte al guardar. */
export function fechaHoraLocal(iso: string): string {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return ''
  const dos = (n: number) => String(n).padStart(2, '0')
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
}
