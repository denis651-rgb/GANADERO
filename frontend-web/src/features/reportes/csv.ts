/** Genera un CSV en el cliente (sin endpoint de backend) y dispara su descarga. */
export function descargarCsv(nombreArchivo: string, encabezados: string[], filas: (string | number | undefined)[][]) {
  const escapar = (valor: string | number | undefined) => {
    const texto = valor === undefined || valor === null ? '' : String(valor)
    return /[",\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto
  }
  const contenido = [encabezados, ...filas].map((fila) => fila.map(escapar).join(',')).join('\n')
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
