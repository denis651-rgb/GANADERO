import type { UnidadEdadDeclarada } from '@/features/animales/types'

export function calcularNacimientoEstimado(referencia?: string, valor?: number | string, unidad?: UnidadEdadDeclarada) {
  const cantidad = Number(valor)
  if (!referencia || !Number.isInteger(cantidad) || cantidad <= 0 || !unidad) return undefined
  const fecha = new Date(`${referencia}T12:00:00Z`)
  if (unidad === 'DIAS') fecha.setUTCDate(fecha.getUTCDate() - cantidad)
  if (unidad === 'MESES') {
    const mesAbsoluto = fecha.getUTCFullYear() * 12 + fecha.getUTCMonth() - cantidad
    const anio = Math.floor(mesAbsoluto / 12)
    const mes = ((mesAbsoluto % 12) + 12) % 12
    fecha.setUTCFullYear(anio, mes, Math.min(fecha.getUTCDate(), new Date(Date.UTC(anio, mes + 1, 0)).getUTCDate()))
  }
  if (unidad === 'ANIOS') {
    const anio = fecha.getUTCFullYear() - cantidad
    fecha.setUTCFullYear(anio, fecha.getUTCMonth(), Math.min(fecha.getUTCDate(), new Date(Date.UTC(anio, fecha.getUTCMonth() + 1, 0)).getUTCDate()))
  }
  return fecha.toISOString().slice(0, 10)
}

export function calcularEdadMeses(fechaNacimiento?: string, referencia?: string) {
  if (!fechaNacimiento || !referencia || fechaNacimiento > referencia) return undefined
  const [anioNacimiento, mesNacimiento, diaNacimiento] = fechaNacimiento.split('-').map(Number)
  const [anioReferencia, mesReferencia, diaReferencia] = referencia.split('-').map(Number)
  let meses = (anioReferencia - anioNacimiento) * 12 + mesReferencia - mesNacimiento
  if (diaReferencia < diaNacimiento) meses--
  return Math.max(0, meses)
}

export function formatearEdadMeses(edadMeses: number): string {
  if (edadMeses >= 12) {
    const anios = Math.floor(edadMeses / 12)
    const m = edadMeses % 12
    return `${anios} año${anios > 1 ? 's' : ''}${m ? ` ${m} m` : ''}`
  }
  return `${edadMeses} mes${edadMeses === 1 ? '' : 'es'}`
}

export function categoriaSugerida<T extends { sexoAplicable: 'MACHO' | 'HEMBRA' | 'AMBOS'; clasificacionAutomatica: boolean; activo?: boolean; edadMinMeses?: number; edadMaxMeses?: number }>(
  categorias: T[] | undefined,
  sexo: 'MACHO' | 'HEMBRA',
  fechaNacimiento?: string,
  referencia?: string,
) {
  const edadMeses = calcularEdadMeses(fechaNacimiento, referencia)
  if (edadMeses == null) return undefined
  return categorias?.find((categoria) => categoria.clasificacionAutomatica
    && categoria.activo !== false
    && (categoria.sexoAplicable === 'AMBOS' || categoria.sexoAplicable === sexo)
    && edadMeses >= (categoria.edadMinMeses ?? 0)
    && (categoria.edadMaxMeses == null || edadMeses <= categoria.edadMaxMeses))
}
