export type UnidadEdad = 'DIAS' | 'MESES' | 'ANIOS'

const FACTOR_EDAD: Record<UnidadEdad, number> = { DIAS: 1, MESES: 30, ANIOS: 365 }

export function convertirEdadADias(valor: string, unidad: UnidadEdad): number | undefined {
  if (valor.trim() === '') return undefined
  const numero = Number(valor)
  if (!Number.isInteger(numero) || numero < 0) return undefined
  return numero * FACTOR_EDAD[unidad]
}

export function errorRangoEdad(minimo?: number, maximo?: number): string | undefined {
  if (minimo !== undefined && maximo !== undefined && maximo < minimo) {
    return `La edad máxima (${maximo} días) no puede ser menor que la mínima (${minimo} días).`
  }
  return undefined
}
