import { db } from '@/offline/db'

export interface OfflineFormCatalogs {
  breeds: Array<{ id: string; nombre: string }>
  categories: Array<{ id: string; nombre: string }>
  properties: Array<{ id: string; nombre: string; activo: boolean }>
  paddocks: Array<{ id: string; nombre: string; activo: boolean; propiedadId: string }>
  lots: { content: Array<{ id: string; nombre: string; estado: string; propiedadId: string }> }
}

export interface LocalAnimalResult {
  id: string
  codigo: string
  nombre?: string
  estado: string
}

export async function offlineFormCatalogs(): Promise<OfflineFormCatalogs> {
  const [razas, categorias, propiedades, potreros, lotes] = await Promise.all([
    db.catalogos.where('type').equals('RAZA').toArray(),
    db.catalogos.where('type').equals('CATEGORIA').toArray(),
    db.catalogos.where('type').equals('PROPIEDAD').toArray(),
    db.catalogos.where('type').equals('POTRERO').toArray(),
    db.catalogos.where('type').equals('LOTE').toArray(),
  ])
  return {
    breeds: razas.map((c) => ({ id: c.id, nombre: c.name })),
    categories: categorias.map((c) => ({ id: c.id, nombre: c.name })),
    properties: propiedades.map((c) => ({ id: c.id, nombre: c.name, activo: c.activo ?? true })),
    paddocks: potreros.map((c) => ({ id: c.id, nombre: c.name, activo: c.activo ?? true, propiedadId: c.propiedadId ?? '' })),
    lots: { content: lotes.map((c) => ({
      id: c.id,
      nombre: c.name,
      estado: c.estado ?? 'ACTIVO',
      propiedadId: c.propiedadId ?? '',
    })) },
  }
}

export async function searchLocalAnimals(search: string): Promise<LocalAnimalResult[]> {
  const query = search.trim().toLowerCase()
  if (query.length < 2) return []
  const [animales, identificadores] = await Promise.all([
    db.animalesResumen.where('status').equals('ACTIVO').toArray(),
    db.identificadores.toArray(),
  ])
  const byId = new Map(animales.map((animal) => [animal.id, animal]))
  const seen = new Set<string>()
  const result: LocalAnimalResult[] = []
  for (const animal of animales) {
    const codigo = animal.code.toLowerCase()
    const nombre = (animal.name ?? '').toLowerCase()
    if (codigo.includes(query) || nombre.includes(query)) {
      seen.add(animal.id)
      result.push({ id: animal.id, codigo: animal.code, nombre: animal.name, estado: animal.status })
    }
  }
  for (const identificador of identificadores) {
    if (seen.has(identificador.animalId)) continue
    if (identificador.valor.toLowerCase().includes(query)) {
      seen.add(identificador.animalId)
      const animal = byId.get(identificador.animalId)
      result.push({
        id: identificador.animalId,
        codigo: animal?.code ?? `ID: ${identificador.valor}`,
        nombre: animal?.name,
        estado: animal?.status ?? 'ACTIVO',
      })
    }
  }
  return result.slice(0, 8)
}
