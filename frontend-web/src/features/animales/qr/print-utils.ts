export interface PrintItem {
  animalId: string
  identifierId: string
  codigo: string
}

export function encodePrintItems(items: PrintItem[]) {
  return encodeURIComponent(JSON.stringify(items))
}

export function decodePrintItems(query: string | null): PrintItem[] {
  if (!query) return []
  try {
    const parsed = JSON.parse(decodeURIComponent(query))
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item) => item && typeof item.animalId === 'string' && typeof item.identifierId === 'string')
  } catch {
    return []
  }
}

export function printRoute(items: PrintItem[]) {
  return `/animales/qr/imprimir?q=${encodePrintItems(items)}`
}
