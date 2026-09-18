export type UnidadEdad = 'DIAS' | 'MESES' | 'ANIOS'

const FACTOR_EDAD: Record<UnidadEdad, number> = { DIAS: 1, MESES: 30, ANIOS: 365 }

export function convertirEdadADias(valor: string, unidad: UnidadEdad): number | undefined {
  if (valor.trim() === '') return undefined
  const numero = Number(valor)
  if (!Number.isInteger(numero) || numero < 0) return undefined
  return numero * FACTOR_EDAD[unidad]
}

/**
 * Expresa un rango de edad guardado en días en una sola unidad, para mostrarlo al editar. Prefiere
 * la unidad guardada si ambos límites dividen exacto; si no, la mayor unidad exacta y, como último
 * recurso, días. Así 210 días nunca se muestra como «210 meses».
 */
export function rangoEdadDesdeDias(
  minimo: number | undefined, maximo: number | undefined, preferida?: UnidadEdad,
): { unidad: UnidadEdad; minimo: string; maximo: string } {
  const limites = [minimo, maximo].filter((dias): dias is number => Boolean(dias))
  const exacta = (unidad: UnidadEdad) => limites.every((dias) => dias % FACTOR_EDAD[unidad] === 0)
  const unidad: UnidadEdad = preferida && exacta(preferida)
    ? preferida
    : limites.length === 0 ? preferida ?? 'MESES' : (['ANIOS', 'MESES'] as const).find(exacta) ?? 'DIAS'
  const mostrar = (dias?: number) => (dias ? String(dias / FACTOR_EDAD[unidad]) : '')
  return { unidad, minimo: mostrar(minimo), maximo: mostrar(maximo) }
}

export function errorRangoEdad(minimo?: number, maximo?: number): string | undefined {
  if (minimo !== undefined && maximo !== undefined && maximo < minimo) {
    return `La edad máxima (${maximo} días) no puede ser menor que la mínima (${minimo} días).`
  }
  return undefined
}

/**
 * En una actividad POR_EDAD el animal tiene exactamente la edad objetivo el día del evento. Si esa
 * edad cae fuera del rango de animales elegibles, ninguno lo sería y la actividad no se programaría
 * nunca. El backend aplica la misma regla; acá se avisa antes de enviar.
 */
export function errorEdadObjetivoFueraDeRango(objetivo?: number, minimo?: number, maximo?: number): string | undefined {
  if (objetivo === undefined) return undefined
  if (minimo !== undefined && objetivo < minimo) {
    return `La edad objetivo (${objetivo} días) es menor que la edad mínima de los animales elegibles (${minimo} días). Ajusta una de las dos.`
  }
  if (maximo !== undefined && objetivo > maximo) {
    return `La edad objetivo (${objetivo} días) supera la edad máxima de los animales elegibles (${maximo} días). Ajusta una de las dos.`
  }
  return undefined
}
