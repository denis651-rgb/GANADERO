export interface RetentionPolicy {
  retencionDiarios: number
  retencionSemanales: number
  retencionMensuales: number
}

export interface RespaldoResumen {
  nombreArchivo: string
  fechaCreacion: string // ISO
  estado: string
  integridad: 'DESCONOCIDA' | 'VALIDA' | 'INVALIDA'
}

const ESTADOS_EN_PROGRESO = new Set(['CREANDO', 'COPIANDO_A_CARPETA_EXTERNA'])

function fechaISO(iso: string): string {
  return iso.slice(0, 10)
}

/** Semana ISO-8601 (lunes a domingo), formato "YYYY-Www". */
function semanaIso(iso: string): string {
  const fecha = new Date(iso)
  const dia = (fecha.getUTCDay() + 6) % 7 // lunes=0 ... domingo=6
  const jueves = new Date(fecha)
  jueves.setUTCDate(fecha.getUTCDate() - dia + 3)
  const inicioAnio = new Date(Date.UTC(jueves.getUTCFullYear(), 0, 1))
  const semana = Math.floor((jueves.getTime() - inicioAnio.getTime()) / (7 * 86_400_000)) + 1
  return `${jueves.getUTCFullYear()}-W${String(semana).padStart(2, '0')}`
}

function mesIso(iso: string): string {
  return iso.slice(0, 7)
}

/**
 * Calcula qué respaldos sobran dado la política de retención: conserva la unión de los últimos N
 * diarios, uno de cada una de las últimas N semanas ISO, uno de cada uno de los últimos N meses,
 * y siempre el último respaldo con integridad válida. Nunca marca para borrar uno en progreso o
 * de integridad desconocida (el backend igual lo rechazaría, pero no tiene sentido intentarlo).
 * Devuelve los nombres de archivo que ya no hace falta conservar.
 */
export function calcularSobrantes(respaldos: RespaldoResumen[], politica: RetentionPolicy): string[] {
  const ordenados = [...respaldos].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion))
  const validos = ordenados.filter((r) => r.integridad === 'VALIDA')

  const conservar = new Set<string>()
  if (validos.length > 0) conservar.add(validos[0].nombreArchivo)

  agregarUltimosPorClave(validos, fechaISO, politica.retencionDiarios, conservar)
  agregarUltimosPorClave(validos, semanaIso, politica.retencionSemanales, conservar)
  agregarUltimosPorClave(validos, mesIso, politica.retencionMensuales, conservar)

  return ordenados
      .filter((r) => !conservar.has(r.nombreArchivo))
      .filter((r) => r.integridad !== 'DESCONOCIDA' && !ESTADOS_EN_PROGRESO.has(r.estado))
      .map((r) => r.nombreArchivo)
}

function agregarUltimosPorClave(
  validosDesc: RespaldoResumen[],
  claveDe: (iso: string) => string,
  limite: number,
  conservar: Set<string>,
): void {
  if (limite <= 0) return
  const vistos = new Set<string>()
  for (const respaldo of validosDesc) {
    const clave = claveDe(respaldo.fechaCreacion)
    if (vistos.has(clave)) continue
    vistos.add(clave)
    conservar.add(respaldo.nombreArchivo)
    if (vistos.size >= limite) break
  }
}
