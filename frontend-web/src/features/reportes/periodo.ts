export type TipoPeriodo = 'TRIMESTRE' | 'SEMESTRE' | 'ANIO'

export interface RangoPeriodo {
  desde: string
  hasta: string
}

function ultimoDiaMes(anio: number, mesIndex0: number): number {
  return new Date(anio, mesIndex0 + 1, 0).getDate()
}

function fecha(anio: number, mesIndex0: number, dia: number): string {
  return `${anio}-${String(mesIndex0 + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`
}

/** Calcula el rango de fechas (inclusive) de un trimestre/semestre/año calendario, en horario local. */
export function calcularRangoPeriodo(tipo: TipoPeriodo, anio: number, numero?: number): RangoPeriodo {
  if (tipo === 'ANIO') return { desde: fecha(anio, 0, 1), hasta: fecha(anio, 11, 31) }

  if (tipo === 'SEMESTRE') {
    const semestre = numero === 2 ? 2 : 1
    const mesInicio = semestre === 1 ? 0 : 6
    const mesFin = semestre === 1 ? 5 : 11
    return { desde: fecha(anio, mesInicio, 1), hasta: fecha(anio, mesFin, ultimoDiaMes(anio, mesFin)) }
  }

  const trimestre = numero && numero >= 1 && numero <= 4 ? numero : 1
  const mesInicio = (trimestre - 1) * 3
  const mesFin = mesInicio + 2
  return { desde: fecha(anio, mesInicio, 1), hasta: fecha(anio, mesFin, ultimoDiaMes(anio, mesFin)) }
}
