import type { TipoPasto } from './api'

const CARGA_UA_POR_HECTAREA: Record<string, number> = {
  BRACHIARIA: 1.5,
  MOMBASA: 2,
  TANZANIA: 1.8,
  NATURAL: 0.7,
}

export function calcularCapacidadRecomendada(superficieHa: number, pasto: TipoPasto | undefined, tieneAgua: boolean) {
  if (!Number.isFinite(superficieHa) || superficieHa <= 0 || !pasto) return undefined
  const carga = CARGA_UA_POR_HECTAREA[pasto.codigo.toUpperCase()]
  if (!carga) return undefined
  return Math.round(superficieHa * carga * (tieneAgua ? 1 : 0.7) * 100) / 100
}

export function cargaRecomendadaUaHa(pasto: TipoPasto | undefined) {
  return pasto ? CARGA_UA_POR_HECTAREA[pasto.codigo.toUpperCase()] : undefined
}
