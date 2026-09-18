import type { UnidadDosis } from '@/features/sanidad/api'

/**
 * Peso de referencia (kg) que ya está implícito en las unidades «por peso». El backend calcula
 * `cantidad × peso del animal ÷ peso de referencia` e ignora la unidad, así que si el usuario
 * elige «ml por 50 kg» la referencia tiene que ser 50: cualquier otro valor daría una dosis errónea.
 */
const PESO_REFERENCIA_POR_UNIDAD: Partial<Record<UnidadDosis, number>> = {
  ML_POR_KG: 1,
  MG_POR_KG: 1,
  ML_POR_10KG: 10,
  ML_POR_50KG: 50,
}

/** Devuelve el peso de referencia fijado por la unidad, o `undefined` si el usuario debe indicarlo. */
export function pesoReferenciaFijo(unidad: UnidadDosis | ''): number | undefined {
  return unidad ? PESO_REFERENCIA_POR_UNIDAD[unidad] : undefined
}
