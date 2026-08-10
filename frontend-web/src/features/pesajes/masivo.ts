import { createUuid } from '@/shared/utils/uuid'
import type { PesajeMasivoInput, PesajeMasivoItemInput, PesajeMasivoItemResultado } from '@/features/pesajes/types'

export interface PesajeLoteMasivoParams {
  loteId: string
  animalIds: string[]
  pesoKg: number
  fecha?: string
  observaciones?: string
}

export function buildMasivoInput(params: PesajeLoteMasivoParams): PesajeMasivoInput {
  return {
    fecha: params.fecha,
    dispositivo: 'WEB',
    observaciones: params.observaciones,
    items: params.animalIds.map((animalId): PesajeMasivoItemInput => ({
      id: createUuid(),
      animalId,
      fecha: params.fecha,
      pesoKg: params.pesoKg,
      tipo: 'RUTINA',
      loteId: params.loteId,
      observaciones: params.observaciones,
    })),
  }
}

export function failedAnimalIds(resultados: PesajeMasivoItemResultado[]): Set<string> {
  return new Set(resultados.filter((item) => !item.ok).map((item) => item.animalId))
}

export function retryMasivoInput(original: PesajeMasivoInput, failedAnimalIds_: Set<string>): PesajeMasivoInput {
  return {
    fecha: original.fecha,
    dispositivo: original.dispositivo,
    observaciones: original.observaciones,
    items: original.items.filter((item) => failedAnimalIds_.has(item.animalId)),
  }
}
