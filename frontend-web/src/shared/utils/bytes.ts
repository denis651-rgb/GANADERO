const UNIDADES = ['B', 'KB', 'MB', 'GB', 'TB']

export function formatBytes(bytes?: number | null): string {
  if (bytes === undefined || bytes === null || Number.isNaN(bytes)) return '—'
  if (bytes < 1024) return `${bytes} B`
  let valor = bytes
  let unidad = 0
  while (valor >= 1024 && unidad < UNIDADES.length - 1) {
    valor /= 1024
    unidad += 1
  }
  return `${valor.toFixed(valor >= 10 ? 0 : 1)} ${UNIDADES[unidad]}`
}
