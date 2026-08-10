import { describe, expect, it } from 'vitest'
import { buildMasivoInput, failedAnimalIds, retryMasivoInput } from '@/features/pesajes/masivo'
import type { PesajeMasivoInput, PesajeMasivoItemResultado } from '@/features/pesajes/types'

const ANIMAL_A = '00000000-0000-0000-0000-000000000001'
const ANIMAL_B = '00000000-0000-0000-0000-000000000002'
const LOTE = '00000000-0000-0000-0000-000000000003'

describe('buildMasivoInput', () => {
  it('crea un ítem por animal con lote, RUTINA y claves únicas', () => {
    const input = buildMasivoInput({ loteId: LOTE, animalIds: [ANIMAL_A, ANIMAL_B], pesoKg: 250, fecha: '2026-08-10' })

    expect(input.items).toHaveLength(2)
    expect(input.dispositivo).toBe('WEB')
    expect(input.items.every((item) => item.loteId === LOTE && item.tipo === 'RUTINA' && item.pesoKg === 250)).toBe(true)
    expect(new Set(input.items.map((item) => item.id)).size).toBe(2)
  })
})

describe('failedAnimalIds', () => {
  it('devuelve solo los animales con error', () => {
    const resultados: PesajeMasivoItemResultado[] = [
      { animalId: ANIMAL_A, ok: true },
      { animalId: ANIMAL_B, ok: false, errorCode: 'ANIMAL_NOT_ACTIVE' },
    ]
    expect([...failedAnimalIds(resultados)]).toEqual([ANIMAL_B])
  })
})

describe('retryMasivoInput', () => {
  it('reintenta solo los ítems fallidos conservando sus claves de idempotencia', () => {
    const original: PesajeMasivoInput = {
      fecha: '2026-08-10',
      dispositivo: 'WEB',
      items: [
        { id: 'id-a', animalId: ANIMAL_A, pesoKg: 250, loteId: LOTE },
        { id: 'id-b', animalId: ANIMAL_B, pesoKg: 250, loteId: LOTE },
      ],
    }
    const reintento = retryMasivoInput(original, new Set([ANIMAL_B]))

    expect(reintento.items).toHaveLength(1)
    expect(reintento.items[0].animalId).toBe(ANIMAL_B)
    expect(reintento.items[0].id).toBe('id-b')
  })
})
