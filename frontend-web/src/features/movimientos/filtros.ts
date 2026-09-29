import type { AnimalSummary } from '@/features/animales/types'
import type { TipoMovimiento } from './api'

export const movementSearchAvailable = false

export interface FiltrosOrigen {
  propiedadId: string
  potreroId: string
  loteId: string
}

export type AnimalFiltrable = Pick<AnimalSummary, 'propiedadActualId' | 'potreroActualId' | 'loteActualId' | 'codigo' | 'nombre'>

export function destinoRequerido(tipo: TipoMovimiento): 'propiedad' | 'potrero' | 'lote' | 'potrero-o-lote' {
  if (tipo === 'CAMBIO_LOTE') return 'lote'
  if (tipo === 'CAMBIO_POTRERO' || tipo === 'CUARENTENA' || tipo === 'RETORNO_CUARENTENA') return 'potrero'
  if (tipo === 'INGRESO_COMPRA' || tipo === 'TRANSFERENCIA_PROPIEDAD' || tipo === 'SALIDA_VENTA') return 'propiedad'
  return 'potrero-o-lote'
}

export function filtrarAnimalesPorOrigen<T extends AnimalFiltrable>(
  animales: T[],
  filtros: FiltrosOrigen,
  busqueda = ''
): T[] {
  if (!filtros.propiedadId) return []
  const termino = busqueda.trim().toLocaleLowerCase('es-BO')
  return animales.filter((animal) => {
    if (animal.propiedadActualId !== filtros.propiedadId) return false
    if (filtros.potreroId && animal.potreroActualId !== filtros.potreroId) return false
    if (filtros.loteId && animal.loteActualId !== filtros.loteId) return false
    if (!termino) return true
    return animal.codigo.toLocaleLowerCase('es-BO').includes(termino)
      || (animal.nombre ?? '').toLocaleLowerCase('es-BO').includes(termino)
  })
}
