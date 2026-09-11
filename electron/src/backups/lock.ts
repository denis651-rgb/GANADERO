/**
 * Candado único para todo el módulo de respaldos: crear, restaurar y la corrida programada nunca
 * pueden solaparse. El backend tiene su propio lock para /api/v1/respaldos, pero la restauración
 * no pasa por HTTP (el backend está apagado durante el reemplazo de archivo), así que Electron
 * necesita su propio candado para cubrir ese caso.
 */
export class OperacionEnCursoError extends Error {
  constructor() {
    super('Ya hay una operación de respaldo en curso; espera a que termine e intenta de nuevo.')
    this.name = 'OperacionEnCursoError'
  }
}

let operacionEnCurso = false

export async function conLockDeOperacion<T>(fn: () => Promise<T>): Promise<T> {
  if (operacionEnCurso) throw new OperacionEnCursoError()
  operacionEnCurso = true
  try {
    return await fn()
  } finally {
    operacionEnCurso = false
  }
}
