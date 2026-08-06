import { z } from 'zod'

const backendUuid = (message: string) => z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  message,
)

const optionalUuid = (message: string) => z.union([z.literal(''), backendUuid(message)]).optional()
  .transform((value) => (value === '' ? undefined : value))

const optionalText = (max: number) => z.union([z.literal(''), z.string().trim().max(max)]).optional()
  .transform((value) => (value === '' ? undefined : value))

export const registrarPesajeSchema = z.object({
  animalId: backendUuid('Selecciona un animal.'),
  fecha: optionalText(10),
  pesoKg: z.coerce.number().positive('El peso debe ser mayor a cero.').max(2000, 'Peso fuera de rango.'),
  tipo: z.enum(['RUTINA', 'NACIMIENTO', 'DESTETE', 'ENTRADA', 'VENTA', 'PESADA_ESPECIAL']),
  condicionCorporal: z.union([z.literal(''), z.coerce.number().min(1, 'La condición corporal va de 1 a 9.').max(9, 'La condición corporal va de 1 a 9.')]).optional()
    .transform((value) => (value === '' ? undefined : value)),
  bascula: optionalText(50),
  propiedadId: optionalUuid('Selecciona una propiedad.'),
  potreroId: optionalUuid('Selecciona un potrero.'),
  loteId: optionalUuid('Selecciona un lote.'),
  observaciones: optionalText(500),
})

export const pesajeLoteSchema = z.object({
  loteId: backendUuid('Selecciona un lote.'),
  pesoKg: z.coerce.number().positive('El peso debe ser mayor a cero.').max(2000, 'Peso fuera de rango.'),
  fecha: optionalText(10),
  observaciones: optionalText(500),
})

export const anularPesajeSchema = z.object({
  motivo: z.string().trim().min(3, 'Indica el motivo de anulación.').max(300, 'El motivo es demasiado largo.'),
})

export type RegistrarPesajeFormInput = z.input<typeof registrarPesajeSchema>
export type RegistrarPesajeForm = z.output<typeof registrarPesajeSchema>
export type PesajeLoteFormInput = z.input<typeof pesajeLoteSchema>
export type PesajeLoteForm = z.output<typeof pesajeLoteSchema>
export type AnularPesajeForm = z.infer<typeof anularPesajeSchema>
