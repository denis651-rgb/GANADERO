import { useQuery } from '@tanstack/react-query'
import { listAnimals } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { listCategorias } from '@/features/animales/api'
import { listLotes } from '@/features/lotes/api'
import { listPotreros } from '@/features/potreros/api'
import { listPropiedades } from '@/features/propiedades/api'
import type { Propiedad } from '@/features/propiedades/api'

export interface SanidadCatalogs {
  properties: Propiedad[]
  paddocks: Array<{ id: string; propiedadId: string; codigo: string; nombre: string; activo: boolean }>
  categories: Array<{ id: string; codigo: string; nombre: string; sexoAplicable: string }>
  lots: Array<{ id: string; propiedadId: string; codigo: string; nombre: string; estado: string }>
  animals: AnimalSummary[]
  animalLabel: (id?: string) => string
}

export function useSanidadCatalogs() {
  return useQuery<SanidadCatalogs>({
    queryKey: ['sanidad-catalogos'],
    queryFn: async () => {
      const [properties, paddocks, categories, lotsPage, animalsPage] = await Promise.all([
        listPropiedades(),
        listPotreros(),
        listCategorias(),
        listLotes({ estado: 'ACTIVO', page: 0, size: 100 }),
        listAnimals({ estado: 'ACTIVO', page: 0, size: 500 }),
      ])
      const animalLabel = (id?: string) => {
        if (!id) return '—'
        const animal = animalsPage.content.find((item) => item.id === id)
        return animal ? (animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo) : id.slice(0, 8)
      }
      return {
        properties,
        paddocks,
        categories,
        lots: lotsPage.content,
        animals: animalsPage.content,
        animalLabel,
      }
    },
    staleTime: 60_000,
  })
}
