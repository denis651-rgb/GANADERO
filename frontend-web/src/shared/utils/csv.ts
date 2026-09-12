/** Arma el texto de un CSV, escapando comas/comillas/saltos de línea. */
export function construirCsv(encabezados: string[], filas: (string | number | undefined)[][]): string {
  const escapar = (valor: string | number | undefined) => {
    const texto = valor === undefined || valor === null ? '' : String(valor)
    return /[",\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto
  }
  // Encabezados vacío = todo el contenido ya viene armado en `filas` (p. ej. varias líneas de
  // metadatos antes de la tabla); no tiene sentido anteponer una fila en blanco en ese caso.
  return [...(encabezados.length ? [encabezados] : []), ...filas].map((fila) => fila.map(escapar).join(',')).join('\n')
}

/** Genera un CSV en el cliente (sin endpoint de backend) y dispara su descarga. */
export function descargarCsv(nombreArchivo: string, encabezados: string[], filas: (string | number | undefined)[][]) {
  const contenido = construirCsv(encabezados, filas)
  // BOM inicial: para que Excel detecte UTF-8 y no rompa las tildes/ñ.
  const blob = new Blob([`\uFEFF${contenido}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  document.body.removeChild(enlace)
  URL.revokeObjectURL(url)
}
