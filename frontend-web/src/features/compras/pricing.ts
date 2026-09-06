/**
 * Vista previa en el cliente del reparto "por tropa o punta". El backend nunca confía en este
 * cálculo: siempre recalcula con aritmética decimal exacta (ver CompraService.calcularPrecios).
 * Esto es solo para que el usuario vea el reparto antes de enviar.
 */
export function distribuirPorTropa(total: number, cantidad: number): number[] {
  if (!(total >= 0) || cantidad <= 0) return []
  const unidad = Math.round((total / cantidad) * 100) / 100
  const filas = Array<number>(cantidad - 1).fill(unidad)
  const acumulado = unidad * (cantidad - 1)
  filas.push(Math.round((total - acumulado) * 100) / 100)
  return filas
}

export function totalPorUnidad(precioUnitario: number, overrides: Array<number | undefined>): number {
  const total = overrides.reduce((sum: number, override) => sum + (override ?? precioUnitario), 0)
  return Math.round(total * 100) / 100
}
