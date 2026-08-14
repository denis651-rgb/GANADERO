import { useQuery } from '@tanstack/react-query'
import { listAnimals } from '@/features/animales/api'
import type { AnimalSummary } from '@/features/animales/types'
import { listLotes } from '@/features/lotes/api'
import { listPotreros } from '@/features/potreros/api'
import { listPropiedades } from '@/features/propiedades/api'
import { listUsuarios } from '@/features/usuarios/api'
import type { Miembro } from '@/features/usuarios/api'

export interface ReproduccionCatalogs {
  properties: Array<{ id: string; codigo: string; nombre: string; activo: boolean }>
  paddocks: Array<{ id: string; propiedadId: string; codigo: string; nombre: string; activo: boolean }>
  lots: Array<{ id: string; propiedadId: string; codigo: string; nombre: string; estado: string }>
  users: Miembro[]
  hembras: AnimalSummary[]
  machos: AnimalSummary[]
  animales: AnimalSummary[]
  animalLabel: (id?: string) => string
  userLabel: (id?: string) => string
  propertyLabel: (id?: string) => string
  paddockLabel: (id?: string) => string
}

export function useReproduccionCatalogs() {
  return useQuery<ReproduccionCatalogs>({
    queryKey: ['reproduccion-catalogos'],
    queryFn: async () => {
      const [properties, paddocks, lotsPage, users, hembrasPage, machosPage] = await Promise.all([
        listPropiedades(),
        listPotreros(),
        listLotes({ estado: 'ACTIVO', page: 0, size: 100 }),
        listUsuarios(),
        listAnimals({ estado: 'ACTIVO', sexo: 'HEMBRA', page: 0, size: 500 }),
        listAnimals({ estado: 'ACTIVO', sexo: 'MACHO', page: 0, size: 500 }),
      ])
      const hembras = hembrasPage.content
      const machos = machosPage.content
      const animales = [...hembras, ...machos]
      const animalLabel = (id?: string) => {
        if (!id) return '—'
        const animal = animales.find((item) => item.id === id)
        return animal ? (animal.nombre ? `${animal.codigo} · ${animal.nombre}` : animal.codigo) : id.slice(0, 8)
      }
      const userLabel = (id?: string) => {
        if (!id) return '—'
        const user = users.find((item) => item.usuarioId === id || item.id === id)
        return user ? `${user.nombres} ${user.apellidos}`.trim() : id.slice(0, 8)
      }
      const propertyLabel = (id?: string) => {
        if (!id) return '—'
        return properties.find((item) => item.id === id)?.nombre ?? id.slice(0, 8)
      }
      const paddockLabel = (id?: string) => {
        if (!id) return '—'
        return paddocks.find((item) => item.id === id)?.nombre ?? id.slice(0, 8)
      }
      return { properties, paddocks, lots: lotsPage.content, users, hembras, machos, animales, animalLabel, userLabel, propertyLabel, paddockLabel }
    },
    staleTime: 60_000,
  })
}
